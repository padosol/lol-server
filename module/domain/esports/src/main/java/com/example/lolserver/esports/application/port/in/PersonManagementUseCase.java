package com.example.lolserver.esports.application.port.in;

import com.example.lolserver.esports.application.command.LinkSummonerCommand;
import com.example.lolserver.esports.application.command.PersonCommand;
import com.example.lolserver.esports.domain.Person;
import com.example.lolserver.esports.domain.PersonSummoner;

import java.util.List;

/** 관리자 — 인물과 소환사(라이엇 계정) 연결. */
public interface PersonManagementUseCase {

    Person createPerson(PersonCommand command);

    Person updatePerson(String personId, PersonCommand command);

    void deletePerson(String personId);

    PersonSummoner linkSummoner(String personId, String puuid, LinkSummonerCommand command);

    void unlinkSummoner(String personId, String puuid);

    /** 비공개 계정을 포함한 전체 연결 (관리 화면용). */
    List<PersonSummoner> getSummoners(String personId);
}
