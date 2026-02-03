package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.patchnote.PatchNoteController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.patchnote.application.PatchNoteService;
import com.example.lolserver.domain.patchnote.application.model.PatchNoteReadModel;
import com.example.lolserver.domain.patchnote.application.model.PatchNoteSummaryReadModel;
import com.example.lolserver.domain.patchnote.application.model.patchnote.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PatchNoteController 테스트")
@ExtendWith(MockitoExtension.class)
class PatchNoteControllerTest extends RestDocsSupport {

    @Mock
    private PatchNoteService patchNoteService;

    @Override
    protected Object initController() {
        return new PatchNoteController(patchNoteService);
    }

    @DisplayName("전체 패치노트 목록 조회 API")
    @Test
    void getAllPatchNotes_성공() throws Exception {
        // given
        List<PatchNoteSummaryReadModel> patchNotes = List.of(
                new PatchNoteSummaryReadModel("26.S1.1", "패치 26.1 노트", "2026-01-15"),
                new PatchNoteSummaryReadModel("26.S1.2", "패치 26.2 노트", "2026-01-01")
        );

        given(patchNoteService.getAllPatchNotes()).willReturn(patchNotes);

        // when & then
        mockMvc.perform(
                        get("/api/v1/patch-notes")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("patch-note-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                        .description("패치노트 요약 목록"),
                                fieldWithPath("data[].versionId").type(JsonFieldType.STRING)
                                        .description("패치노트 버전 ID"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING)
                                        .description("패치노트 제목"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING)
                                        .description("패치노트 생성 일시")
                        )
                ));
    }

    @DisplayName("특정 버전 패치노트 상세 조회 API")
    @Test
    void getPatchNoteByVersionId_성공() throws Exception {
        // given
        String versionId = "26.S1.1";
        PatchNoteReadModel patchNote = createSamplePatchNote(versionId);

        given(patchNoteService.getPatchNoteByVersionId(versionId)).willReturn(patchNote);

        // when & then
        mockMvc.perform(
                        get("/api/v1/patch-notes/{versionId}", versionId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("patch-note-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("versionId").description("조회할 패치노트 버전 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.versionId").type(JsonFieldType.STRING)
                                        .description("패치노트 버전 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                        .description("패치노트 제목"),
                                fieldWithPath("data.content").type(JsonFieldType.OBJECT)
                                        .description("패치노트 내용"),
                                fieldWithPath("data.patchUrl").type(JsonFieldType.STRING)
                                        .description("패치노트 원본 URL"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                        .description("패치노트 생성 일시"),
                                fieldWithPath("data.content.version").type(JsonFieldType.STRING)
                                        .description("게임 버전"),
                                // Rift (소환사의 협곡)
                                fieldWithPath("data.content.rift").type(JsonFieldType.OBJECT)
                                        .description("소환사의 협곡 변경사항"),
                                fieldWithPath("data.content.rift.champions[]").type(JsonFieldType.ARRAY)
                                        .description("챔피언 변경 목록"),
                                fieldWithPath("data.content.rift.champions[].targetName").type(JsonFieldType.STRING)
                                        .description("대상 이름"),
                                fieldWithPath("data.content.rift.champions[].type").type(JsonFieldType.STRING)
                                        .description("변경 유형 (champion/item/system)"),
                                fieldWithPath("data.content.rift.champions[].direction").type(JsonFieldType.STRING)
                                        .description("변경 방향 (buff/nerf/adjust)"),
                                fieldWithPath("data.content.rift.champions[].changes[]").type(JsonFieldType.ARRAY)
                                        .description("상세 변경사항"),
                                fieldWithPath("data.content.rift.champions[].changes[].statName").type(JsonFieldType.STRING)
                                        .description("스탯 이름"),
                                fieldWithPath("data.content.rift.champions[].changes[].before").type(JsonFieldType.STRING)
                                        .description("변경 전 값"),
                                fieldWithPath("data.content.rift.champions[].changes[].after").type(JsonFieldType.STRING)
                                        .description("변경 후 값"),
                                fieldWithPath("data.content.rift.items[]").type(JsonFieldType.ARRAY)
                                        .description("아이템 변경 목록"),
                                fieldWithPath("data.content.rift.items[].targetName").type(JsonFieldType.STRING)
                                        .description("대상 이름"),
                                fieldWithPath("data.content.rift.items[].type").type(JsonFieldType.STRING)
                                        .description("변경 유형"),
                                fieldWithPath("data.content.rift.items[].direction").type(JsonFieldType.STRING)
                                        .description("변경 방향"),
                                fieldWithPath("data.content.rift.items[].changes[]").type(JsonFieldType.ARRAY)
                                        .description("상세 변경사항"),
                                fieldWithPath("data.content.rift.items[].changes[].statName").type(JsonFieldType.STRING)
                                        .description("스탯 이름"),
                                fieldWithPath("data.content.rift.items[].changes[].before").type(JsonFieldType.STRING)
                                        .description("변경 전 값"),
                                fieldWithPath("data.content.rift.items[].changes[].after").type(JsonFieldType.STRING)
                                        .description("변경 후 값"),
                                fieldWithPath("data.content.rift.systems[]").type(JsonFieldType.ARRAY)
                                        .description("시스템 변경 목록"),
                                fieldWithPath("data.content.rift.systems[].targetName").type(JsonFieldType.STRING)
                                        .description("대상 이름"),
                                fieldWithPath("data.content.rift.systems[].type").type(JsonFieldType.STRING)
                                        .description("변경 유형"),
                                fieldWithPath("data.content.rift.systems[].direction").type(JsonFieldType.STRING)
                                        .description("변경 방향"),
                                fieldWithPath("data.content.rift.systems[].changes[]").type(JsonFieldType.ARRAY)
                                        .description("상세 변경사항"),
                                fieldWithPath("data.content.rift.systems[].changes[].statName").type(JsonFieldType.STRING)
                                        .description("스탯 이름"),
                                fieldWithPath("data.content.rift.systems[].changes[].before").type(JsonFieldType.STRING)
                                        .description("변경 전 값"),
                                fieldWithPath("data.content.rift.systems[].changes[].after").type(JsonFieldType.STRING)
                                        .description("변경 후 값"),
                                // Arena (아레나)
                                fieldWithPath("data.content.arena").type(JsonFieldType.OBJECT)
                                        .description("아레나 변경사항"),
                                fieldWithPath("data.content.arena.champions[]").type(JsonFieldType.ARRAY)
                                        .description("챔피언 변경 목록"),
                                fieldWithPath("data.content.arena.champions[].targetName").type(JsonFieldType.STRING)
                                        .description("대상 이름"),
                                fieldWithPath("data.content.arena.champions[].type").type(JsonFieldType.STRING)
                                        .description("변경 유형"),
                                fieldWithPath("data.content.arena.champions[].direction").type(JsonFieldType.STRING)
                                        .description("변경 방향"),
                                fieldWithPath("data.content.arena.champions[].changes[]").type(JsonFieldType.ARRAY)
                                        .description("상세 변경사항"),
                                fieldWithPath("data.content.arena.champions[].changes[].statName").type(JsonFieldType.STRING)
                                        .description("스탯 이름"),
                                fieldWithPath("data.content.arena.champions[].changes[].before").type(JsonFieldType.STRING)
                                        .description("변경 전 값"),
                                fieldWithPath("data.content.arena.champions[].changes[].after").type(JsonFieldType.STRING)
                                        .description("변경 후 값"),
                                fieldWithPath("data.content.arena.items[]").type(JsonFieldType.ARRAY)
                                        .description("아이템 변경 목록"),
                                fieldWithPath("data.content.arena.items[].targetName").type(JsonFieldType.STRING)
                                        .description("대상 이름"),
                                fieldWithPath("data.content.arena.items[].type").type(JsonFieldType.STRING)
                                        .description("변경 유형"),
                                fieldWithPath("data.content.arena.items[].direction").type(JsonFieldType.STRING)
                                        .description("변경 방향"),
                                fieldWithPath("data.content.arena.items[].changes[]").type(JsonFieldType.ARRAY)
                                        .description("상세 변경사항"),
                                fieldWithPath("data.content.arena.items[].changes[].statName").type(JsonFieldType.STRING)
                                        .description("스탯 이름"),
                                fieldWithPath("data.content.arena.items[].changes[].before").type(JsonFieldType.STRING)
                                        .description("변경 전 값"),
                                fieldWithPath("data.content.arena.items[].changes[].after").type(JsonFieldType.STRING)
                                        .description("변경 후 값"),
                                fieldWithPath("data.content.arena.systems[]").type(JsonFieldType.ARRAY)
                                        .description("시스템 변경 목록"),
                                fieldWithPath("data.content.arena.systems[].targetName").type(JsonFieldType.STRING)
                                        .description("대상 이름"),
                                fieldWithPath("data.content.arena.systems[].type").type(JsonFieldType.STRING)
                                        .description("변경 유형"),
                                fieldWithPath("data.content.arena.systems[].direction").type(JsonFieldType.STRING)
                                        .description("변경 방향"),
                                fieldWithPath("data.content.arena.systems[].changes[]").type(JsonFieldType.ARRAY)
                                        .description("상세 변경사항"),
                                fieldWithPath("data.content.arena.systems[].changes[].statName").type(JsonFieldType.STRING)
                                        .description("스탯 이름"),
                                fieldWithPath("data.content.arena.systems[].changes[].before").type(JsonFieldType.STRING)
                                        .description("변경 전 값"),
                                fieldWithPath("data.content.arena.systems[].changes[].after").type(JsonFieldType.STRING)
                                        .description("변경 후 값"),
                                // ARAM (칼바람 나락)
                                fieldWithPath("data.content.aram").type(JsonFieldType.OBJECT)
                                        .description("칼바람 나락 변경사항"),
                                fieldWithPath("data.content.aram.champions[]").type(JsonFieldType.ARRAY)
                                        .description("챔피언 변경 목록"),
                                fieldWithPath("data.content.aram.champions[].targetName").type(JsonFieldType.STRING)
                                        .description("대상 이름"),
                                fieldWithPath("data.content.aram.champions[].type").type(JsonFieldType.STRING)
                                        .description("변경 유형"),
                                fieldWithPath("data.content.aram.champions[].direction").type(JsonFieldType.STRING)
                                        .description("변경 방향"),
                                fieldWithPath("data.content.aram.champions[].changes[]").type(JsonFieldType.ARRAY)
                                        .description("상세 변경사항"),
                                fieldWithPath("data.content.aram.champions[].changes[].statName").type(JsonFieldType.STRING)
                                        .description("스탯 이름"),
                                fieldWithPath("data.content.aram.champions[].changes[].before").type(JsonFieldType.STRING)
                                        .description("변경 전 값"),
                                fieldWithPath("data.content.aram.champions[].changes[].after").type(JsonFieldType.STRING)
                                        .description("변경 후 값"),
                                fieldWithPath("data.content.aram.augments[]").type(JsonFieldType.ARRAY)
                                        .description("증강 변경 목록"),
                                fieldWithPath("data.content.aram.augments[].targetName").type(JsonFieldType.STRING)
                                        .description("대상 이름"),
                                fieldWithPath("data.content.aram.augments[].type").type(JsonFieldType.STRING)
                                        .description("변경 유형"),
                                fieldWithPath("data.content.aram.augments[].direction").type(JsonFieldType.STRING)
                                        .description("변경 방향"),
                                fieldWithPath("data.content.aram.augments[].changes[]").type(JsonFieldType.ARRAY)
                                        .description("상세 변경사항"),
                                fieldWithPath("data.content.aram.augments[].changes[].statName").type(JsonFieldType.STRING)
                                        .description("스탯 이름"),
                                fieldWithPath("data.content.aram.augments[].changes[].before").type(JsonFieldType.STRING)
                                        .description("변경 전 값"),
                                fieldWithPath("data.content.aram.augments[].changes[].after").type(JsonFieldType.STRING)
                                        .description("변경 후 값"),
                                fieldWithPath("data.content.aram.items[]").type(JsonFieldType.ARRAY)
                                        .description("아이템 변경 목록"),
                                fieldWithPath("data.content.aram.items[].targetName").type(JsonFieldType.STRING)
                                        .description("대상 이름"),
                                fieldWithPath("data.content.aram.items[].type").type(JsonFieldType.STRING)
                                        .description("변경 유형"),
                                fieldWithPath("data.content.aram.items[].direction").type(JsonFieldType.STRING)
                                        .description("변경 방향"),
                                fieldWithPath("data.content.aram.items[].changes[]").type(JsonFieldType.ARRAY)
                                        .description("상세 변경사항"),
                                fieldWithPath("data.content.aram.items[].changes[].statName").type(JsonFieldType.STRING)
                                        .description("스탯 이름"),
                                fieldWithPath("data.content.aram.items[].changes[].before").type(JsonFieldType.STRING)
                                        .description("변경 전 값"),
                                fieldWithPath("data.content.aram.items[].changes[].after").type(JsonFieldType.STRING)
                                        .description("변경 후 값")
                        )
                ));
    }

    private PatchNoteReadModel createSamplePatchNote(String versionId) {
        // Rift 변경사항 (챔피언, 아이템, 시스템)
        List<ChangeEntry> riftChampions = List.of(
                new ChangeEntry("카시오페아", "champion", "buff",
                        List.of(new StatChange("기본 이동 속도", "328", "335")))
        );
        List<ChangeEntry> riftItems = List.of(
                new ChangeEntry("무한의 대검", "item", "nerf",
                        List.of(new StatChange("공격력", "70", "65")))
        );
        List<ChangeEntry> riftSystems = List.of(
                new ChangeEntry("바론 버프", "system", "adjust",
                        List.of(new StatChange("공격력 증가", "40", "35")))
        );
        GameModeChanges rift = new GameModeChanges(riftChampions, riftItems, riftSystems);

        // Arena 변경사항
        List<ChangeEntry> arenaChampions = List.of(
                new ChangeEntry("야스오", "champion", "nerf",
                        List.of(new StatChange("받는 피해", "-5%", "0%")))
        );
        List<ChangeEntry> arenaItems = List.of(
                new ChangeEntry("죽음의 무도", "item", "buff",
                        List.of(new StatChange("체력", "300", "350")))
        );
        List<ChangeEntry> arenaSystems = List.of(
                new ChangeEntry("아레나 전투 시간", "system", "adjust",
                        List.of(new StatChange("최대 시간", "30초", "25초")))
        );
        GameModeChanges arena = new GameModeChanges(arenaChampions, arenaItems, arenaSystems);

        // ARAM 변경사항
        List<ChangeEntry> aramChampions = List.of(
                new ChangeEntry("럭스", "champion", "nerf",
                        List.of(new StatChange("주는 피해", "+5%", "0%")))
        );
        List<ChangeEntry> aramAugments = List.of(
                new ChangeEntry("속사", "augment", "buff",
                        List.of(new StatChange("공격 속도", "20%", "25%")))
        );
        List<ChangeEntry> aramItems = List.of(
                new ChangeEntry("눈덩이", "item", "adjust",
                        List.of(new StatChange("재사용 대기시간", "60초", "50초")))
        );
        AramChanges aram = new AramChanges(aramChampions, aramAugments, aramItems);

        PatchNoteContent content = new PatchNoteContent("26.1", rift, arena, aram);

        return new PatchNoteReadModel(
                versionId,
                "패치 26.1 노트",
                content,
                "https://www.leagueoflegends.com/ko-kr/news/game-updates/patch-26-1-notes/",
                "2026-01-15"
        );
    }
}
