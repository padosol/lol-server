package com.example.lolserver.duo.application.model.resultmodel;

import com.example.lolserver.duo.domain.DuoPost;
import com.example.lolserver.duo.domain.DuoRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DuoMatchResultModel {
    private final Long duoPostId;
    private final Long requestId;
    private final String partnerGameName;
    private final String partnerTagLine;
    private final String status;

    public static DuoMatchResultModel of(DuoPost duoPost, DuoRequest duoRequest) {
        return DuoMatchResultModel.builder()
                .duoPostId(duoPost.getId())
                .requestId(duoRequest.getId())
                .partnerGameName(null)
                .partnerTagLine(null)
                .status(duoRequest.getStatus().name())
                .build();
    }

    /** 상대가 게시글 작성자 — 신청자에게 보여 줄 결과. */
    public static DuoMatchResultModel forRequester(DuoPost duoPost, DuoRequest duoRequest) {
        return withPartner(duoPost, duoRequest, duoPost.getGameName(), duoPost.getTagLine());
    }

    /** 상대가 신청자 — 게시글 작성자에게 보여 줄 결과. */
    public static DuoMatchResultModel forPostOwner(DuoPost duoPost, DuoRequest duoRequest) {
        return withPartner(duoPost, duoRequest, duoRequest.getGameName(), duoRequest.getTagLine());
    }

    // Riot ID 는 생성 시점 스냅샷이다. 스냅샷이 없는 예전 행이면 null 로 나간다.
    private static DuoMatchResultModel withPartner(DuoPost duoPost, DuoRequest duoRequest,
            String partnerGameName, String partnerTagLine) {
        return DuoMatchResultModel.builder()
                .duoPostId(duoPost.getId())
                .requestId(duoRequest.getId())
                .partnerGameName(partnerGameName)
                .partnerTagLine(partnerTagLine)
                .status(duoRequest.getStatus().name())
                .build();
    }
}
