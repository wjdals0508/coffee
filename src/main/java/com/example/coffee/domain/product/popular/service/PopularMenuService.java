package com.example.coffee.domain.product.popular.service;

import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.entity.ProductStatus;
import com.example.coffee.domain.product.popular.dto.PopularMenuResponse;
import com.example.coffee.domain.product.popular.repository.PopularMenuRedisRepository;
import com.example.coffee.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularMenuService {

    private static final int PERIOD_DAYS = 7;
    private static final int TOP_N = 3;

    private final PopularMenuRedisRepository popularMenuRedisRepository;
    private final ProductRepository productRepository;

    public List<PopularMenuResponse> getPopularMenus() {
        // 1. 오늘 포함 최근 7일 (D-6 ~ D)
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = IntStream.range(0, PERIOD_DAYS)
                .mapToObj(today::minusDays)
                .toList();

        // 2~4. 합산 → 0 이하 제외 → 수량 내림차순, 동점이면 상품 ID 오름차순
        List<Map.Entry<Long, Long>> ranked = popularMenuRedisRepository.sumQuantities(dates)
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Comparator.<Map.Entry<Long, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .toList();

        if (ranked.isEmpty()) {
            return List.of();
        }

        // 5. 상품 정보 한 번에 조회
        List<Long> productIds = ranked.stream().map(Map.Entry::getKey).toList();
        Map<Long, Product> productById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 6. 없는 상품·판매 종료 상품을 건너뛰며 상위 3개
        List<PopularMenuResponse> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : ranked) {
            Product product = productById.get(entry.getKey());
            if (product == null || product.getStatus() == ProductStatus.DISCONTINUED) {
                continue;
            }
            result.add(PopularMenuResponse.of(result.size() + 1, product, entry.getValue()));
            if (result.size() == TOP_N) {
                break;
            }
        }
        return result;
    }
}