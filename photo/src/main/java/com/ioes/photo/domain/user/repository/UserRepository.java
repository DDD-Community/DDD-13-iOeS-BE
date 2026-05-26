package com.ioes.photo.domain.user.repository;

import com.ioes.photo.domain.user.entity.User;
import com.ioes.photo.global.auth.oauth.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    // @SQLRestriction("deleted_at IS NULL") 적용으로 활성 유저 ID만 반환됨
    @Query("SELECT u.id FROM User u WHERE u.id IN :ids")
    Set<Long> findActiveIdsByIdIn(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("UPDATE User u SET u.deletedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE users SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") Long id);

    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(
        value = "SELECT * FROM users WHERE provider = :provider AND provider_user_id = :providerUserId",
        nativeQuery = true
    )
    Optional<User> findByProviderAndProviderUserIdIncludingDeleted(
        @Param("provider") String provider,
        @Param("providerUserId") String providerUserId
    );
}
