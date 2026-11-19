package com.example.lolserver.esports.adapter.out.persistence;

import com.example.lolserver.esports.EsportsPostgresTestBase;
import com.example.lolserver.esports.application.port.out.PersonPersistencePort;
import com.example.lolserver.esports.domain.Person;
import com.example.lolserver.esports.domain.PersonSummoner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class PersonPersistenceAdapterTest extends EsportsPostgresTestBase {

    @Autowired
    private PersonPersistencePort personPersistencePort;

    @DisplayName("대표 계정을 바꾸면 이전 대표 계정이 해제된다 (인물당 대표 1개 제약에 걸리지 않는다)")
    @Test
    void saveSummoner_대표계정을_바꾸면_이전대표가_해제된다() {
        personPersistencePort.save(Person.builder().personId("faker").nickName("Faker").build());
        personPersistencePort.saveSummoner(summoner("puuid-1", true));

        personPersistencePort.saveSummoner(summoner("puuid-2", true));

        assertThat(personPersistencePort.findSummoner("puuid-1"))
                .hasValueSatisfying(s -> assertThat(s.isMain()).isFalse());
        assertThat(personPersistencePort.findSummoner("puuid-2"))
                .hasValueSatisfying(s -> assertThat(s.isMain()).isTrue());
    }

    @DisplayName("계약·사건·소환사 연결 중 하나라도 있으면 기록이 있는 인물이다")
    @Test
    void hasRecords_계약이나_사건이나_소환사가_있으면_true() {
        personPersistencePort.save(Person.builder().personId("faker").nickName("Faker").build());
        personPersistencePort.save(Person.builder().personId("creator").nickName("Creator").build());
        personPersistencePort.save(Person.builder().personId("retired").nickName("Retired").build());
        personPersistencePort.saveSummoner(summoner("puuid-1", false));
        sql("INSERT INTO esports_career_event (person_id, event_type, event_date) VALUES ('retired', 'RETIRE', '2020-01-01');");

        assertThat(personPersistencePort.hasRecords("faker")).isTrue();
        assertThat(personPersistencePort.hasRecords("retired")).isTrue();
        assertThat(personPersistencePort.hasRecords("creator")).isFalse();
    }

    private static PersonSummoner summoner(String puuid, boolean main) {
        return PersonSummoner.builder().puuid(puuid).personId("faker").main(main).visible(true).build();
    }
}
