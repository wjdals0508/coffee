package com.example.coffee.domain.product.popular.service;

import com.example.coffee.domain.order.repository.OrderItemRepository;
import com.example.coffee.domain.order.repository.dto.DailyProductSales;
import com.example.coffee.domain.product.popular.dto.PopularMenuRebuildResponse;
import com.example.coffee.domain.product.popular.repository.PopularMenuRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularMenuRebuildService {

    private static final int PERIOD_DAYS = 7;

    private final OrderItemRepository orderItemRepository;
    private final PopularMenuRedisRepository popularMenuRedisRepository;

    @Transactional(readOnly = true)
    public PopularMenuRebuildResponse rebuild() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(PERIOD_DAYS - 1);

        // 원본(MySQL)에서 날짜별·상품별 판매 수량 집계 (취소 제외)
        List<DailyProductSales> sales = orderItemRepository.sumDailySales(
                fromDate.atStartOfDay(), today.plusDays(1).atStartOfDay());

        Map<LocalDate, Map<Long, Long>> quantityByDate = sales.stream()
                .collect(Collectors.groupingBy(
                        DailyProductSales::orderDate,
                        Collectors.toMap(DailyProductSales::productId, DailyProductSales::quantity)));

        // 7일 모두 교체 (판매가 없던 날은 키 삭제)
        for (LocalDate date = fromDate; !date.isAfter(today); date = date.plusDays(1)) {
            popularMenuRedisRepository.replaceDaily(date, quantityByDate.getOrDefault(date, Map.of()));
        }

        log.info("[POPULAR] 랭킹 재구축 완료: {} ~ {}, 판매 기록 {}건", fromDate, today, sales.size());
        return new PopularMenuRebuildResponse(fromDate, today, sales.size());
    }
}