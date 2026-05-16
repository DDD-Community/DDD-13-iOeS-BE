package com.ioes.photo.domain.user.entity;

import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.common.util.NullUtils;
import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 사용자 엔티티
 *
 * OAuth 소셜 로그인으로 가입된 사용자를 나타냅니다
 * (provider, providerUserId) 복합 유니크 제약을 활용
 *
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_users_provider_provider_user_id",
            columnNames = {"provider", "provider_user_id"}
        ),
        @UniqueConstraint(
            name = "uk_users_nickname_hash_tag",
            columnNames = {"nickname", "hash_tag"}
        )
    }
)
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    public static final String DEFAULT_ARCHIVE_NAME = "나의 보관함";

    @Column(nullable = false, length = 4)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column
    private String email;

    @Column
    private String nickname;

    @Column
    private Long hashTag;

    @Column
    private String profileImageUrl;  // OAuth 공급자(카카오/애플)로부터 제공된 프로필 이미지 URL

    @Column
    private String profileImageKey; // 사용자가 직접 업로드한 프로필 이미지

    @Column
    private String archiveImageKey; // 보관함 이미지 (PRIVATE, S3 키 저장)

    @Column(name = "archive_name", nullable = false, length = 20)
    private String archiveName = DEFAULT_ARCHIVE_NAME;

    @Column
    private LocalDateTime deletedAt;

    @Builder
    private User(OAuthProvider provider, String providerUserId, String email,
                 String nickname, String profileImageUrl, Long hashTag) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.hashTag = hashTag;
    }

    public String getDisplayName() {
        return hashTag != null
                ? nickname + "#" + hashTag
                : nickname;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        if (NullUtils.isNotBlank(nickname)) {
            this.nickname = nickname;
            this.hashTag = null;
        }
        if (NullUtils.isNotBlank(profileImageUrl)) {
            this.profileImageUrl = profileImageUrl;
        }
    }


    public void updateProfileImageKey(String key) {
        this.profileImageKey = key;
        this.profileImageUrl = null;
    }

    public void updateArchiveImageKey(String key) {
        this.archiveImageKey = key;
    }

    public void updateArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

}
