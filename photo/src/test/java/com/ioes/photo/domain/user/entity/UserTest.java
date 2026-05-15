package com.ioes.photo.domain.user.entity;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link User} 엔티티 단위 테스트.
 *
 * @author 황제연
 */
@DisplayName("User 엔티티 단위 테스트")
class UserTest {

    // ── builder ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("builder()")
    class Builder {

        @Test
        @DisplayName("Kakao 사용자를 올바르게 생성한다")
        void shouldCreateKakaoUser() {
            User user = User.builder()
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-123")
                .email("kakao@test.com")
                .nickname("카카오유저")
                .profileImageUrl("https://profile.kakao.com/image.jpg")
                .build();

            assertThat(user.getProvider()).isEqualTo(OAuthProvider.KAKAO);
            assertThat(user.getProviderUserId()).isEqualTo("kakao-123");
            assertThat(user.getEmail()).isEqualTo("kakao@test.com");
            assertThat(user.getNickname()).isEqualTo("카카오유저");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://profile.kakao.com/image.jpg");
        }

        @Test
        @DisplayName("Apple 사용자를 이메일 없이 생성할 수 있다")
        void shouldCreateAppleUserWithoutEmail() {
            User user = User.builder()
                .provider(OAuthProvider.APPLE)
                .providerUserId("apple-sub-456")
                .build();

            assertThat(user.getProvider()).isEqualTo(OAuthProvider.APPLE);
            assertThat(user.getProviderUserId()).isEqualTo("apple-sub-456");
            assertThat(user.getEmail()).isNull();
            assertThat(user.getNickname()).isNull();
        }

        @Test
        @DisplayName("생성 직후 deletedAt은 null이다 (활성 계정)")
        void shouldHaveNullDeletedAt_whenCreated() {
            User user = User.builder()
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-123")
                .build();

            assertThat(user.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("생성 직후 archiveName은 기본값 '나의 보관함'이다")
        void shouldHaveDefaultArchiveName_whenCreated() {
            User user = User.builder()
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-123")
                .build();

            assertThat(user.getArchiveName()).isEqualTo(User.DEFAULT_ARCHIVE_NAME);
        }
    }

    // ── updateArchiveName ─────────────────────────────────────────────────

    @Nested
    @DisplayName("updateArchiveName()")
    class UpdateArchiveName {

        @Test
        @DisplayName("보관함 이름을 정상적으로 변경한다")
        void shouldUpdateArchiveName() {
            User user = User.builder()
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-123")
                .build();

            user.updateArchiveName("제주 여행 스팟");

            assertThat(user.getArchiveName()).isEqualTo("제주 여행 스팟");
        }
    }

    // ── getDisplayName ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDisplayName()")
    class GetDisplayName {

        @Test
        @DisplayName("hashTag가 있으면 nickname#hashTag 형식으로 반환한다")
        void shouldReturnNicknameWithHashTag_whenHashTagExists() {
            User user = User.builder()
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-123")
                .nickname("멋진코끼리")
                .hashTag(7L)
                .build();

            assertThat(user.getDisplayName()).isEqualTo("멋진코끼리#7");
        }

        @Test
        @DisplayName("hashTag가 없으면 nickname만 반환한다")
        void shouldReturnNicknameOnly_whenHashTagIsNull() {
            User user = User.builder()
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-123")
                .nickname("카카오유저")
                .build();

            assertThat(user.getDisplayName()).isEqualTo("카카오유저");
        }

        @Test
        @DisplayName("hashTag 1은 nickname#1 형식으로 반환한다")
        void shouldReturnNicknameWithTag1() {
            User user = User.builder()
                .provider(OAuthProvider.APPLE)
                .providerUserId("apple-123")
                .nickname("포근한여우")
                .hashTag(1L)
                .build();

            assertThat(user.getDisplayName()).isEqualTo("포근한여우#1");
        }

        @Test
        @DisplayName("hashTag 99999는 nickname#99999 형식으로 반환한다")
        void shouldReturnNicknameWithMaxTag() {
            User user = User.builder()
                .provider(OAuthProvider.APPLE)
                .providerUserId("apple-456")
                .nickname("고요한독수리")
                .hashTag(99999L)
                .build();

            assertThat(user.getDisplayName()).isEqualTo("고요한독수리#99999");
        }
    }

    // ── updateProfile ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        private User baseUser() {
            return User.builder()
                .provider(OAuthProvider.KAKAO)
                .providerUserId("kakao-123")
                .email("original@test.com")
                .nickname("원래닉네임")
                .profileImageUrl("https://original.com/image.jpg")
                .build();
        }

        @Test
        @DisplayName("null이 아닌 필드만 업데이트된다")
        void shouldUpdateNonNullFields() {
            User user = baseUser();

            user.updateProfile("new@test.com", "새닉네임", "https://new.com/image.jpg");

            assertThat(user.getEmail()).isEqualTo("new@test.com");
            assertThat(user.getNickname()).isEqualTo("새닉네임");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://new.com/image.jpg");
        }

        @Test
        @DisplayName("null 필드는 기존 값을 유지한다")
        void shouldNotUpdateNullFields() {
            User user = baseUser();

            user.updateProfile(null, null, null);

            assertThat(user.getEmail()).isEqualTo("original@test.com");
            assertThat(user.getNickname()).isEqualTo("원래닉네임");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://original.com/image.jpg");
        }

        @Test
        @DisplayName("일부 필드만 업데이트할 수 있다")
        void shouldUpdatePartialFields() {
            User user = baseUser();

            user.updateProfile(null, "업데이트된닉네임", null);

            assertThat(user.getEmail()).isEqualTo("original@test.com");
            assertThat(user.getNickname()).isEqualTo("업데이트된닉네임");
            assertThat(user.getProfileImageUrl()).isEqualTo("https://original.com/image.jpg");
        }

        @Test
        @DisplayName("updateProfile 호출 후 deletedAt은 변경되지 않는다")
        void shouldNotAffectDeletedAt_whenUpdatingProfile() {
            User user = baseUser();

            user.updateProfile("new@test.com", "새닉네임", null);

            assertThat(user.getDeletedAt()).isNull();
        }
    }
}
