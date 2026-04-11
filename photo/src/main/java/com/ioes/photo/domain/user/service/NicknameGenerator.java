package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.error.UserErrorCode;
import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 닉네임 자동 생성 컴포넌트.
 *
 * 형용사 + 명사 조합으로 닉네임을 생성하고,
 * 해당 닉네임에 부여 가능한 최솟값 해시태그를 탐색합니다.
 *
 * 최대 시도 횟수내에 생성에 실패하면
 * NICKNAME_GENERATION_FAILED 예외를 발생시킵니다.
 *
 * @author 황제연
 */
@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private final NicknameProperties properties;
    private final UserRepository userRepository;

    public record Result(String nickname, Long hashTag) {}

    public int maxAttempts() {
        return properties.getHashtag().getMaxAttempts();
    }

    public Result generate() {
        int maxAttempts = properties.getHashtag().getMaxAttempts();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String nickname = randomNickname();
            Long tag = findMinAvailableHashTag(nickname);
            if (tag != null) {
                return new Result(nickname, tag);
            }
        }

        throw new BusinessException(UserErrorCode.NICKNAME_GENERATION_FAILED);
    }

    private String randomNickname() {
        List<String> adjectives = properties.getAdjectives();
        List<String> nouns = properties.getNouns();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        return adjectives.get(rng.nextInt(adjectives.size()))
             + nouns.get(rng.nextInt(nouns.size()));
    }

    private Long findMinAvailableHashTag(String nickname) {
        long min = properties.getHashtag().getMin();
        long max = properties.getHashtag().getMax();

        List<Long> usedTags = userRepository.findHashTagsByNickname(nickname);

        long expected = min;
        for (Long tag : usedTags) {
            if (tag > expected) {
                return expected;
            }
            if (tag.equals(expected)) {
                expected++;
            }
        }
        return expected <= max ? expected : null;
    }
}