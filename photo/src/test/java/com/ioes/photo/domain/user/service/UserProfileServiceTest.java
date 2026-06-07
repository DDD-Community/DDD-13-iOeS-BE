package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthService;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

/**
 * {@link UserProfileService} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService 단위 테스트")
class UserProfileServiceTest {

    @Mock UserRepository userRepository;
    @Mock TokenService   tokenService;
    @Mock OAuthService   oAuthService;

    @InjectMocks UserProfileService userService;

    private static final Long USER_ID = 1L;

    // ── deleteAccount ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAccount()")
    class DeleteAccount {

        @Test
        @DisplayName("존재하는 사용자이면 OAuth 연동 해제 → 토큰 무효화 → 소프트 삭제 순서로 실행된다")
        void shouldRevokeTokensAndSoftDelete_whenUserExists() {
            User user = buildUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            userService.deleteAccount(USER_ID);

            InOrder order = inOrder(oAuthService, tokenService, userRepository);
            order.verify(oAuthService).revokeOAuthProvider(user);
            order.verify(tokenService).invalidateAllUserTokens(USER_ID.toString());
            order.verify(userRepository).softDeleteById(USER_ID);
        }

        @Test
        @DisplayName("OAuth 연동 해제가 실패해도 토큰 무효화와 소프트 삭제는 계속 진행된다")
        void shouldContinueDeletion_whenRevokeThrows() {
            User user = buildUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            willThrow(new RuntimeException("revoke 실패")).given(oAuthService).revokeOAuthProvider(user);

            userService.deleteAccount(USER_ID);

            then(tokenService).should().invalidateAllUserTokens(USER_ID.toString());
            then(userRepository).should().softDeleteById(USER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 USER_NOT_FOUND 예외를 던진다")
        void shouldThrow_whenUserNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteAccount(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("사용자가 없으면 이후 처리를 진행하지 않는다")
        void shouldNotProceed_whenUserNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteAccount(USER_ID))
                .isInstanceOf(BusinessException.class);

            then(oAuthService).should(never()).revokeOAuthProvider(any());
            then(tokenService).should(never()).invalidateAllUserTokens(any());
            then(userRepository).should(never()).softDeleteById(any());
        }
    }

    // ── helper ───────────────────────────────────────────────────────────

    private User buildUser() {
        User user = User.builder()
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-123")
            .nickname("테스트유저")
            .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }
}
