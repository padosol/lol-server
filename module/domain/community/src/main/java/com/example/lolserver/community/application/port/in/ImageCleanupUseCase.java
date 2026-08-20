package com.example.lolserver.community.application.port.in;

public interface ImageCleanupUseCase {

    /** 상태별 유예기간이 지난 고아 이미지를 스토리지·DB 에서 지운다. */
    void cleanupOrphans();
}
