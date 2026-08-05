package com.ioes.photo.domain.metrics.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 운영 지표 집계 MyBatis 매퍼.
 *
 * 날짜 그룹핑을 DB 날짜 함수로 하지 않고 애플리케이션에서 계산한 [start, end) 범위를 파라미터로 받는다.
 * {@code DATE()}/{@code AT TIME ZONE} 같은 함수는 H2(테스트)와 PostgreSQL(운영) 동작이 달라
 * 범위 조건으로 통일한다. 가입 집계는 @SQLRestriction의 영향을 받지 않으므로 탈퇴 유저를 포함한다.
 *
 * @author 김성민
 */
@Mapper
public interface MetricsMapper {

    /** 대상 기간 [start, end) 신규 가입자 수 (탈퇴 포함). */
    long countSignups(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 대상 기간 [start, end) provider별 신규 가입자 수 (탈퇴 포함). */
    List<ProviderCountRow> countSignupsByProvider(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /** end 이전 누적 가입자 수 (탈퇴 포함). */
    long countCumulativeSignups(@Param("end") LocalDateTime end);

    /** 활성 저장(북마크)을 1건 이상 보유한 활성 유저 수. */
    long countActiveSavers();

    /** 활성(미탈퇴) 유저 수. */
    long countActiveUsers();

    /** 활성 저장 수 상위 스팟 목록. */
    List<TopSpotRow> findTopSavedSpots(@Param("limit") int limit);
}
