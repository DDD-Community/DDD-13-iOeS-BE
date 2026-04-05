package com.ioes.photo.domain.user.repository;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 사용자 JPA 리포지토리.
 *
 * @author 황제연
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /*
     * OAuth 공급자와 공급자 사용자 ID로 활성 사용자를 조회합니다.
     */
    Optional<User> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    /*
     * 소프트 삭제 - deleted_at을 현재 시각으로 설정합니다.
     */
    @Modifying
    @Query("UPDATE User u SET u.deletedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id);
}
