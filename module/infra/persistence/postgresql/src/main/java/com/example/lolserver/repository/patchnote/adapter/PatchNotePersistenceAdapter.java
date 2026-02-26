package com.example.lolserver.repository.patchnote.adapter;

import com.example.lolserver.domain.patchnote.application.model.PatchNoteReadModel;
import com.example.lolserver.domain.patchnote.application.model.PatchNoteSummaryReadModel;
import com.example.lolserver.domain.patchnote.application.port.out.PatchNotePersistencePort;
import com.example.lolserver.repository.patchnote.PatchNoteJpaRepository;
import com.example.lolserver.repository.patchnote.entity.PatchNoteEntity;
import com.example.lolserver.repository.patchnote.mapper.PatchNoteMapper;
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
