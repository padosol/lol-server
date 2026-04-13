package com.example.lolserver.repository.duo.mapper;

import com.example.lolserver.domain.duo.domain.DuoRequest;
import com.example.lolserver.domain.duo.domain.vo.DuoRequestStatus;
import com.example.lolserver.domain.duo.domain.vo.Lane;
import com.example.lolserver.repository.duo.entity.DuoRequestEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DuoRequestMapperTest {

    private final DuoRequestMapper mapper = new DuoRequestMapperImpl();

    @DisplayName("Entity에서 Domain으로 정상 변환한다 - String을 Lane/DuoRequestStatus enum으로 변환하고 tierRank를 rank로 매핑한다")
    @Test
    void toDomain_validEntity_returnsDuoRequest() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 13, 10, 0);

        DuoRequestEntity entity = DuoRequestEntity.builder()
                .id(1L)
                .duoPostId(10L)
                .requesterId(200L)
                .requesterPuuid("requester-puuid-abc123")
                .primaryLane("ADC")
                .secondaryLane("SUPPORT")
                .hasMicrophone(true)
                .tier("PLATINUM")
                .tierRank("II")
                .leaguePoints(45)
                .memo("듀오 신청합니다")
                .status("PENDING")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // when
        DuoRequest result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDuoPostId()).isEqualTo(10L);
        assertThat(result.getRequesterId()).isEqualTo(200L);
        assertThat(result.getRequesterPuuid()).isEqualTo("requester-puuid-abc123");
        assertThat(result.getPrimaryLane()).isEqualTo(Lane.ADC);
        assertThat(result.getSecondaryLane()).isEqualTo(Lane.SUPPORT);
        assertThat(result.isHasMicrophone()).isTrue();
        assertThat(result.getTier()).isEqualTo("PLATINUM");
        assertThat(result.getRank()).isEqualTo("II");
        assertThat(result.getLeaguePoints()).isEqualTo(45);
        assertThat(result.getMemo()).isEqualTo("듀오 신청합니다");
        assertThat(result.getStatus()).isEqualTo(DuoRequestStatus.PENDING);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @DisplayName("Domain에서 Entity로 정상 변환한다 - Lane/DuoRequestStatus enum을 String으로 변환하고 rank를 tierRank로 매핑한다")
    @Test
    void toEntity_validDomain_returnsDuoRequestEntity() {
        // given
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 13, 10, 0);

        DuoRequest domain = DuoRequest.builder()
                .id(1L)
                .duoPostId(10L)
                .requesterId(200L)
                .requesterPuuid("requester-puuid-abc123")
                .primaryLane(Lane.JUNGLE)
                .secondaryLane(Lane.TOP)
                .hasMicrophone(false)
                .tier("DIAMOND")
                .rank("I")
                .leaguePoints(80)
                .memo("탑 서브로 갑니다")
                .status(DuoRequestStatus.ACCEPTED)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // when
        DuoRequestEntity result = mapper.toEntity(domain);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDuoPostId()).isEqualTo(10L);
        assertThat(result.getRequesterId()).isEqualTo(200L);
        assertThat(result.getRequesterPuuid()).isEqualTo("requester-puuid-abc123");
        assertThat(result.getPrimaryLane()).isEqualTo("JUNGLE");
        assertThat(result.getSecondaryLane()).isEqualTo("TOP");
        assertThat(result.isHasMicrophone()).isFalse();
        assertThat(result.getTier()).isEqualTo("DIAMOND");
        assertThat(result.getTierRank()).isEqualTo("I");
        assertThat(result.getLeaguePoints()).isEqualTo(80);
        assertThat(result.getMemo()).isEqualTo("탑 서브로 갑니다");
        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @DisplayName("Entity 리스트를 Domain 리스트로 변환한다")
    @Test
    void toDomainList_entityList_returnsDomainList() {
        // given
        DuoRequestEntity entity1 = DuoRequestEntity.builder()
                .id(1L)
                .duoPostId(10L)
                .requesterId(100L)
                .requesterPuuid("puuid-1")
                .primaryLane("MID")
                .secondaryLane("ADC")
                .hasMicrophone(true)
                .tier("GOLD")
                .tierRank("III")
                .leaguePoints(50)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DuoRequestEntity entity2 = DuoRequestEntity.builder()
                .id(2L)
                .duoPostId(10L)
                .requesterId(200L)
                .requesterPuuid("puuid-2")
                .primaryLane("SUPPORT")
                .secondaryLane("FILL")
                .hasMicrophone(false)
                .tier("SILVER")
                .tierRank("I")
                .leaguePoints(90)
                .status("REJECTED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when
        List<DuoRequest> result = mapper.toDomainList(List.of(entity1, entity2));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getPrimaryLane()).isEqualTo(Lane.MID);
        assertThat(result.get(0).getStatus()).isEqualTo(DuoRequestStatus.PENDING);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getPrimaryLane()).isEqualTo(Lane.SUPPORT);
        assertThat(result.get(1).getStatus()).isEqualTo(DuoRequestStatus.REJECTED);
    }

    @DisplayName("null 입력 시 null을 반환한다")
    @Test
    void toDomain_nullInput_returnsNull() {
        // when
        DuoRequest result = mapper.toDomain(null);

        // then
        assertThat(result).isNull();
    }
}
