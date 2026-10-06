package com.example.coffee.global.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** 재발행 대상: 일정 시간 이상 PENDING인 이벤트를 다른 서버가 잡지 않은 것만 잠금 */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
              AND created_at < :threshold
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findPendingForRelay(@Param("threshold") LocalDateTime threshold,
                                          @Param("limit") int limit);

    /** 즉시 발행 성공: 아직 PENDING일 때만 PUBLISHED로 */
    @Transactional
    @Modifying
    @Query("""
            update OutboxEvent o
            set o.status = :published, o.publishedAt = :now
            where o.id = :id and o.status = :pending
            """)
    int markPublishedIfPending(@Param("id") Long id,
                               @Param("pending") OutboxStatus pending,
                               @Param("published") OutboxStatus published,
                               @Param("now") LocalDateTime now);
}