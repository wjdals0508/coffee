package com.example.coffee.domain.auth.repository;

import com.example.coffee.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUserId(Long userId);

    /** 현재 토큰이 oldHash일 때만 교체. 1 = 성공, 0 = 실패 */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            update RefreshToken r
            set r.tokenHash = :newHash,
                r.previousTokenHash = :oldHash,
                r.rotatedAt = :now,
                r.expiredAt = :expiredAt,
                r.updatedAt = :now
            where r.userId = :userId
              and r.sessionId = :sessionId
              and r.tokenHash = :oldHash
            """)
    int rotate(@Param("userId") Long userId,
               @Param("sessionId") String sessionId,
               @Param("oldHash") String oldHash,
               @Param("newHash") String newHash,
               @Param("now") LocalDateTime now,
               @Param("expiredAt") LocalDateTime expiredAt);

    /** 새 로그인: 기존 세션을 새 세션으로 대체 */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            update RefreshToken r
            set r.sessionId = :sessionId,
                r.tokenHash = :tokenHash,
                r.previousTokenHash = null,
                r.rotatedAt = null,
                r.expiredAt = :expiredAt,
                r.updatedAt = :now
            where r.userId = :userId
            """)
    int replaceSession(@Param("userId") Long userId,
                       @Param("sessionId") String sessionId,
                       @Param("tokenHash") String tokenHash,
                       @Param("now") LocalDateTime now,
                       @Param("expiredAt") LocalDateTime expiredAt);

    /** 로그아웃: 현재 토큰을 가진 경우에만 삭제 */
    @Transactional
    @Modifying
    @Query("""
            delete from RefreshToken r
            where r.userId = :userId
              and r.sessionId = :sessionId
              and r.tokenHash = :tokenHash
            """)
    int deleteCurrentSession(@Param("userId") Long userId,
                             @Param("sessionId") String sessionId,
                             @Param("tokenHash") String tokenHash);

    /** 재사용 탐지: 세션 강제 폐기 */
    @Transactional
    @Modifying
    @Query("delete from RefreshToken r where r.userId = :userId and r.sessionId = :sessionId")
    int revokeSession(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}