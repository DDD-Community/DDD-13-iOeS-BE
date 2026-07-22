package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.enums.UserRole;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 사용자 권한(role) 영속성 통합 테스트.
 *
 * <p>role enum이 CodedEnum 코드로 저장/복원되는지, findRole 조회가 올바른지 실제 스키마에서 검증한다.
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("사용자 권한 영속성 통합 테스트")
class UserRoleIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("JWT_SECRET", () -> "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
    }

    @MockitoBean RedisConnectionFactory         redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @Autowired UserRepository     userRepository;
    @Autowired UserAccountService userAccountService;
    @Autowired JdbcTemplate       jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("role 미지정으로 저장하면 기본값 USER_CUSTOMER로 영속된다")
    void shouldPersistDefaultRole() {
        User saved = userRepository.save(User.builder()
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-default")
            .build());

        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getRole()).isEqualTo(UserRole.USER_CUSTOMER);
    }

    @Test
    @DisplayName("USER_ADMIN role이 코드로 저장되고 다시 enum으로 복원된다")
    void shouldPersistAndRestoreAdminRole() {
        User saved = userRepository.save(User.builder()
            .provider(OAuthProvider.KAKAO)
            .providerUserId("kakao-admin")
            .role(UserRole.USER_ADMIN)
            .build());

        String storedCode = jdbcTemplate.queryForObject(
            "SELECT role FROM users WHERE id = ?", String.class, saved.getId());
        assertThat(storedCode).isEqualTo("A");

        User found = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getRole()).isEqualTo(UserRole.USER_ADMIN);
    }

    @Test
    @DisplayName("findRole은 사용자의 권한을 반환한다")
    void findRole_returnsUserRole() {
        User saved = userRepository.save(User.builder()
            .provider(OAuthProvider.APPLE)
            .providerUserId("apple-admin")
            .role(UserRole.USER_ADMIN)
            .build());

        assertThat(userAccountService.findRole(saved.getId())).isEqualTo(UserRole.USER_ADMIN);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 findRole은 BusinessException을 던진다")
    void findRole_throws_whenUserNotFound() {
        assertThatThrownBy(() -> userAccountService.findRole(999_999L))
            .isInstanceOf(BusinessException.class);
    }
}
