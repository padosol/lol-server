package com.example.lolserver.esports.application.port.out;

import com.example.lolserver.esports.domain.Person;
import com.example.lolserver.esports.domain.PersonSummoner;

import java.util.List;
import java.util.Optional;

public interface PersonPersistencePort {

    boolean exists(String personId);

    Optional<Person> findById(String personId);

    Person save(Person person);

    void delete(String personId);

    /** 계약·커리어 사건·소환사 연결 중 하나라도 있는가 — 삭제 검사용. */
    boolean hasRecords(String personId);

    Optional<PersonSummoner> findSummoner(String puuid);

    List<PersonSummoner> findSummoners(String personId);

    /** 대표 계정으로 저장하면 같은 인물의 다른 대표 계정을 먼저 해제한다. */
    PersonSummoner saveSummoner(PersonSummoner summoner);

    void deleteSummoner(String puuid);
}
