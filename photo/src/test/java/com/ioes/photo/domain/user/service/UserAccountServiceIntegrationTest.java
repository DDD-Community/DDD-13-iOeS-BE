package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.auth.oauth.OAuthUserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * UserAccountService 통합 테스트 — 트랜잭션, 롤백, 커넥션 점유 검증.
 *
 * <h3>구조적 배경</h3>
 * Spring 7.x(Boot 4.x)에서 @Transactional과 @Retryable을 같은 메서드에 두면
 * @Transactional이 outer 프록시가 되어, retry가 단일 트랜잭션 내부에서 발생한다.
 * 이 경우 첫 충돌로 트랜잭션이 rollback-only 마킹되면 성공한 retry를 커밋할 수 없어
 * UnexpectedRollbackException이 발생한다.
 *
 * <h3>해결 방향</h3>
 * createUser에는 @Transactional만 유지하고, retry 루프를 호출자(OAuthService)에 위치시킨다.
 * 이렇게 하면 각 createUser 호출이 독립 트랜잭션이 되고, 충돌 후 롤백이 깔끔히 처리된다.
 *
 * <h3>커넥션 점유 증명 원리</h3>
 * pool=2 환경에서 여러 스레드가 동시에 충돌을 일으키고 재시도한다.
 * <ul>
 *   <li>커넥션 미반환(broken) 시: 스레드가 기존 커넥션을 쥔 채 재시도 → pool 고갈 → 획득 대기 latch(10s/30s) 초과 → 실패</li>
 *   <li>커넥션 반환(correct) 시: 트랜잭션 종료와 함께 커넥션 반환 → 재시도가 새 커넥션 획득 → 성공</li>
 * </ul>
 *
 * @author 황제연
 */
@SpringBootTest
@DisplayName("UserAccountService 통합 테스트 — 트랜잭션, 롤백, 커넥션")
class UserAccountServiceIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // HS512 최소 64바이트 — base64("secret-key-for-testing-purposes-only-must-be-at-least-64-bytes-long")
        registry.add("JWT_SECRET", () -> "c2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW11c3QtYmUtYXQtbGVhc3QtNjQtYnl0ZXMtbG9uZw==");
        // pool=2 는 커넥션 반환/동시성 증명을 위해 유지한다.
        // minimum-idle=2 로 양쪽 커넥션을 예열해 획득 지연 스파이크를 줄이고,
        // connection-timeout 은 CI 부하에서도 견디도록 10s 로 둔다.
        // (2s 는 지나치게 빠듯해 단일 스레드 테스트조차 CI 에서 간헐 실패했다 — #133)
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 2);
        registry.add("spring.datasource.hikari.minimum-idle",       () -> 2);
        registry.add("spring.datasource.hikari.connection-timeout",  () -> 10000);
    }

    @MockitoBean RedisConnectionFactory         redisConnectionFactory;
    @MockitoBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean NicknameGenerator              nicknameGenerator;

    @Autowired UserAccountService userAccountService;
    @Autowired UserRepository     userRepository;
    @Autowired JdbcTemplate       jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM users");
    }

    // ── 트랜잭션 롤백 검증 ────────────────────────────────────────────────

    @Test
    @DisplayName("닉네임·해시태그 충돌 시 DataIntegrityViolationException이 발생하고 롤백된다")
    void createUser_rolls_back_on_conflict() {
        preInsertUser("포근한여우", 1L, "pre-existing");
        given(nicknameGenerator.generate())
            .willReturn(new NicknameGenerator.Result("포근한여우", 1L));

        assertThatThrownBy(() -> userAccountService.createUser(kakaoInfo("new-user")))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(userRepository.count()).isEqualTo(1L);
        assertThat(userRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "new-user"))
            .as("롤백 확인 — orphan row 없음")
            .isEmpty();
    }

    // ── retry 성공 시뮬레이션 ─────────────────────────────────────────────

    @Test
    @DisplayName("충돌 후 롤백 → 다른 닉네임으로 재시도 시 정상 저장된다 (OAuthService retry 루프 시뮬레이션)")
    void createUser_succeeds_on_retry_after_conflict() {
        preInsertUser("포근한여우", 1L, "pre-existing");

        // 1회: 충돌
        given(nicknameGenerator.generate())
            .willReturn(new NicknameGenerator.Result("포근한여우", 1L));
        assertThatThrownBy(() -> userAccountService.createUser(kakaoInfo("new-user")))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(userRepository.count()).isEqualTo(1L); // 롤백 확인

        // 2회: 고유 값으로 성공
        given(nicknameGenerator.generate())
            .willReturn(new NicknameGenerator.Result("포근한여우", 2L));
        User created = userAccountService.createUser(kakaoInfo("new-user"));

        assertThat(created.getHashTag()).isEqualTo(2L);
        assertThat(userRepository.count()).isEqualTo(2L);
        then(nicknameGenerator).should(times(2)).generate();
    }

    // ── 5회 소진 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("5회 모두 충돌하면 5번 generate()가 호출되고 DB는 변경되지 않는다")
    void createUser_fails_five_times_all_rolled_back() {
        preInsertUser("포근한여우", 1L, "pre-existing");
        given(nicknameGenerator.generate())
            .willReturn(new NicknameGenerator.Result("포근한여우", 1L));

        int failures = 0;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                userAccountService.createUser(kakaoInfo("new-user"));
            } catch (DataIntegrityViolationException e) {
                failures++;
            }
        }

        assertThat(failures).isEqualTo(5);
        assertThat(userRepository.count()).isEqualTo(1L); // 모두 롤백
        then(nicknameGenerator).should(times(5)).generate();
    }

    // ── 커넥션 점유 검증 ─────────────────────────────────────────────────

    @Test
    @DisplayName("pool=2 / 스레드 2개 동시 충돌+재시도 — 커넥션이 반환되므로 데드락 없이 완료된다")
    void connection_released_between_transactions() throws InterruptedException {
        preInsertUser("포근한여우", 1L, "pre-existing");

        // 처음 2호출: 충돌 / 이후: 고유 해시태그(순번)
        AtomicInteger callSeq = new AtomicInteger(0);
        given(nicknameGenerator.generate()).willAnswer(inv -> {
            int n = callSeq.getAndIncrement();
            return new NicknameGenerator.Result("포근한여우", n < 2 ? 1L : (long)(n + 1));
        });

        List<Exception> errors = new CopyOnWriteArrayList<>();
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(2);

        for (int i = 0; i < 2; i++) {
            String providerId = "conn-test-user-" + i;
            Thread t = new Thread(() -> {
                try {
                    startGate.await();
                    // OAuthService.createUserWithRetry 와 동일한 retry 루프
                    for (int attempt = 1; attempt <= 5; attempt++) {
                        try {
                            userAccountService.createUser(kakaoInfo(providerId));
                            break;
                        } catch (DataIntegrityViolationException e) {
                            if (attempt == 5) errors.add(e);
                        }
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        startGate.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);

        assertThat(finished).as("10초 내 완료 — 커넥션 데드락 없음").isTrue();
        assertThat(errors).as("예외 없음").isEmpty();
        assertThat(userRepository.count()).isEqualTo(3L); // 기존 1 + 신규 2
    }

    // ── 동시 가입 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("5개 스레드가 동시에 신규 가입해도 pool=2 환경에서 모두 정상 저장된다")
    void concurrent_registrations_all_succeed() throws InterruptedException {
        AtomicLong hashTagSeq = new AtomicLong(1);
        given(nicknameGenerator.generate())
            .willAnswer(inv -> new NicknameGenerator.Result("포근한여우", hashTagSeq.getAndIncrement()));

        List<Exception> errors = new CopyOnWriteArrayList<>();
        int threadCount = 5;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            String providerId = "concurrent-user-" + i;
            Thread t = new Thread(() -> {
                try {
                    startGate.await();
                    userAccountService.createUser(kakaoInfo(providerId));
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        startGate.countDown();
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);

        assertThat(finished).isTrue();
        assertThat(errors).isEmpty();
        assertThat(userRepository.count()).isEqualTo((long) threadCount);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private void preInsertUser(String nickname, Long hashTag, String providerId) {
        userRepository.save(User.builder()
            .provider(OAuthProvider.KAKAO)
            .providerUserId(providerId)
            .nickname(nickname)
            .hashTag(hashTag)
            .build());
    }

    private OAuthUserInfo kakaoInfo(String providerId) {
        return new OAuthUserInfo(providerId, null, null, null, OAuthProvider.KAKAO, null);
    }
}