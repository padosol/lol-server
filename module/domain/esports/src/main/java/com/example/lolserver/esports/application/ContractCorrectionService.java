package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.application.command.CorrectContractCommand;
import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.application.model.readmodel.RosterIssueReadModel;
import com.example.lolserver.esports.application.port.in.ContractCorrectionUseCase;
import com.example.lolserver.esports.application.port.out.EsportsQueryPort;
import com.example.lolserver.esports.application.port.out.PersonPersistencePort;
import com.example.lolserver.esports.application.port.out.PersonTimelinePort;
import com.example.lolserver.esports.domain.Contract;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractCorrectionService implements ContractCorrectionUseCase {

    private final PersonPersistencePort personPersistencePort;
    private final PersonTimelinePort personTimelinePort;
    private final EsportsQueryPort esportsQueryPort;

    @Override
    @Transactional(readOnly = true)
    public List<ContractReadModel> getContracts(String personId) {
        if (!personPersistencePort.exists(personId)) {
            throw new CoreException(ErrorType.PERSON_NOT_FOUND);
        }
        return esportsQueryPort.findContracts(personId);
    }

    @Override
    public ContractReadModel correctContract(Long contractId, CorrectContractCommand command) {
        Contract contract = findContract(contractId);
        contract.correct(command.startDate(), command.endDate(), command.actualEndDate(), command.endReason(),
                command.position(), command.note());
        personTimelinePort.saveContract(contract);
        return esportsQueryPort.findContractsByIds(List.of(contractId)).stream()
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));
    }

    @Override
    public void deleteContract(Long contractId) {
        findContract(contractId);
        personTimelinePort.deleteContract(contractId);
    }

    @Override
    public void deleteCareerEvent(Long eventId) {
        personTimelinePort.findEvent(eventId)
                .orElseThrow(() -> new CoreException(ErrorType.CAREER_EVENT_NOT_FOUND));
        personTimelinePort.deleteEvent(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RosterIssueReadModel> getRosterIssues() {
        return esportsQueryPort.findRosterIssues();
    }

    private Contract findContract(Long contractId) {
        return personTimelinePort.findContract(contractId)
                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));
    }
}
