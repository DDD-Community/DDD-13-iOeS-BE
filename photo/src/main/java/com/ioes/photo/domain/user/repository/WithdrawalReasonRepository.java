package com.ioes.photo.domain.user.repository;

import com.ioes.photo.domain.user.entity.WithdrawalReason;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원탈퇴 사유 JPA 리포지토리.
 *
 * @author 황제연
 */
public interface WithdrawalReasonRepository extends JpaRepository<WithdrawalReason, Long> {
}
