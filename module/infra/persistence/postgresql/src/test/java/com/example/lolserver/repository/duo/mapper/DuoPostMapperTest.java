package com.example.lolserver.repository.duo.mapper;

import com.example.lolserver.domain.duo.domain.DuoPost;
import com.example.lolserver.domain.duo.domain.vo.DuoPostStatus;
import com.example.lolserver.domain.duo.domain.vo.Lane;
import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;
import com.example.lolserver.repository.duo.entity.DuoPostEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DuoPostMapperTest {

    private final DuoPostMapper mapper = new DuoPostMapperImpl();

    @DisplayName("Entity에서 Domain으로 정상 변환한다 - String을 Lane/DuoPostStatus enum으로 변환하고 tierRank를 rank로 매핑한다")
    @Test
    void toDomain_validEntity_returnsDuoPost() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2026, 4, 14, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 13, 10, 0);

        DuoPostEntity entity = DuoPostEntity.builder()
                .id(1L)
                .memberId(100L)
                .puuid("test-puuid-abc123")
                .primaryLane("MID")
                .desiredLane("SUPPORT")
                .hasMicrophone(true)
                .tier("GOLD")
                .tierRank("II")
                .leaguePoints(75)
                .memo("듀오 구합니다")
                .mostChampions(List.of(
                        new MostChampion(1, "Jinx", 50, 30, 20),
                        new MostChampion(2, "Lux", 40, 25, 15)
                ))
                .recentGameSummary(new RecentGameSummary(7, 3, List.of(
                        new RecentGameSummary.PlayedChampion(1, "Jinx"),
                        new RecentGameSummary.PlayedChampion(2, "Lux")
                )))
                .status("ACTIVE")
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // when
        DuoPost result = mapper.toDomain(entity);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMemberId()).isEqualTo(100L);
        assertThat(result.getPuuid()).isEqualTo("test-puuid-abc123");
        assertThat(result.getPrimaryLane()).isEqualTo(Lane.MID);
        assertThat(result.getDesiredLane()).isEqualTo(Lane.SUPPORT);
        assertThat(result.isHasMicrophone()).isTrue();
        assertThat(result.getTier()).isEqualTo("GOLD");
        assertThat(result.getRank()).isEqualTo("II");
        assertThat(result.getLeaguePoints()).isEqualTo(75);
        assertThat(result.getMemo()).isEqualTo("듀오 구합니다");
        assertThat(result.getStatus()).isEqualTo(DuoPostStatus.ACTIVE);
        assertThat(result.getMostChampions()).hasSize(2);
        assertThat(result.getMostChampions().get(0).championName()).isEqualTo("Jinx");
        assertThat(result.getRecentGameSummary().wins()).isEqualTo(7);
        assertThat(result.getRecentGameSummary().losses()).isEqualTo(3);
        assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @DisplayName("Domain에서 Entity로 정상 변환한다 - Lane/DuoPostStatus enum을 String으로 변환하고 rank를 tierRank로 매핑한다")
    @Test
    void toEntity_validDomain_returnsDuoPostEntity() {
        // given
        LocalDateTime expiresAt = LocalDateTime.of(2026, 4, 14, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 13, 10, 0);

        DuoPost domain = DuoPost.builder()
                .id(1L)
                .memberId(100L)
                .puuid("test-puuid-abc123")
                .primaryLane(Lane.TOP)
                .desiredLane(Lane.JUNGLE)
                .hasMicrophone(false)
                .tier("PLATINUM")
                .rank("IV")
                .leaguePoints(30)
                .memo("정글러 구합니다")
                .mostChampions(List.of(new MostChampion(3, "LeeSin", 60, 35, 25)))
                .recentGameSummary(new RecentGameSummary(5, 5, List.of(
                        new RecentGameSummary.PlayedChampion(3, "LeeSin")
                )))
                .status(DuoPostStatus.MATCHED)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        // when
        DuoPostEntity result = mapper.toEntity(domain);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMemberId()).isEqualTo(100L);
        assertThat(result.getPuuid()).isEqualTo("test-puuid-abc123");
        assertThat(result.getPrimaryLane()).isEqualTo("TOP");
        assertThat(result.getDesiredLane()).isEqualTo("JUNGLE");
        assertThat(result.isHasMicrophone()).isFalse();
        assertThat(result.getTier()).isEqualTo("PLATINUM");
        assertThat(result.getTierRank()).isEqualTo("IV");
        assertThat(result.getLeaguePoints()).isEqualTo(30);
        assertThat(result.getMemo()).isEqualTo("정글러 구합니다");
        assertThat(result.getStatus()).isEqualTo("MATCHED");
        assertThat(result.getMostChampions()).hasSize(1);
        assertThat(result.getMostChampions().get(0).championName()).isEqualTo("LeeSin");
        assertThat(result.getRecentGameSummary().wins()).isEqualTo(5);
        assertThat(result.getRecentGameSummary().losses()).isEqualTo(5);
        assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @DisplayName("Entity 리스트를 Domain 리스트로 변환한다")
    @Test
    void toDomainList_entityList_returnsDomainList() {
        // given
        DuoPostEntity entity1 = DuoPostEntity.builder()
                .id(1L)
                .memberId(100L)
                .puuid("puuid-1")
                .primaryLane("MID")
                .desiredLane("ADC")
                .hasMicrophone(true)
                .tier("GOLD")
                .tierRank("I")
                .leaguePoints(50)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        DuoPostEntity entity2 = DuoPostEntity.builder()
                .id(2L)
                .memberId(200L)
                .puuid("puuid-2")
                .primaryLane("SUPPORT")
                .desiredLane("FILL")
                .hasMicrophone(false)
                .tier("DIAMOND")
                .tierRank("III")
                .leaguePoints(20)
                .status("EXPIRED")
                .expiresAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when
        List<DuoPost> result = mapper.toDomainList(List.of(entity1, entity2));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getPrimaryLane()).isEqualTo(Lane.MID);
        assertThat(result.get(0).getStatus()).isEqualTo(DuoPostStatus.ACTIVE);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getPrimaryLane()).isEqualTo(Lane.SUPPORT);
        assertThat(result.get(1).getStatus()).isEqualTo(DuoPostStatus.EXPIRED);
    }

    @DisplayName("null 입력 시 null을 반환한다")
    @Test
    void toDomain_nullInput_returnsNull() {
        // when
        DuoPost result = mapper.toDomain(null);

        // then
        assertThat(result).isNull();
    }
}
