package com.example.lolserver.esports.domain.vo;

/** 계약이 어떻게 시작됐나. */
public enum ContractType {
    /** 신규 입단 (FA 영입·신인·은퇴 후 스태프 합류) */
    SIGN,
    /** 이적 (계약 중인 사람을 데려옴. 직전 계약은 TRANSFERRED 로 끝난다) */
    TRANSFER,
    /** 재계약 (같은 조직 직전 계약을 잇는다) */
    RENEWAL,
    /** 임대 (원 계약은 그대로 유지) */
    LOAN,
    /** 팀 내 이동 (콜업·샌드다운·역할·포지션 변경. 직전 계약은 MOVED 로 끝나고 end_date 를 물려받는다) */
    MOVE
}
