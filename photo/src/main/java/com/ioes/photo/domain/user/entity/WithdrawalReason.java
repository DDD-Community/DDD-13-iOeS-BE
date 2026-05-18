package com.ioes.photo.domain.user.entity;

import com.ioes.photo.domain.user.enums.WithdrawalReasonType;
import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원탈퇴 사유 엔티티.
 *
 * 탈퇴한 회원의 사유를 기록한다. 기타(OTHERS) 사유인 경우에만 content가 작성된다.
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(name = "withdrawal_reasons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalReason extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reason_type", nullable = false, length = 10)
    private WithdrawalReasonType reasonType;

    @Column(columnDefinition = "TEXT", length = 200)
    private String content;

    @Builder
    private WithdrawalReason(Long userId, WithdrawalReasonType reasonType, String content) {
        this.userId = userId;
        this.reasonType = reasonType;
        this.content = content;
    }
}
