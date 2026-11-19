package com.example.lolserver.esports.adapter.in.web;

import com.example.lolserver.esports.application.model.readmodel.CareerEventReadModel;
import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonStatusReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonSummaryReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamReadModel;
import com.example.lolserver.esports.application.model.readmodel.TenureReadModel;
import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.ContractStatus;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.PersonStatus;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;

import java.time.LocalDate;
import java.util.List;

/** 컨트롤러 테스트용 ReadModel. */
public final class EsportsReadModelFixtures {

    public static final TeamReadModel T1 = new TeamReadModel("t1", "T1", "T1", "T1", "t1.png", null, "lck", null,
            LocalDate.of(2013, 2, 1), null, LocalDate.of(2019, 10, 25));
    public static final TeamReadModel T1A = new TeamReadModel("t1a", "T1A", "T1 Esports Academy", null, null, null,
            "lck_cl", "t1", null, null, null);
    public static final PersonStatusReadModel CONTRACTED = new PersonStatusReadModel(PersonStatus.CONTRACTED, "t1",
            "t1", Role.PLAYER, Position.MID, LocalDate.of(2029, 11, 19), false, null, false, null, null, null, null);
    public static final PersonSummaryReadModel FAKER = new PersonSummaryReadModel("faker", "Faker", "이상혁", "KR",
            null, "T1", CONTRACTED);
    public static final TenureReadModel T1_TENURE = new TenureReadModel("faker", "Faker", "t1", true,
            LocalDate.of(2013, 2, 1), null, null, 6, 5, false, List.of(Role.PLAYER), List.of("t1"));
    public static final ContractReadModel RENEWAL = new ContractReadModel(11L, "t1", "T1", Role.PLAYER, Position.MID,
            ContractType.RENEWAL, LocalDate.of(2026, 11, 17), LocalDate.of(2029, 11, 19), null,
            LocalDate.of(2029, 11, 19), ContractStatus.ACTIVE, null, null);
    public static final ContractReadModel EXPIRED = new ContractReadModel(10L, "t1", "T1", Role.PLAYER, Position.MID,
            ContractType.RENEWAL, LocalDate.of(2024, 11, 18), LocalDate.of(2026, 11, 16), null,
            LocalDate.of(2026, 11, 16), ContractStatus.ENDED, EndReason.RENEWED, null);
    public static final CareerEventReadModel HIATUS = new CareerEventReadModel(3L, CareerEventType.HIATUS,
            LocalDate.of(2020, 1, 1), "t1", null, "휴식", null);

    private EsportsReadModelFixtures() {
    }
}
