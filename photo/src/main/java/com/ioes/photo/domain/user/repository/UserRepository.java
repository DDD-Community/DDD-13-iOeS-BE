package com.ioes.photo.domain.user.repository;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 JPA 리포지토리.
 *
 * @author 황제연
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    @Query("SELECT u.hashTag FROM User u WHERE u.nickname = :nickname ORDER BY u.hashTag ASC")
    List<Long> findHashTagsByNickname(@Param("nickname") String nickname);

    boolean existsByNicknameAndIdNot(String nickname, Long id);

    @Modifying
    @Query("UPDATE User u SET u.deletedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id);
}
