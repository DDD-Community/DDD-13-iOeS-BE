package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.dto.WithdrawalReasonRequest;
import com.ioes.photo.domain.user.entity.WithdrawalReason;
import com.ioes.photo.domain.user.enums.WithdrawalReasonType;
import com.ioes.photo.domain.user.repository.WithdrawalReasonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;

/**
 * {@link WithdrawalReasonService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawalReasonService 단위 테스트")
class WithdrawalReasonServiceTest {

    @Mock WithdrawalReasonRepository withdrawalReasonRepository;

    @InjectMocks WithdrawalReasonService withdrawalReasonService;

    private static final Long USER_ID = 1L;

    @Test
    @DisplayName("기타 사유와 내용을 함께 저장한다")
    void shouldSaveWithContent_whenOthers() {
        WithdrawalReasonRequest request = new WithdrawalReasonRequest(WithdrawalReasonType.OTHERS, "서비스가 불편해요");

        withdrawalReasonService.saveWithdrawalReason(USER_ID, request);

        ArgumentCaptor<WithdrawalReason> captor = ArgumentCaptor.forClass(WithdrawalReason.class);
        then(withdrawalReasonRepository).should().save(captor.capture());
        WithdrawalReason saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getReasonType()).isEqualTo(WithdrawalReasonType.OTHERS);
        assertThat(saved.getContent()).isEqualTo("서비스가 불편해요");
    }

    @Test
    @DisplayName("내용 없이도 저장된다")
    void shouldSaveWithoutContent() {
        WithdrawalReasonRequest request = new WithdrawalReasonRequest(WithdrawalReasonType.OTHERS, null);

        withdrawalReasonService.saveWithdrawalReason(USER_ID, request);

        then(withdrawalReasonRepository).should().save(any(WithdrawalReason.class));
    }
}
