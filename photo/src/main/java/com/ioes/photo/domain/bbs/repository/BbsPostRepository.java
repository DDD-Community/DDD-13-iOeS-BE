package com.ioes.photo.domain.bbs.repository;

import com.ioes.photo.domain.bbs.entity.BbsPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 게시판 게시글 JPA 리포지토리.
 *
 * @author 황제연
 */
public interface BbsPostRepository extends JpaRepository<BbsPost, Long> {

    Page<BbsPost> findByMasterId(Long masterId, Pageable pageable);

    Optional<BbsPost> findByIdAndMasterId(Long id, Long masterId);
}
