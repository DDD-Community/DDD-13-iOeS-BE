package com.ioes.photo.domain.user.service;

import com.ioes.photo.domain.user.repository.UserRepository;
import com.ioes.photo.global.auth.token.TokenService;
import com.ioes.photo.global.error.code.CommonErrorCode;
import com.ioes.photo.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 도메인 서비스
 *
 * @author 황제연
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    /*
     * 회원탈퇴 처리
     *
     * @param userId 탈퇴할 사용자의 ID
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public void deleteAccount(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        tokenService.invalidateAllUserTokens(userId.toString());
        userRepository.softDeleteById(userId);
        log.info("회원탈퇴 완료: userId={}", userId);
    }
}
