package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.dto.WithdrawalReasonRequest;
import com.ioes.photo.domain.user.entity.WithdrawalReason;
import com.ioes.photo.domain.user.repository.WithdrawalReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원탈퇴 사유 서비스.
 *
 * @author 황제연
 */
@Service
@RequiredArgsConstructor
public class WithdrawalReasonService {

    private final WithdrawalReasonRepository withdrawalReasonRepository;

    @Transactional
    public void saveWithdrawalReason(Long userId, WithdrawalReasonRequest request) {
        withdrawalReasonRepository.save(WithdrawalReason.builder()
            .userId(userId)
            .reasonType(request.reasonType())
            .content(request.content())
            .build());
    }
}
