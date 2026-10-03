package com.example.coffee.domain.point.service;

import com.example.coffee.domain.order.entity.Order;
import com.example.coffee.domain.point.dto.response.PointBalanceResponse;
import com.example.coffee.domain.point.dto.response.PointChargeResponse;
import com.example.coffee.domain.point.dto.response.PointHistoryResponse;
import com.example.coffee.domain.point.entity.PointHistory;
import com.example.coffee.domain.point.entity.UserPoint;
import com.example.coffee.domain.point.repository.PointHistoryRepository;
import com.example.coffee.domain.point.repository.UserPointRepository;
import com.example.coffee.domain.user.entity.User;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /** 회원가입 시 0P 지갑 생성 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void createWallet(User user) {
        userPointRepository.save(UserPoint.create(user));
    }

    /** 사용자 직접 충전 */
    @Transactional
    public PointChargeResponse charge(Long userId, long amount) {
        UserPoint userPoint = getUserPointForUpdate(userId);

        userPoint.charge(amount);
        pointHistoryRepository.save(
                PointHistory.charge(userPoint.getUser(), amount, userPoint.getBalance())
        );

        return PointChargeResponse.of(userId, amount, userPoint.getBalance());
    }

    /** 이벤트·운영자 지급 */
    @Transactional
    public void give(Long userId, long amount) {
        UserPoint userPoint = getUserPointForUpdate(userId);

        userPoint.give(amount);
        pointHistoryRepository.save(
                PointHistory.give(userPoint.getUser(), amount, userPoint.getBalance())
        );
    }

    /** 주문 결제 — 반드시 주문 트랜잭션 안에서 호출 */
    // MANDATORY : 반드시 상위 트랜잭션이 있어야 한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public long use(Long userId, Order order, long amount) {
        UserPoint userPoint = getUserPointForUpdate(userId);

        userPoint.use(amount);
        pointHistoryRepository.save(
                PointHistory.use(userPoint.getUser(), order, amount, userPoint.getBalance())
        );

        return userPoint.getBalance();
    }

    public PointBalanceResponse getBalance(Long userId) {
        UserPoint userPoint = userPointRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return PointBalanceResponse.from(userPoint);
    }

    public Page<PointHistoryResponse> getHistories(Long userId, Pageable pageable) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                .map(PointHistoryResponse::from);
    }

    private UserPoint getUserPointForUpdate(Long userId) {
        return userPointRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}