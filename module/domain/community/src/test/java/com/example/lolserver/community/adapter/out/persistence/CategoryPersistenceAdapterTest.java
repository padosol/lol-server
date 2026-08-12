package com.example.lolserver.community.adapter.out.persistence;

import com.example.lolserver.common.test.RepositoryTestBase;
import com.example.lolserver.community.adapter.out.persistence.adapter.CategoryPersistenceAdapter;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBoardGroupEntity;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityBoardGroupI18nEntity;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityCategoryEntity;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityCategoryI18nEntity;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityBoardGroupI18nJpaRepository;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityBoardGroupJpaRepository;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityCategoryI18nJpaRepository;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityCategoryJpaRepository;
import com.example.lolserver.community.domain.BoardGroup;
import com.example.lolserver.community.domain.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 정렬과 그룹핑은 쿼리 메서드 이름과 메모리 조립에 걸쳐 있어 단위 테스트로 덮이지
 * 않는다. 특히 파생 쿼리 이름(findAllByActiveTrueOrderByDisplayOrderAscCodeAsc)은
 * 컴파일이 아니라 리포지토리 부트스트랩 시점에 파싱되므로, 이 테스트가 없으면
 * 이름을 잘못 써도 CI 는 green 이고 운영 기동에서 처음 터진다.
 */
class CategoryPersistenceAdapterTest extends RepositoryTestBase {

    @Autowired
    private CategoryPersistenceAdapter categoryPersistenceAdapter;

    @Autowired
    private CommunityBoardGroupJpaRepository groupRepository;

    @Autowired
    private CommunityCategoryJpaRepository categoryRepository;

    @Autowired
    private CommunityBoardGroupI18nJpaRepository groupI18nRepository;

    @Autowired
    private CommunityCategoryI18nJpaRepository categoryI18nRepository;

    @DisplayName("그룹은 display_order 순으로, 각 그룹의 카테고리도 그 안에서 순서대로 묶인다")
    @Test
    void findAllGroupsWithCategories_groupedAndOrdered() {
        // given — 저장 순서를 의도적으로 뒤섞는다
        Long info = saveGroup("INFO", 30);
        Long community = saveGroup("COMMUNITY", 10);
        Long esports = saveGroup("ESPORTS", 20);

        saveCategory("USER_NEWS", info, 20);
        saveCategory("HUMOR", community, 20);
        saveCategory("LCK", esports, 10);
        saveCategory("GENERAL", community, 10);
        saveCategory("TIPS_AND_GUIDES", info, 10);

        // when
        List<BoardGroup> groups = categoryPersistenceAdapter.findAllGroupsWithCategories();

        // then
        assertThat(groups).extracting(BoardGroup::getCode)
                .containsExactly("COMMUNITY", "ESPORTS", "INFO");
        assertThat(groups.get(0).getCategories()).extracting(Category::getCode)
                .containsExactly("GENERAL", "HUMOR");
        assertThat(groups.get(1).getCategories()).extracting(Category::getCode)
                .containsExactly("LCK");
        assertThat(groups.get(2).getCategories()).extracting(Category::getCode)
                .containsExactly("TIPS_AND_GUIDES", "USER_NEWS");
    }

    @DisplayName("display_order 가 같으면 code 오름차순으로 순서가 결정된다")
    @Test
    void findAllGroupsWithCategories_tieBreaksByCode() {
        // given — display_order 에 UNIQUE 를 걸지 않으므로 동점이 생길 수 있고,
        // 2차 키가 없으면 반환 순서가 비결정적이 되어 배포마다 사이드바가 뒤바뀐다.
        Long zulu = saveGroup("ZULU", 10);
        Long alpha = saveGroup("ALPHA", 10);

        saveCategory("B_CATEGORY", alpha, 10);
        saveCategory("A_CATEGORY", alpha, 10);

        // when
        List<BoardGroup> groups = categoryPersistenceAdapter.findAllGroupsWithCategories();

        // then
        assertThat(groups).extracting(BoardGroup::getCode)
                .containsExactly("ALPHA", "ZULU");
        assertThat(groups.get(0).getCategories()).extracting(Category::getCode)
                .containsExactly("A_CATEGORY", "B_CATEGORY");
        assertThat(zulu).isNotNull();
    }

    @DisplayName("라벨은 로케일별로 실려 오고 카테고리 설명도 함께 따라온다")
    @Test
    void findAllGroupsWithCategories_carriesAllLocaleLabels() {
        // given
        Long community = saveGroup("COMMUNITY", 10);
        Long generalId = saveCategory("GENERAL", community, 10);

        saveGroupLabel(community, "ko", "커뮤니티");
        saveGroupLabel(community, "en", "Community");
        saveCategoryLabel(generalId, "ko", "자유", "자유롭게 이야기하는 공간");
        saveCategoryLabel(generalId, "en", "General", null);

        // when
        List<BoardGroup> groups = categoryPersistenceAdapter.findAllGroupsWithCategories();

        // then — 어댑터는 로케일을 고르지 않는다. 폴백은 도메인의 판단이다.
        BoardGroup group = groups.get(0);
        assertThat(group.getLabels()).containsEntry("ko", "커뮤니티")
                .containsEntry("en", "Community");
        assertThat(group.resolveName("en")).isEqualTo("Community");

        Category general = group.getCategories().get(0);
        assertThat(general.resolveName("ko")).isEqualTo("자유");
        assertThat(general.resolveDescription("ko")).isEqualTo("자유롭게 이야기하는 공간");
        assertThat(general.resolveDescription("en")).isNull();
        assertThat(general.getGroupCode()).isEqualTo("COMMUNITY");
    }

    @DisplayName("숨겨진 카테고리는 응답에 남지만 비활성 그룹은 통째로 빠진다")
    @Test
    void findAllGroupsWithCategories_excludesInactiveGroupOnly() {
        // given
        Long community = saveGroup("COMMUNITY", 10);
        Long retiredGroup = saveInactiveGroup("LEGACY", 40);

        saveCategory("GENERAL", community, 10);
        saveInactiveCategory("PATCH_NOTES", community, 20);
        saveCategory("OLD_BOARD", retiredGroup, 10);

        // when
        List<BoardGroup> groups = categoryPersistenceAdapter.findAllGroupsWithCategories();

        // then — 숨긴 카테고리는 기존 글의 배지 라벨을 해석하는 데 필요해 남긴다.
        // 그룹이 통째로 빠지면 그 안의 카테고리는 표현할 자리가 없어 함께 빠진다.
        assertThat(groups).extracting(BoardGroup::getCode).containsExactly("COMMUNITY");
        assertThat(groups.get(0).getCategories()).extracting(Category::getCode)
                .containsExactly("GENERAL", "PATCH_NOTES");
        assertThat(groups.get(0).getCategories().get(1).isActive()).isFalse();
    }

    @DisplayName("그룹에 카테고리가 하나도 없으면 빈 리스트로 내려간다")
    @Test
    void findAllGroupsWithCategories_emptyGroup() {
        // given
        saveGroup("ESPORTS", 20);

        // when
        List<BoardGroup> groups = categoryPersistenceAdapter.findAllGroupsWithCategories();

        // then — 프론트가 이때 "준비 중" 을 그린다
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).getCategories()).isEmpty();
    }

    @DisplayName("코드로 카테고리를 찾으면 쓰기 가능 여부를 판단할 수 있다")
    @Test
    void findCategoryByCode() {
        // given
        Long community = saveGroup("COMMUNITY", 10);
        saveCategory("GENERAL", community, 10);
        saveReadOnlyCategory("NOTICE", community, 5);

        // when
        Optional<Category> general = categoryPersistenceAdapter.findCategoryByCode("GENERAL");
        Optional<Category> notice = categoryPersistenceAdapter.findCategoryByCode("NOTICE");
        Optional<Category> ghost = categoryPersistenceAdapter.findCategoryByCode("GHOST");

        // then
        assertThat(general).isPresent();
        assertThat(general.get().isWritable()).isTrue();
        assertThat(notice).isPresent();
        assertThat(notice.get().isWritable()).isFalse();
        assertThat(ghost).isEmpty();
    }

    private Long saveGroup(String code, int order) {
        return saveGroup(code, order, true);
    }

    private Long saveInactiveGroup(String code, int order) {
        return saveGroup(code, order, false);
    }

    private Long saveGroup(String code, int order, boolean active) {
        LocalDateTime now = LocalDateTime.now();
        return groupRepository.save(CommunityBoardGroupEntity.builder()
                .code(code).displayOrder(order).active(active)
                .createdAt(now).updatedAt(now)
                .build()).getId();
    }

    private Long saveCategory(String code, Long groupId, int order) {
        return saveCategory(code, groupId, order, true, true);
    }

    private Long saveInactiveCategory(String code, Long groupId, int order) {
        return saveCategory(code, groupId, order, false, true);
    }

    private Long saveReadOnlyCategory(String code, Long groupId, int order) {
        return saveCategory(code, groupId, order, true, false);
    }

    private Long saveCategory(
            String code, Long groupId, int order, boolean active, boolean writable) {
        LocalDateTime now = LocalDateTime.now();
        return categoryRepository.save(CommunityCategoryEntity.builder()
                .code(code).groupId(groupId).displayOrder(order)
                .active(active).writable(writable)
                .createdAt(now).updatedAt(now)
                .build()).getId();
    }

    private void saveGroupLabel(Long groupId, String locale, String name) {
        groupI18nRepository.save(CommunityBoardGroupI18nEntity.builder()
                .groupId(groupId).locale(locale).name(name)
                .build());
    }

    private void saveCategoryLabel(
            Long categoryId, String locale, String name, String description) {
        categoryI18nRepository.save(CommunityCategoryI18nEntity.builder()
                .categoryId(categoryId).locale(locale).name(name).description(description)
                .build());
    }
}
