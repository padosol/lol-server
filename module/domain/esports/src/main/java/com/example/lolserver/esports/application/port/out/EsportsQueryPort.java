package com.example.lolserver.esports.application.port.out;

import com.example.lolserver.common.support.PageResult;
import com.example.lolserver.esports.application.command.PersonSearchCommand;
import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.application.model.readmodel.LeagueReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonCareerReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonProfileReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonStatusReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonSummaryReadModel;
import com.example.lolserver.esports.application.model.readmodel.RosterIssueReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonRosterReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamDetailReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamMemberReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamRosterReadModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 로스터 조회. V5 유도 뷰를 그대로 읽는다 (만료·재계약 유도, 소속 기간 묶기, 임대 표시를 다시 계산하지 않는다). */
public interface EsportsQueryPort {

    List<LeagueReadModel> findLeagues();

    List<SeasonReadModel> findSeasons(String leagueId);

    List<TeamReadModel> findTeams(String leagueId, boolean includeDisbanded);

    Optional<TeamDetailReadModel> findTeamDetail(String teamId);

    TeamRosterReadModel findCurrentRoster(String teamId);

    List<TeamMemberReadModel> findTeamMembers(String teamId);

    List<SeasonRosterReadModel> findSeasonRoster(String seasonId, String teamId);

    PageResult<PersonSummaryReadModel> searchPersons(PersonSearchCommand command);

    Optional<PersonProfileReadModel> findPersonProfile(String personId);

    Optional<PersonCareerReadModel> findPersonCareer(String personId);

    /** 공개(visible) 연결만. 이력 없는 인물도 돌려주되 status 는 null. */
    Optional<PersonSummaryReadModel> findPersonByPuuid(String puuid);

    Optional<PersonStatusReadModel> findPersonStatus(String personId);

    List<ContractReadModel> findContracts(String personId);

    List<ContractReadModel> findContractsByIds(Collection<Long> contractIds);

    List<RosterIssueReadModel> findRosterIssues();
}
