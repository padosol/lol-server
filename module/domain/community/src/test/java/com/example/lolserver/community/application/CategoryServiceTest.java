package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.model.readmodel.BoardGroupReadModel;
import com.example.lolserver.community.application.model.readmodel.CategoryReadModel;
import com.example.lolserver.community.application.model.readmodel.CategoryTreeReadModel;
import com.example.lolserver.community.application.port.out.CategoryPersistencePort;
import com.example.lolserver.community.domain.BoardGroup;
import com.example.lolserver.community.domain.Category;
import com.example.lolserver.community.domain.vo.CategoryLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryPersistencePort categoryPersistencePort;

    @InjectMocks
    private CategoryService categoryService;

    @DisplayName("그룹과 카테고리를 트리로 반환하며 순서는 어댑터가 준 그대로 유지된다")
    @Test
    void getCategoryTree_keepsOrder() {
        // given
        given(categoryPersistencePort.findAllGroupsWithCategories())
                .willReturn(List.of(
                        group("COMMUNITY", 10, Map.of("ko", "커뮤니티"), List.of(
                                category("GENERAL", "COMMUNITY", 10, Map.of("ko", label("자유"))),
                                category("HUMOR", "COMMUNITY", 20, Map.of("ko", label("유머"))))),
                        group("ESPORTS", 20, Map.of("ko", "e-스포츠"), List.of(
                                category("LCK", "ESPORTS", 10, Map.of("ko", label("LCK")))))));

        // when
        CategoryTreeReadModel tree = categoryService.getCategoryTree("ko");

        // then
        assertThat(tree.groups()).extracting(BoardGroupReadModel::code)
                .containsExactly("COMMUNITY", "ESPORTS");
        assertThat(tree.groups().get(0).name()).isEqualTo("커뮤니티");
        assertThat(tree.groups().get(0).categories())
                .extracting(CategoryReadModel::code)
                .containsExactly("GENERAL", "HUMOR");
        assertThat(tree.groups().get(1).categories()).hasSize(1);
    }

    @DisplayName("요청 로케일 라벨이 없으면 ko 로, ko 도 없으면 코드 원문으로 폴백한다")
    @Test
    void getCategoryTree_fallsBackToKoThenCode() {
        // given: LCK 는 en 라벨이 없고, USER_NEWS 는 라벨이 아예 없다
        given(categoryPersistencePort.findAllGroupsWithCategories())
                .willReturn(List.of(
                        group("ESPORTS", 20, Map.of("ko", "e-스포츠"), List.of(
                                category("LCK", "ESPORTS", 10, Map.of("ko", label("LCK 한글"))),
                                category("USER_NEWS", "ESPORTS", 20, Map.of())))));

        // when
        CategoryTreeReadModel tree = categoryService.getCategoryTree("en");

        // then
        BoardGroupReadModel group = tree.groups().get(0);
        assertThat(group.name()).isEqualTo("e-스포츠");
        assertThat(group.categories()).extracting(CategoryReadModel::name)
                .containsExactly("LCK 한글", "USER_NEWS");
    }

    @DisplayName("로케일이 비어 있으면 ko 로 조회한다")
    @Test
    void getCategoryTree_blankLocaleUsesKo() {
        // given
        given(categoryPersistencePort.findAllGroupsWithCategories())
                .willReturn(List.of(
                        group("COMMUNITY", 10, Map.of("ko", "커뮤니티", "en", "Community"), List.of())));

        // when
        CategoryTreeReadModel tree = categoryService.getCategoryTree("  ");

        // then
        assertThat(tree.groups().get(0).name()).isEqualTo("커뮤니티");
    }

    @DisplayName("비활성 카테고리도 응답에 포함되며 visible=false 로 내려간다")
    @Test
    void getCategoryTree_includesInactiveAsInvisible() {
        // given: 기존 글의 배지 라벨을 해석하려면 숨겨진 카테고리도 필요하다
        Category hidden = Category.builder()
                .code("PATCH_NOTES").groupCode("INFO").displayOrder(40)
                .active(false).writable(false)
                .labels(Map.of("ko", label("패치노트")))
                .build();
        given(categoryPersistencePort.findAllGroupsWithCategories())
                .willReturn(List.of(group("INFO", 30, Map.of("ko", "정보"), List.of(hidden))));

        // when
        CategoryTreeReadModel tree = categoryService.getCategoryTree("ko");

        // then
        CategoryReadModel category = tree.groups().get(0).categories().get(0);
        assertThat(category.name()).isEqualTo("패치노트");
        assertThat(category.visible()).isFalse();
        assertThat(category.writable()).isFalse();
    }

    @DisplayName("존재하지 않는 카테고리로 검증하면 예외가 발생한다")
    @Test
    void validateWritable_notFound() {
        // given
        given(categoryPersistencePort.findCategoryById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> categoryService.validateWritable(999L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_CATEGORY);
    }

    @DisplayName("읽기 전용 카테고리에는 글을 쓸 수 없다")
    @Test
    void validateWritable_readOnly() {
        // given
        Category notice = Category.builder()
                .id(7L)
                .code("NOTICE").displayOrder(5).active(true).writable(false)
                .labels(Map.of())
                .build();
        given(categoryPersistencePort.findCategoryById(7L))
                .willReturn(Optional.of(notice));

        // when & then
        assertThatThrownBy(() -> categoryService.validateWritable(7L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_CATEGORY);
    }

    @DisplayName("숨겨진 카테고리에는 글을 쓸 수 없다")
    @Test
    void validateWritable_inactive() {
        // given: writable 만 보면 사이드바에서 내린 게시판에 글이 계속 쌓인다
        Category retired = Category.builder()
                .id(8L)
                .code("PATCH_NOTES").displayOrder(40).active(false).writable(true)
                .labels(Map.of())
                .build();
        given(categoryPersistencePort.findCategoryById(8L))
                .willReturn(Optional.of(retired));

        // when & then
        assertThatThrownBy(() -> categoryService.validateWritable(8L))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.INVALID_CATEGORY);
    }

    @DisplayName("활성 상태의 쓰기 가능 카테고리는 검증을 통과한다")
    @Test
    void validateWritable_success() {
        // given
        Category general = category("GENERAL", "COMMUNITY", 10, Map.of());
        given(categoryPersistencePort.findCategoryById(general.getId()))
                .willReturn(Optional.of(general));

        // when & then
        categoryService.validateWritable(general.getId());
    }

    private BoardGroup group(String code, int order, Map<String, String> labels, List<Category> categories) {
        return BoardGroup.builder()
                .code(code).displayOrder(order).active(true)
                .labels(labels).categories(categories)
                .build();
    }

    /** id 는 code 해시로 만든다 — 값 자체에 의미는 없고 카테고리끼리 겹치지만 않으면 된다. */
    private Category category(String code, String groupCode, int order, Map<String, CategoryLabel> labels) {
        return Category.builder()
                .id((long) Math.abs(code.hashCode()))
                .code(code).groupCode(groupCode).displayOrder(order)
                .active(true).writable(true)
                .labels(labels)
                .build();
    }

    private CategoryLabel label(String name) {
        return CategoryLabel.of(name, null);
    }
}
