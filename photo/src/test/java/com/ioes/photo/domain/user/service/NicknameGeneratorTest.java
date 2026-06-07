package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * {@link NicknameGenerator} 단위 테스트.
 *
 * @author 황제연
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NicknameGenerator 단위 테스트")
class NicknameGeneratorTest {

    @Mock NicknameProperties properties;
    @Mock UserRepository     userRepository;

    @InjectMocks NicknameGenerator nicknameGenerator;

    /** 테스트 전용 해시태그 범위 (1~10, 최대 시도 3회) */
    private NicknameProperties.Hashtag hashtagConf;

    @BeforeEach
    void setUp() {
        hashtagConf = new NicknameProperties.Hashtag();
        hashtagConf.setMin(1);
        hashtagConf.setMax(10);
        hashtagConf.setMaxAttempts(3);

        given(properties.getAdjectives()).willReturn(List.of("멋진", "포근한", "고요한"));
        given(properties.getNouns()).willReturn(List.of("코끼리", "여우", "고양이"));
        given(properties.getHashtag()).willReturn(hashtagConf);
    }

    // ── generate ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("사용 중인 닉네임이 없으면 해시태그 1을 부여한다")
        void shouldAssignTag1_whenNicknameIsUnused() {
            given(userRepository.findHashTagsByNickname(anyString())).willReturn(List.of());

            NicknameGenerator.Result result = nicknameGenerator.generate();

            assertThat(result.nickname()).isNotBlank();
            assertThat(result.hashTag()).isEqualTo(1L);
        }

        @Test
        @DisplayName("반환된 닉네임은 형용사 + 명사 조합이다")
        void shouldReturnAdjectivePlusNounNickname() {
            given(userRepository.findHashTagsByNickname(anyString())).willReturn(List.of());

            NicknameGenerator.Result result = nicknameGenerator.generate();

            List<String> adjectives = List.of("멋진", "포근한", "고요한");
            List<String> nouns      = List.of("코끼리", "여우", "고양이");
            boolean startsWithAdjective = adjectives.stream().anyMatch(a -> result.nickname().startsWith(a));
            boolean endsWithNoun        = nouns.stream().anyMatch(n -> result.nickname().endsWith(n));

            assertThat(startsWithAdjective).isTrue();
            assertThat(endsWithNoun).isTrue();
        }

        @Test
        @DisplayName("해시태그 1이 사용 중이면 2를 부여한다")
        void shouldAssignTag2_whenTag1IsUsed() {
            given(userRepository.findHashTagsByNickname(anyString())).willReturn(List.of(1L));

            NicknameGenerator.Result result = nicknameGenerator.generate();

            assertThat(result.hashTag()).isEqualTo(2L);
        }

        @Test
        @DisplayName("연속된 앞 번호가 모두 사용 중이면 다음 번호를 부여한다")
        void shouldAssignNextTag_whenConsecutiveTagsUsed() {
            given(userRepository.findHashTagsByNickname(anyString())).willReturn(List.of(1L, 2L, 3L));

            NicknameGenerator.Result result = nicknameGenerator.generate();

            assertThat(result.hashTag()).isEqualTo(4L);
        }

        @Test
        @DisplayName("중간 번호가 비어있으면 그 번호를 부여한다")
        void shouldAssignGapTag_whenMiddleTagIsAvailable() {
            given(userRepository.findHashTagsByNickname(anyString())).willReturn(List.of(1L, 2L, 4L, 5L));

            NicknameGenerator.Result result = nicknameGenerator.generate();

            assertThat(result.hashTag()).isEqualTo(3L);
        }

        @Test
        @DisplayName("첫 번호보다 작은 번호가 비어있으면 min 값을 부여한다")
        void shouldAssignMinTag_whenFirstTagIsNotMin() {
            given(userRepository.findHashTagsByNickname(anyString())).willReturn(List.of(2L, 3L, 5L));

            NicknameGenerator.Result result = nicknameGenerator.generate();

            assertThat(result.hashTag()).isEqualTo(1L);
        }

        @Test
        @DisplayName("해시태그가 모두 소진된 닉네임은 건너뛰고 다음 조합으로 재시도한다")
        void shouldRetryWithDifferentNickname_whenAllTagsExhausted() {
            List<Long> allTags = LongStream.rangeClosed(1, 10).boxed().toList();
            given(userRepository.findHashTagsByNickname(anyString()))
                .willReturn(allTags)   // 첫 번째 닉네임: 모두 소진
                .willReturn(List.of()); // 두 번째 닉네임: 사용 가능

            NicknameGenerator.Result result = nicknameGenerator.generate();

            assertThat(result.hashTag()).isEqualTo(1L);
            then(userRepository).should(times(2)).findHashTagsByNickname(anyString());
        }

        @Test
        @DisplayName("최대 시도 횟수 초과 시 NICKNAME_GENERATION_FAILED 예외를 던진다")
        void shouldThrowNicknameGenerationFailed_whenMaxAttemptsExceeded() {
            List<Long> allTags = LongStream.rangeClosed(1, 10).boxed().toList();
            given(userRepository.findHashTagsByNickname(anyString())).willReturn(allTags);

            assertThatThrownBy(() -> nicknameGenerator.generate())
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(UserErrorCode.NICKNAME_GENERATION_FAILED))
                .hasMessage("닉네임을 생성할 수 없습니다. 관리자에게 문의해주세요.");
        }

        @Test
        @DisplayName("최대 시도 횟수만큼 정확히 DB를 조회한다")
        void shouldQueryExactlyMaxAttemptsTimes_whenAllFail() {
            List<Long> allTags = LongStream.rangeClosed(1, 10).boxed().toList();
            given(userRepository.findHashTagsByNickname(anyString())).willReturn(allTags);

            assertThatThrownBy(() -> nicknameGenerator.generate())
                .isInstanceOf(BusinessException.class);

            then(userRepository).should(times(hashtagConf.getMaxAttempts()))
                .findHashTagsByNickname(anyString());
        }

        @Test
        @DisplayName("해시태그 max 경계값을 정상적으로 부여한다")
        void shouldAssignMaxTag_whenAllExceptMaxAreUsed() {
            List<Long> tagsExceptLast = LongStream.rangeClosed(1, 9).boxed().toList();
            given(userRepository.findHashTagsByNickname(anyString())).willReturn(tagsExceptLast);

            NicknameGenerator.Result result = nicknameGenerator.generate();

            assertThat(result.hashTag()).isEqualTo(10L);
        }
    }
}