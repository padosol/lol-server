package com.example.lolserver.gamedata.adapter.out.persistence;

import com.example.lolserver.gamedata.application.model.PatchNoteReadModel;
import com.example.lolserver.gamedata.application.model.PatchNoteSummaryReadModel;
import com.example.lolserver.gamedata.application.port.out.PatchNotePersistencePort;
import com.example.lolserver.gamedata.adapter.out.persistence.entity.PatchNoteEntity;
import com.example.lolserver.gamedata.adapter.out.persistence.mapper.PatchNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PatchNotePersistenceAdapter implements PatchNotePersistencePort {

    private final PatchNoteJpaRepository patchNoteJpaRepository;
    private final PatchNoteMapper patchNoteMapper;

    @Override
    public List<PatchNoteSummaryReadModel> findAllSummary() {
        List<PatchNoteEntity> entities = patchNoteJpaRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return patchNoteMapper.toSummaryReadModelList(entities);
    }

    @Override
    public Optional<PatchNoteReadModel> findByVersionId(String versionId) {
        return patchNoteJpaRepository.findById(versionId)
                .map(patchNoteMapper::toReadModel);
    }
}
