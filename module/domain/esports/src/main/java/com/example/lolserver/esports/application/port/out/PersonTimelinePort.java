package com.example.lolserver.esports.application.port.out;

import com.example.lolserver.esports.domain.CareerEvent;
import com.example.lolserver.esports.domain.Contract;
import com.example.lolserver.esports.domain.PersonTimeline;

import java.util.Optional;

public interface PersonTimelinePort {

    /** 인물의 계약(조직 ID 포함)·사건을 시간순으로 읽는다. */
    PersonTimeline load(String personId);

    /**
     * 사건이 바꾼 것을 저장한다. 조기 종료된 기존 계약(UPDATE)을 먼저 반영하고 새 계약·사건을 넣는다 —
     * 순서가 바뀌면 새 계약이 기간 겹침 제약에 걸린다. 생성된 ID 를 채운 같은 타임라인을 돌려준다.
     */
    PersonTimeline save(PersonTimeline timeline);

    Optional<Contract> findContract(Long contractId);

    Contract saveContract(Contract contract);

    void deleteContract(Long contractId);

    Optional<CareerEvent> findEvent(Long eventId);

    void deleteEvent(Long eventId);
}
