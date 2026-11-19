package com.example.lolserver.esports.adapter.out.persistence.adapter;

import com.example.lolserver.esports.adapter.out.persistence.mapper.EsportsMasterMapper;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsCareerEventJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsContractJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.repository.PersonJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.repository.PersonSummonerJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.support.EsportsConstraintTranslator;
import com.example.lolserver.esports.application.port.out.PersonPersistencePort;
import com.example.lolserver.esports.domain.Person;
import com.example.lolserver.esports.domain.PersonSummoner;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PersonPersistenceAdapter implements PersonPersistencePort {

    private final PersonJpaRepository personRepository;
    private final PersonSummonerJpaRepository summonerRepository;
    private final EsportsContractJpaRepository contractRepository;
    private final EsportsCareerEventJpaRepository eventRepository;
    private final EsportsMasterMapper mapper;

    @Override
    public boolean exists(String personId) {
        return personRepository.existsById(personId);
    }

    @Override
    public Optional<Person> findById(String personId) {
        return personRepository.findById(personId).map(mapper::toDomain);
    }

    @Override
    public Person save(Person person) {
        try {
            return mapper.toDomain(personRepository.saveAndFlush(mapper.toEntity(person)));
        } catch (DataIntegrityViolationException e) {
            throw EsportsConstraintTranslator.translate(e);
        }
    }

    @Override
    public void delete(String personId) {
        try {
            personRepository.deleteById(personId);
            personRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw EsportsConstraintTranslator.translate(e);
        }
    }

    @Override
    public boolean hasRecords(String personId) {
        return contractRepository.existsByPersonId(personId)
                || eventRepository.existsByPersonId(personId)
                || summonerRepository.existsByPersonId(personId);
    }

    @Override
    public Optional<PersonSummoner> findSummoner(String puuid) {
        return summonerRepository.findById(puuid).map(mapper::toDomain);
    }

    @Override
    public List<PersonSummoner> findSummoners(String personId) {
        return summonerRepository.findByPersonIdOrderByMainDescCreatedAtAsc(personId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PersonSummoner saveSummoner(PersonSummoner summoner) {
        try {
            if (summoner.isMain()) {
                summonerRepository.unsetMainExcept(summoner.getPersonId(), summoner.getPuuid());
            }
            return mapper.toDomain(summonerRepository.saveAndFlush(mapper.toEntity(summoner)));
        } catch (DataIntegrityViolationException e) {
            throw EsportsConstraintTranslator.translate(e);
        }
    }

    @Override
    public void deleteSummoner(String puuid) {
        summonerRepository.deleteById(puuid);
        summonerRepository.flush();
    }
}
