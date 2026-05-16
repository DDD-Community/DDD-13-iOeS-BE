package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.savedspot.repository.SavedSpotArchiveRepository;
import com.ioes.photo.domain.spot.repository.SpotRepository;
import com.ioes.photo.domain.user.dto.MypageHomeResponse;
import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthService;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.config.security.JwtProvider;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * {@link UserProfileService} 홈탭 조회 및 계정 복구 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService 홈탭/복구 단위 테스트")
class UserProfileServiceRestoreTest {

    @Mock UserRepository userRepository;
    @Mock TokenService tokenService;
    @Mock OAuthService oAuthService;
    @Mock SavedSpotArchiveRepository savedSpotArchiveRepository;
    @Mock SpotRepository spotRepository;
    @Mock JwtProvider jwtProvider;

    @InjectMocks UserProfileService userService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getMyPageHome()")
    class GetMyPageHome {

        @Test
        @DisplayName("저장 스팟 수와 등록 스팟 수를 함께 반환한다")
        void shouldReturnCounts() {
            User user = buildUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(savedSpotArchiveRepository.countByUserId(USER_ID)).willReturn(5L);
            given(spotRepository.countByUserId(USER_ID)).willReturn(3L);

            MypageHomeResponse response = userService.getMyPageHome(USER_ID);

            assertThat(response.savedSpotCount()).isEqualTo(5L);
            assertThat(response.recordedSpotCount()).isEqualTo(3L);
            assertThat(response.nickname()).isEqualTo(user.getDisplayName());
        }

        @Test
        @DisplayName("존재하지 않는 유저이면 USER_NOT_FOUND 예외를 던진다")
        void shouldThrow_whenUserNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMyPageHome(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("restoreAccount()")
    class RestoreAccount {

        @Test
        @DisplayName("유효한 RESTORE 토큰이면 계정을 복구한다")
        void shouldRestoreAccount_whenValidRestoreToken() {
            String restoreToken = "valid-restore-token";
            User user = buildUser();
            given(jwtProvider.validateToken(restoreToken)).willReturn(true);
            given(jwtProvider.extractTokenType(restoreToken)).willReturn(JwtProvider.TOKEN_TYPE_RESTORE);
            given(jwtProvider.extractSubject(restoreToken)).willReturn(USER_ID.toString());
            given(userRepository.findByIdIncludingDeleted(USER_ID)).willReturn(Optional.of(user));

            userService.restoreAccount(restoreToken);

            then(userRepository).should().restoreById(USER_ID);
        }

        @Test
        @DisplayName("만료된 토큰이면 RESTORE_TOKEN_INVALID 예외를 던진다")
        void shouldThrow_whenTokenExpired() {
            String expiredToken = "expired-token";
            given(jwtProvider.validateToken(expiredToken)).willReturn(false);

            assertThatThrownBy(() -> userService.restoreAccount(expiredToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.RESTORE_TOKEN_INVALID));
        }

        @Test
        @DisplayName("ACCESS 타입 토큰으로 복구 시도 시 RESTORE_TOKEN_INVALID 예외를 던진다")
        void shouldThrow_whenAccessTokenUsed() {
            String accessToken = "access.token.value";
            given(jwtProvider.validateToken(accessToken)).willReturn(true);
            given(jwtProvider.extractTokenType(accessToken)).willReturn(JwtProvider.TOKEN_TYPE_ACCESS);

            assertThatThrownBy(() -> userService.restoreAccount(accessToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.RESTORE_TOKEN_INVALID));
        }

        @Test
        @DisplayName("복구할 유저를 찾을 수 없으면 USER_NOT_FOUND 예외를 던진다")
        void shouldThrow_whenUserNotFound() {
            String restoreToken = "valid-restore-token";
            given(jwtProvider.validateToken(restoreToken)).willReturn(true);
            given(jwtProvider.extractTokenType(restoreToken)).willReturn(JwtProvider.TOKEN_TYPE_RESTORE);
            given(jwtProvider.extractSubject(restoreToken)).willReturn(USER_ID.toString());
            given(userRepository.findByIdIncludingDeleted(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.restoreAccount(restoreToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }
    }

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
