package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

/**
 * {@link UserService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock TokenService   tokenService;

    @InjectMocks UserService userService;

    private static final Long USER_ID = 1L;

    // ── deleteAccount ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAccount()")
    class DeleteAccount {

        @Test
        @DisplayName("존재하는 사용자이면 토큰 무효화 후 소프트 삭제한다")
        void shouldInvalidateTokensAndSoftDelete_whenUserExists() {
            given(userRepository.existsById(USER_ID)).willReturn(true);

            userService.deleteAccount(USER_ID);

            then(tokenService).should().invalidateAllUserTokens(USER_ID.toString());
            then(userRepository).should().softDeleteById(USER_ID);
        }

        @Test
        @DisplayName("토큰 무효화가 소프트 삭제보다 먼저 실행된다")
        void shouldInvalidateTokensBeforeSoftDelete() {
            given(userRepository.existsById(USER_ID)).willReturn(true);

            userService.deleteAccount(USER_ID);

            InOrder order = inOrder(tokenService, userRepository);
            order.verify(tokenService).invalidateAllUserTokens(USER_ID.toString());
            order.verify(userRepository).softDeleteById(USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 BusinessException(RESOURCE_NOT_FOUND)을 던진다")
        void shouldThrow_whenUserNotFound() {
            given(userRepository.existsById(USER_ID)).willReturn(false);

            assertThatThrownBy(() -> userService.deleteAccount(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        }

        @Test
        @DisplayName("사용자가 없으면 토큰 무효화를 시도하지 않는다")
        void shouldNotInvalidateTokens_whenUserNotFound() {
            given(userRepository.existsById(USER_ID)).willReturn(false);

            assertThatThrownBy(() -> userService.deleteAccount(USER_ID))
                .isInstanceOf(BusinessException.class);

            then(tokenService).should(never()).invalidateAllUserTokens(any());
            then(userRepository).should(never()).softDeleteById(any());
        }
    }
}
