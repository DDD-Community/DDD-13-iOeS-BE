package com.ioes.photo.domain.user.entity;

import com.ioes.photo.global.auth.oauth.OAuthProvider;
import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "users", // 테이블 명 강제
    uniqueConstraints = @UniqueConstraint(
        name = "uk_users_provider_provider_user_id", // 복합 유니크 제약조건명
        columnNames = {"provider", "provider_user_id"} // 복합키 대상 컬럼
    )
)
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    /*
     * OAuth 공급자 정보 (APPLE, KAKAO)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    /*
     * OAuth 공급자가 발급한 고유 사용자 ID
     */
    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    /*
     * 이메일 (공급자가 제공하지 않을 경우 null)
     */
    @Column
    private String email;

    /*
     * 닉네임 (값이 없을 때 대비해서 랜덤 닉네임 생성 규칙 구현 필요)
     */
    @Column
    private String nickname;

    /*
     * 프로필 이미지 URL
     */
    @Column
    private String profileImageUrl;

    /*
     * 탈퇴 일시 (null이면 활성 계정)
     */
    @Column
    private LocalDateTime deletedAt;

    /*
     * 전체 값을 주는 builder 패턴만 허용
     */
    @Builder
    private User(OAuthProvider provider, String providerUserId, String email,
                 String nickname, String profileImageUrl) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    /*
     * 프로필 정보를 업데이트합니다. null 값은 기존 값을 유지합니다.
     */
    public void updateProfile(String email, String nickname, String profileImageUrl) {
        if (email != null) {
            this.email = email;
        }
        if (nickname != null){
            this.nickname = nickname;
        }
        if (profileImageUrl != null){
            this.profileImageUrl = profileImageUrl;
        }
    }

}
