package com.example.lolserver.esports.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamTest {

    private static final TeamName T1_NAME = new TeamName("T1", "T1", "T1", "t1.png", "t1-dark.png");
    private static final TeamName NEW_NAME = new TeamName("T1X", "T1 X", "T1 X", "t1x.png", null);

    @DisplayName("조직 ID 는 상위 팀이 있으면 상위 팀, 없으면 자기 자신")
    @Test
    void organizationId_상위팀이_있으면_상위팀() {
        assertThat(team("t1a", "t1").organizationId()).isEqualTo("t1");
        assertThat(team("t1", null).organizationId()).isEqualTo("t1");
    }

    @DisplayName("상위 팀에 또 상위 팀이 있으면 거부한다 (1단계만)")
    @Test
    void validateParent_상위팀에_상위팀이_있으면_TEAM_NESTED_TOO_DEEP() {
        Team x = team("x", null);

        assertThatThrownBy(() -> x.validateParent(team("t1a", "t1")))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.TEAM_NESTED_TOO_DEEP);
    }

    @DisplayName("자기 자신을 상위 팀으로 지정하면 거부한다")
    @Test
    void validateParent_자기자신이면_TEAM_NESTED_TOO_DEEP() {
        Team t1 = team("t1", null);

        assertThatThrownBy(() -> t1.validateParent(team("t1", null)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.TEAM_NESTED_TOO_DEEP);
    }

    @DisplayName("개명하면 현재 이름이 [nameSince, 개명일 전날] 이력으로 옮겨가고 팀은 새 이름을 쓴다")
    @Test
    void rename_현재이름을_이력으로_옮긴다() {
        Team t1 = Team.builder().teamId("t1").gameCode("lol").teamCode("T1").name("T1").nameEng("T1")
                .imageUrl("t1.png").darkImageUrl("t1-dark.png").nameSince(LocalDate.of(2020, 1, 1)).build();

        TeamNameHistory history = t1.rename(NEW_NAME, LocalDate.of(2026, 11, 20), null, "리브랜딩");

        assertThat(history.getTeamId()).isEqualTo("t1");
        assertThat(history.getTeamCode()).isEqualTo("T1");
        assertThat(history.getName()).isEqualTo("T1");
        assertThat(history.getImageUrl()).isEqualTo("t1.png");
        assertThat(history.getValidFrom()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(history.getValidTo()).isEqualTo(LocalDate.of(2026, 11, 19));
        assertThat(history.getNote()).isEqualTo("리브랜딩");
        assertThat(t1.getTeamCode()).isEqualTo("T1X");
        assertThat(t1.getName()).isEqualTo("T1 X");
        assertThat(t1.getDarkImageUrl()).isNull();
        assertThat(t1.getNameSince()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @DisplayName("nameSince·foundedAt 이 모두 없으면 previousNameFrom 을 이력 시작일로 쓰고, 셋 다 없으면 거부한다")
    @Test
    void rename_nameSince와_foundedAt이_없으면_previousNameFrom을_쓴다() {
        Team withoutDates = team("t1", null);

        TeamNameHistory history = withoutDates.rename(NEW_NAME, LocalDate.of(2026, 11, 20),
                LocalDate.of(2019, 5, 1), null);

        assertThat(history.getValidFrom()).isEqualTo(LocalDate.of(2019, 5, 1));
        assertThatThrownBy(() -> team("t2", null).rename(NEW_NAME, LocalDate.of(2026, 11, 20), null, null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.INVALID_INPUT);
    }

    @DisplayName("개명일이 현재 이름 시작일 이하이면 거부한다")
    @Test
    void rename_개명일이_validFrom이하면_INVALID_INPUT() {
        Team t1 = Team.builder().teamId("t1").gameCode("lol").teamCode("T1").name("T1")
                .nameSince(LocalDate.of(2026, 11, 20)).build();

        assertThatThrownBy(() -> t1.rename(NEW_NAME, LocalDate.of(2026, 11, 20), null, null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.INVALID_INPUT);
    }

    private static Team team(String teamId, String parentTeamId) {
        return Team.builder().teamId(teamId).gameCode("lol").teamCode(T1_NAME.teamCode()).name(T1_NAME.name())
                .parentTeamId(parentTeamId).build();
    }
}
