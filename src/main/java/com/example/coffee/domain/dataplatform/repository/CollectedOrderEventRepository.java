package com.example.coffee.domain.dataplatform.repository;

import com.example.coffee.domain.dataplatform.entity.CollectedOrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectedOrderEventRepository extends JpaRepository<CollectedOrderEvent, Long> {

    boolean existsByEventId(String eventId);

    long countByEventId(String eventId);

    List<CollectedOrderEvent> findAllByOrderIdOrderByIdAsc(Long orderId);
}