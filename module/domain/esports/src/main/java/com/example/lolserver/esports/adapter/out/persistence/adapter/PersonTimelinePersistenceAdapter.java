package com.example.lolserver.esports.adapter.out.persistence.adapter;

import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsContractEntity;
import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsTeamEntity;
import com.example.lolserver.esports.adapter.out.persistence.mapper.EsportsContractMapper;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsCareerEventJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsContractJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.repository.EsportsTeamJpaRepository;
import com.example.lolserver.esports.adapter.out.persistence.support.EsportsConstraintTranslator;
import com.example.lolserver.esports.application.port.out.PersonTimelinePort;
import com.example.lolserver.esports.domain.CareerEvent;
import com.example.lolserver.esports.domain.Contract;
import com.example.lolserver.esports.domain.PersonTimeline;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 인물 타임라인 저장. Hibernate 는 IDENTITY INSERT 를 쌓인 UPDATE 보다 먼저 보낸다.
 * 이적·이동·조기 연장에서 새 계약 INSERT 가 직전 계약 조기 종료 UPDATE 보다 먼저 나가면 기간 겹침 제약
 * (ex_contract_no_overlap)에 걸리므로, 조기 종료를 먼저 flush 한 뒤 새 계약을 넣는다.
 */
@Component
@RequiredArgsConstructor
public class PersonTimelinePersistenceAdapter implements PersonTimelinePort {

    private final EsportsContractJpaRepository contractRepository;
    private final EsportsCareerEventJpaRepository eventRepository;
    private final EsportsTeamJpaRepository teamRepository;
    private final EsportsContractMapper mapper;

    @Override
    public PersonTimeline load(String personId) {
        List<EsportsContractEntity> entities =
                contractRepository.findByPersonIdOrderByStartDateAscContractIdAsc(personId);
        Map<String, String> organizations = organizations(
                entities.stream().map(EsportsContractEntity::getTeamId).collect(Collectors.toSet()));
        List<Contract> contracts = entities.stream()
                .map(e -> mapper.toDomain(e, organizations.getOrDefault(e.getTeamId(), e.getTeamId())))
                .toList();
        List<CareerEvent> events = eventRepository.findByPersonIdOrderByEventDateAscEventIdAsc(personId).stream()
                .map(mapper::toDomain)
                .toList();
        return new PersonTimeline(personId, contracts, events);
    }

    @Override
    public PersonTimeline save(PersonTimeline timeline) {
        try {
            contractRepository.saveAll(timeline.endedContracts().stream().map(mapper::toEntity).toList());
            contractRepository.flush();
            for (Contract contract : timeline.newContracts()) {
                contract.assignId(contractRepository.save(mapper.toEntity(contract)).getContractId());
            }
            contractRepository.flush();
            for (CareerEvent event : timeline.newEvents()) {
                event.assignId(eventRepository.save(mapper.toEntity(event)).getEventId());
            }
            eventRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw EsportsConstraintTranslator.translate(e);
        }
        return timeline;
    }

    @Override
    public Optional<Contract> findContract(Long contractId) {
        return contractRepository.findById(contractId).map(this::toDomain);
    }

    @Override
    public Contract saveContract(Contract contract) {
        try {
            return toDomain(contractRepository.saveAndFlush(mapper.toEntity(contract)));
        } catch (DataIntegrityViolationException e) {
            throw EsportsConstraintTranslator.translate(e);
        }
    }

    @Override
    public void deleteContract(Long contractId) {
        contractRepository.deleteById(contractId);
        contractRepository.flush();
    }

    @Override
    public Optional<CareerEvent> findEvent(Long eventId) {
        return eventRepository.findById(eventId).map(mapper::toDomain);
    }

    @Override
    public void deleteEvent(Long eventId) {
        eventRepository.deleteById(eventId);
        eventRepository.flush();
    }

    private Contract toDomain(EsportsContractEntity entity) {
        String teamId = entity.getTeamId();
        return mapper.toDomain(entity, organizations(List.of(teamId)).getOrDefault(teamId, teamId));
    }

    /** 팀 ID → 조직 ID (상위 팀이 있으면 상위 팀). */
    private Map<String, String> organizations(Collection<String> teamIds) {
        return teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(EsportsTeamEntity::getTeamId,
                        t -> t.getParentTeamId() != null ? t.getParentTeamId() : t.getTeamId()));
    }
}
