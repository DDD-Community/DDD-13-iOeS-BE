package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.dto.UpdateProfileRequest;
import com.ioes.photo.domain.user.dto.UpdateProfileResponse;
import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthService;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.error.exception.BusinessException;
import com.ioes.photo.global.storage.StorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * {@link UserService#updateProfile} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService.updateProfile() 단위 테스트")
class UserServiceUpdateProfileTest {

    @Mock UserRepository userRepository;
    @Mock TokenService   tokenService;
    @Mock StorageService storageService;
    @Mock OAuthService   oAuthService;

    @InjectMocks UserService userService;

    private static final Long USER_ID = 1L;

    private User baseUser(String nickname, Long hashTag, String email) {
        User user = User.builder()
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-123")
            .nickname(nickname)
            .hashTag(hashTag)
            .email(email)
            .profileImageUrl("https://original.com/image.jpg")
            .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    // ── 유저 조회 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("유저 조회")
    class UserLookup {

        @Test
        @DisplayName("존재하지 않는 userId이면 USER_NOT_FOUND 예외를 던진다")
        void shouldThrow_whenUserNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, new UpdateProfileRequest(null, null), null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND));
        }
    }

    // ── 닉네임 업데이트 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("닉네임 업데이트")
    class NicknameUpdate {

        @BeforeEach
        void setUp() {
            given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(baseUser("멋진코끼리", 3L, "test@example.com")));
        }

        @Test
        @DisplayName("새 닉네임으로 변경 시 닉네임이 업데이트되고 hashTag는 null이 된다")
        void shouldUpdateNicknameAndClearHashTag() {
            given(userRepository.existsByNicknameAndIdNot("포근한여우", USER_ID)).willReturn(false);

            UpdateProfileResponse response = userService.updateProfile(
                USER_ID, new UpdateProfileRequest("포근한여우", null), null
            );

            assertThat(response.displayName()).isEqualTo("포근한여우");
        }

        @Test
        @DisplayName("기존에 hashTag가 있어도 닉네임 변경 시 hashTag가 제거된다")
        void shouldClearHashTag_whenNicknameChangedFromGenerated() {
            given(userRepository.existsByNicknameAndIdNot("새닉네임", USER_ID)).willReturn(false);

            UpdateProfileResponse response = userService.updateProfile(
                USER_ID, new UpdateProfileRequest("새닉네임", null), null
            );

            assertThat(response.displayName()).isEqualTo("새닉네임");
        }

        @Test
        @DisplayName("현재 닉네임과 동일하면 중복 체크를 수행하지 않는다")
        void shouldNotCheckDuplicate_whenNicknameUnchanged() {
            userService.updateProfile(USER_ID, new UpdateProfileRequest("멋진코끼리", null), null);

            then(userRepository).should(never()).existsByNicknameAndIdNot(anyString(), any());
        }

        @Test
        @DisplayName("닉네임이 null이면 기존 닉네임과 hashTag가 유지된다")
        void shouldKeepNickname_whenNicknameIsNull() {
            UpdateProfileResponse response = userService.updateProfile(
                USER_ID, new UpdateProfileRequest(null, null), null
            );

            assertThat(response.displayName()).isEqualTo("멋진코끼리#3");
            then(userRepository).should(never()).existsByNicknameAndIdNot(anyString(), any());
        }

        @Test
        @DisplayName("다른 유저가 이미 사용 중인 닉네임이면 DUPLICATE_NICKNAME 예외를 던진다")
        void shouldThrow_whenNicknameIsDuplicate() {
            given(userRepository.existsByNicknameAndIdNot("이미사용중", USER_ID)).willReturn(true);

            assertThatThrownBy(() -> userService.updateProfile(
                USER_ID, new UpdateProfileRequest("이미사용중", null), null
            ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.DUPLICATE_NICKNAME));
        }
    }

    // ── 이메일 업데이트 ───────────────────────────────────────────────────

    @Nested
    @DisplayName("이메일 업데이트")
    class EmailUpdate {

        @Test
        @DisplayName("이메일이 없을 때 새 이메일을 등록할 수 있다")
        void shouldRegisterEmail_whenNotSet() {
            given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(baseUser("테스트유저", null, null)));

            UpdateProfileResponse response = userService.updateProfile(
                USER_ID, new UpdateProfileRequest(null, "new@example.com"), null
            );

            assertThat(response.email()).isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("이미 이메일이 등록되어 있으면 EMAIL_ALREADY_REGISTERED 예외를 던진다")
        void shouldThrow_whenEmailAlreadyRegistered() {
            given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(baseUser("테스트유저", null, "existing@example.com")));

            assertThatThrownBy(() -> userService.updateProfile(
                USER_ID, new UpdateProfileRequest(null, "new@example.com"), null
            ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.EMAIL_ALREADY_REGISTERED));
        }

        @Test
        @DisplayName("이메일이 null이면 기존 이메일이 유지된다")
        void shouldKeepEmail_whenEmailIsNull() {
            given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(baseUser("테스트유저", null, "original@example.com")));

            UpdateProfileResponse response = userService.updateProfile(
                USER_ID, new UpdateProfileRequest(null, null), null
            );

            assertThat(response.email()).isEqualTo("original@example.com");
        }
    }

    // ── 프로필 이미지 업데이트 ────────────────────────────────────────────

    @Nested
    @DisplayName("프로필 이미지 업데이트")
    class ProfileImageUpdate {

        @BeforeEach
        void setUp() {
            given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(baseUser("테스트유저", null, "test@example.com")));
        }

        @Test
        @DisplayName("이미지 파일이 있으면 StorageService를 호출하고 URL을 저장한다")
        void shouldUploadImage_whenFileProvided() {
            MultipartFile image = new MockMultipartFile(
                "profileImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
            );
            given(storageService.uploadImage(any())).willReturn("https://storage.example.com/photo.jpg");

            UpdateProfileResponse response = userService.updateProfile(
                USER_ID, new UpdateProfileRequest(null, null), image
            );

            then(storageService).should().uploadImage(image);
            assertThat(response.profileImageUrl()).isEqualTo("https://storage.example.com/photo.jpg");
        }

        @Test
        @DisplayName("이미지 파일이 null이면 StorageService를 호출하지 않는다")
        void shouldNotUploadImage_whenFileIsNull() {
            userService.updateProfile(USER_ID, new UpdateProfileRequest(null, null), null);

            then(storageService).should(never()).uploadImage(any());
        }

        @Test
        @DisplayName("이미지 파일이 비어있으면 StorageService를 호출하지 않는다")
        void shouldNotUploadImage_whenFileIsEmpty() {
            MultipartFile emptyImage = new MockMultipartFile(
                "profileImage", "empty.jpg", "image/jpeg", new byte[0]
            );

            userService.updateProfile(USER_ID, new UpdateProfileRequest(null, null), emptyImage);

            then(storageService).should(never()).uploadImage(any());
        }
    }

    // ── 복합 업데이트 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("복합 업데이트")
    class CombinedUpdate {

        @Test
        @DisplayName("닉네임, 이메일, 이미지를 동시에 업데이트할 수 있다")
        void shouldUpdateAllFieldsAtOnce() {
            given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(baseUser("멋진코끼리", 3L, null)));
            given(userRepository.existsByNicknameAndIdNot("새닉네임", USER_ID)).willReturn(false);
            MultipartFile image = new MockMultipartFile(
                "profileImage", "photo.jpg", "image/jpeg", "imagedata".getBytes()
            );
            given(storageService.uploadImage(any())).willReturn("https://storage.example.com/photo.jpg");

            UpdateProfileResponse response = userService.updateProfile(
                USER_ID, new UpdateProfileRequest("새닉네임", "new@example.com"), image
            );

            assertThat(response.displayName()).isEqualTo("새닉네임");
            assertThat(response.email()).isEqualTo("new@example.com");
            assertThat(response.profileImageUrl()).isEqualTo("https://storage.example.com/photo.jpg");
        }

        @Test
        @DisplayName("모든 필드가 null이면 아무것도 변경되지 않는다")
        void shouldChangeNothing_whenAllFieldsAreNull() {
            given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(baseUser("멋진코끼리", 3L, "test@example.com")));

            UpdateProfileResponse response = userService.updateProfile(
                USER_ID, new UpdateProfileRequest(null, null), null
            );

            assertThat(response.displayName()).isEqualTo("멋진코끼리#3");
            assertThat(response.email()).isEqualTo("test@example.com");
            assertThat(response.profileImageUrl()).isEqualTo("https://original.com/image.jpg");
            then(userRepository).should(never()).existsByNicknameAndIdNot(anyString(), any());
            then(storageService).should(never()).uploadImage(any());
        }
    }
}