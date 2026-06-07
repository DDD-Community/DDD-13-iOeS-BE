package com.ioes.photo.domain.bbs.entity;

import com.ioes.photo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시판(BBS) 게시글 엔티티.
 *
 * masterId로 게시판 종류를 구분한다 (1: 공지사항, 향후 문의사항·약관 등 확장 가능).
 * pinned가 true인 게시글은 목록 상단에 고정된다.
 *
 * @author 황제연
 */
@Getter
@Entity
@Table(name = "bbs_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BbsPost extends BaseEntity {

    @Column(name = "master_id", nullable = false)
    private Long masterId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean pinned = false;

    @Builder
    private BbsPost(Long masterId, String title, String content, boolean pinned) {
        this.masterId = masterId;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }
}
