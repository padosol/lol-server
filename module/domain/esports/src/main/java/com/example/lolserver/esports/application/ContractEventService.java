package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.application.command.ContractCommand;
import com.example.lolserver.esports.application.command.DatedCommand;
import com.example.lolserver.esports.application.command.HiatusCommand;
import com.example.lolserver.esports.application.command.LoanCommand;
import com.example.lolserver.esports.application.command.MoveCommand;
import com.example.lolserver.esports.application.model.readmodel.CareerEventReadModel;
import com.example.lolserver.esports.application.model.resultmodel.ContractEventResultModel;
import com.example.lolserver.esports.application.port.in.ContractEventUseCase;
import com.example.lolserver.esports.application.port.out.EsportsQueryPort;
import com.example.lolserver.esports.application.port.out.PersonPersistencePort;
import com.example.lolserver.esports.application.port.out.PersonTimelinePort;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
import com.example.lolserver.esports.domain.Contract;
import com.example.lolserver.esports.domain.PersonTimeline;
import com.example.lolserver.esports.domain.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** 흐름: 인물 확인 → 팀 조회 → 타임라인 읽기 → 도메인 규칙 → 저장 → 저장 뒤 상태·바뀐 계약 조회. */
@Service
@RequiredArgsConstructor
@Transactional
public class ContractEventService implements ContractEventUseCase {

    private final PersonPersistencePort personPersistencePort;
    private final TeamPersistencePort teamPersistencePort;
    private final PersonTimelinePort personTimelinePort;
    private final EsportsQueryPort esportsQueryPort;

    @Override
    public ContractEventResultModel sign(String personId, ContractCommand command) {
        validatePerson(personId);
        Team team = findTeam(command.teamId());
        return apply(personId, t -> t.sign(team, command.role(), command.position(), command.date(),
                command.endDate(), command.note()));
    }

    @Override
    public ContractEventResultModel renew(String personId, ContractCommand command) {
        validatePerson(personId);
        Team team = findTeam(command.teamId());
        return apply(personId, t -> t.renew(team, command.role(), command.position(), command.date(),
                command.endDate(), command.note()));
    }

    @Override
    public ContractEventResultModel transfer(String personId, ContractCommand command) {
        validatePerson(personId);
        Team team = findTeam(command.teamId());
        return apply(personId, t -> t.transfer(team, command.role(), command.position(), command.date(),
                command.endDate(), command.note()));
    }

    @Override
    public ContractEventResultModel move(String personId, MoveCommand command) {
        validatePerson(personId);
        Team team = findTeam(command.teamId());
        return apply(personId, t -> t.move(team, command.role(), command.position(), command.date(),
                command.note()));
    }

    @Override
    public ContractEventResultModel loan(String personId, LoanCommand command) {
        validatePerson(personId);
        Team team = findTeam(command.teamId());
        return apply(personId, t -> t.loan(team, command.position(), command.date(), command.endDate(),
                command.note()));
    }

    @Override
    public ContractEventResultModel recall(String personId, Long contractId, DatedCommand command) {
        validatePerson(personId);
        return apply(personId, t -> t.recall(contractId, command.date()));
    }

    @Override
    public ContractEventResultModel release(String personId, DatedCommand command) {
        validatePerson(personId);
        return apply(personId, t -> t.release(command.date()));
    }

    @Override
    public ContractEventResultModel retire(String personId, DatedCommand command) {
        validatePerson(personId);
        return apply(personId, t -> t.retire(command.date(), command.note(), command.sourceUrl()));
    }

    @Override
    public ContractEventResultModel unretire(String personId, DatedCommand command) {
        validatePerson(personId);
        return apply(personId, t -> t.unretire(command.date(), command.note(), command.sourceUrl()));
    }

    @Override
    public ContractEventResultModel hiatus(String personId, HiatusCommand command) {
        validatePerson(personId);
        return apply(personId, t -> t.hiatus(command.date(), command.reason(), command.note(),
                command.sourceUrl()));
    }

    @Override
    public ContractEventResultModel comeBack(String personId, DatedCommand command) {
        validatePerson(personId);
        return apply(personId, t -> t.comeBack(command.date(), command.note(), command.sourceUrl()));
    }

    private ContractEventResultModel apply(String personId, Consumer<PersonTimeline> event) {
        PersonTimeline timeline = personTimelinePort.load(personId);
        event.accept(timeline);
        personTimelinePort.save(timeline);
        List<Long> changedContractIds = Stream.concat(timeline.endedContracts().stream(),
                        timeline.newContracts().stream())
                .map(Contract::getContractId)
                .toList();
        return new ContractEventResultModel(personId,
                esportsQueryPort.findPersonStatus(personId).orElse(null),
                esportsQueryPort.findContractsByIds(changedContractIds),
                timeline.newEvents().stream().map(CareerEventReadModel::of).toList());
    }

    private void validatePerson(String personId) {
        if (!personPersistencePort.exists(personId)) {
            throw new CoreException(ErrorType.PERSON_NOT_FOUND);
        }
    }

    private Team findTeam(String teamId) {
        return teamPersistencePort.findById(teamId)
                .orElseThrow(() -> new CoreException(ErrorType.ESPORTS_TEAM_NOT_FOUND));
    }
}
