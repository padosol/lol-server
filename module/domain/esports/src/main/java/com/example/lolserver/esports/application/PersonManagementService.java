package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.application.command.LinkSummonerCommand;
import com.example.lolserver.esports.application.command.PersonCommand;
import com.example.lolserver.esports.application.port.in.PersonManagementUseCase;
import com.example.lolserver.esports.application.port.out.PersonPersistencePort;
import com.example.lolserver.esports.domain.Person;
import com.example.lolserver.esports.domain.PersonSummoner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonManagementService implements PersonManagementUseCase {

    private final PersonPersistencePort personPersistencePort;

    @Override
    public Person createPerson(PersonCommand command) {
        if (personPersistencePort.exists(command.personId())) {
            throw new CoreException(ErrorType.ESPORTS_ID_DUPLICATED);
        }
        return personPersistencePort.save(toPerson(command.personId(), command));
    }

    @Override
    public Person updatePerson(String personId, PersonCommand command) {
        validateExists(personId);
        return personPersistencePort.save(toPerson(personId, command));
    }

    @Override
    public void deletePerson(String personId) {
        validateExists(personId);
        if (personPersistencePort.hasRecords(personId)) {
            throw new CoreException(ErrorType.PERSON_IN_USE);
        }
        personPersistencePort.delete(personId);
    }

    @Override
    public PersonSummoner linkSummoner(String personId, String puuid, LinkSummonerCommand command) {
        validateExists(personId);
        validateOwnedBy(personId, personPersistencePort.findSummoner(puuid));
        return personPersistencePort.saveSummoner(PersonSummoner.builder()
                .puuid(puuid)
                .personId(personId)
                .main(command.main())
                .visible(command.visible())
                .note(command.note())
                .build());
    }

    /** 연결이 없으면 아무것도 하지 않는다(DELETE 는 멱등). 다른 인물의 연결이면 거부한다. */
    @Override
    public void unlinkSummoner(String personId, String puuid) {
        validateExists(personId);
        Optional<PersonSummoner> summoner = personPersistencePort.findSummoner(puuid);
        validateOwnedBy(personId, summoner);
        if (summoner.isPresent()) {
            personPersistencePort.deleteSummoner(puuid);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonSummoner> getSummoners(String personId) {
        validateExists(personId);
        return personPersistencePort.findSummoners(personId);
    }

    private void validateExists(String personId) {
        if (!personPersistencePort.exists(personId)) {
            throw new CoreException(ErrorType.PERSON_NOT_FOUND);
        }
    }

    private static void validateOwnedBy(String personId, Optional<PersonSummoner> summoner) {
        if (summoner.isPresent() && !summoner.get().getPersonId().equals(personId)) {
            throw new CoreException(ErrorType.SUMMONER_ALREADY_LINKED);
        }
    }

    private static Person toPerson(String personId, PersonCommand command) {
        return Person.builder()
                .personId(personId)
                .nickName(command.nickName())
                .realName(command.realName())
                .realNameEng(command.realNameEng())
                .birthDate(command.birthDate())
                .nationality(command.nationality())
                .imageUrl(command.imageUrl())
                .build();
    }
}
