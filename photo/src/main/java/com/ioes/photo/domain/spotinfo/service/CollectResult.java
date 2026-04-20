package com.ioes.photo.domain.spotinfo.service;

/**
 * 스케줄러 1회 실행 결과 집계.
 *
 * @author 김성민
 */
public record CollectResult(int success, int fail) {
    public int total() {
        return success + fail;
    }
}
