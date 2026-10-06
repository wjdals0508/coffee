package com.example.coffee.domain.order.service;

import com.example.coffee.domain.order.dto.request.OrderCreateRequest;
import com.example.coffee.domain.order.dto.request.OrderItemRequest;
import com.example.coffee.domain.order.dto.response.OrderCancelResponse;
import com.example.coffee.domain.order.dto.response.OrderCreateResponse;
import com.example.coffee.domain.order.dto.response.OrderResponse;
import com.example.coffee.domain.order.entity.Order;
import com.example.coffee.domain.order.entity.OrderItem;
import com.example.coffee.domain.order.event.OrderEventService;
import com.example.coffee.domain.order.repository.OrderItemRepository;
import com.example.coffee.domain.order.repository.OrderRepository;
import com.example.coffee.domain.point.service.PointService;
import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.repository.ProductRepository;
import com.example.coffee.domain.user.entity.User;
import com.example.coffee.domain.user.repository.UserRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final OrderEventService orderEventService;

    @Transactional
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        // 1. 같은 상품 수량 합치기
        Map<Long, Integer> quantityByProductId = mergeQuantities(request.items());

        // 2. 상품 락 + 존재 확인 (id 오름차순으로 락 획득)
        Map<Long, Product> productById = findProductsForUpdate(quantityByProductId.keySet());

        // 3. 주문 항목 생성 (가격 스냅샷, 구매 가능 여부 검증)
        List<OrderItem> items = quantityByProductId.entrySet().stream()
                .map(entry -> OrderItem.of(productById.get(entry.getKey()), entry.getValue()))
                .toList();

        // 4. 재고 차감 (락을 잡은 상태, 커밋 시 변경 감지로 UPDATE)
        items.forEach(item -> item.getProduct().decreaseStock(item.getQuantity()));

        // 5. 주문 생성 + 저장
        User user = userRepository.getReferenceById(userId);
        Order order = orderRepository.save(Order.create(user, items));
        orderItemRepository.saveAll(items);

        // 6. 포인트 결제 (잔액 부족 시 재고 차감까지 전체 롤백)
        long remainingPoint = pointService.use(userId, order, order.getTotalAmount());

        // 7. 주문 완료 이벤트를 Outbox에 기록
        orderEventService.recordOrderCompleted(order, items);

        return new OrderCreateResponse(OrderResponse.of(order, items), remainingPoint);
    }

    private Map<Long, Product> findProductsForUpdate(Set<Long> productIds) {
        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);
        if (products.size() != productIds.size()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "존재하지 않는 상품이 포함되어 있습니다.");
        }
        return products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    public OrderResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "주문을 찾을 수 없습니다."));
        List<OrderItem> items = orderItemRepository.findAllWithProductByOrderId(orderId);
        return OrderResponse.of(order, items);
    }

    public Page<OrderResponse> getMyOrders(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
        if (orders.isEmpty()) {
            return orders.map(order -> OrderResponse.of(order, List.of()));
        }

        List<Long> orderIds = orders.getContent().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository.findAllWithProductByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.map(order -> OrderResponse.of(order, itemsByOrderId.getOrDefault(order.getId(), List.of())));
    }

    private Map<Long, Integer> mergeQuantities(List<OrderItemRequest> requests) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (OrderItemRequest request : requests) {
            merged.merge(request.productId(), request.quantity(), Math::addExact);
        }
        return merged;
    }

    @Transactional
    public OrderCancelResponse cancelOrder(Long userId, Long orderId) {
        // 1. 주문 락 + 소유자 확인
        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 상태 변경 (취소 불가면 여기서 종료 → 상품·포인트 락을 잡지 않음)
        order.cancel();

        // 3. 주문 항목 조회 + 재고 복구 (상품 락, id 오름차순)
        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId);
        restoreStocks(items);

        // 4. 포인트 환불 (포인트 락)
        long refundedAmount = order.getTotalAmount();
        long remainingPoint = pointService.refund(userId, order, refundedAmount);

        // 5. 주문 취소 이벤트를 Outbox에 기록 (Outbox 구현 시 추가)
        orderEventService.recordOrderCanceled(order, items);

        return new OrderCancelResponse(OrderResponse.of(order, items), refundedAmount, remainingPoint);
    }

    private void restoreStocks(List<OrderItem> items) {
        Set<Long> productIds = items.stream()
                .map(item -> item.getProduct().getId())// id만 꺼낼때는 쿼리를 보내지 않음
                .collect(Collectors.toSet());

        Map<Long, Product> productById = productRepository.findAllByIdInForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (OrderItem item : items) {
            Product product = productById.get(item.getProduct().getId());
            product.restoreStock(item.getQuantity());
        }
    }
}