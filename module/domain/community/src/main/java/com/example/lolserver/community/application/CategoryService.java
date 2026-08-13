package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.model.readmodel.BoardGroupReadModel;
import com.example.lolserver.community.application.model.readmodel.CategoryReadModel;
import com.example.lolserver.community.application.model.readmodel.CategoryTreeReadModel;
import com.example.lolserver.community.application.port.in.CategoryQueryUseCase;
import com.example.lolserver.community.application.port.out.CategoryPersistencePort;
import com.example.lolserver.community.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService implements CategoryQueryUseCase {

    private final CategoryPersistencePort categoryPersistencePort;

    /**
     * 서버 캐시를 두지 않는다. 조회 대상이 그룹 3행 + 카테고리 8행 + 라벨 22행이라
     * 캐시 이득이 측정되지 않고, 대신 직렬화 함정과 무효화 책임이 따라붙는다.
     * DB 를 고치면 다음 요청부터 즉시 반영되는 편이 "배포 없이 게시판 추가" 라는
     * 목적에 부합한다. 도입한다면 이 포트 뒤에 캐시 어댑터를 끼우면 된다.
     */
    @Override
    public CategoryTreeReadModel getCategoryTree(String locale) {
        String resolved = locale != null && !locale.isBlank() ? locale : Category.DEFAULT_LOCALE;

        List<BoardGroupReadModel> groups = categoryPersistencePort.findAllGroupsWithCategories()
                .stream()
                .map(group -> new BoardGroupReadModel(
                        group.getCode(),
                        group.resolveName(resolved),
                        group.getDisplayOrder(),
                        toCategoryReadModels(group.getCategories(), resolved)))
                .toList();

        return new CategoryTreeReadModel(groups);
    }

    /**
     * 비활성 카테고리도 응답에 포함시킨다. 사이드바에서 숨기는 것과 기존 글의
     * 배지 라벨을 해석하는 것은 다른 일이고, 후자에는 숨겨진 카테고리도 필요하다.
     * 숨김 여부는 {@code visible} 로 내려보내 프론트가 판단한다.
     */
    private List<CategoryReadModel> toCategoryReadModels(List<Category> categories, String locale) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
                .map(category -> new CategoryReadModel(
                        category.getId(),
                        category.getCode(),
                        category.resolveName(locale),
                        category.resolveDescription(locale),
                        category.getDisplayOrder(),
                        category.isActive(),
                        category.isWritable(),
                        category.getIcon()))
                .toList();
    }

    /**
     * 숨김(active=false)도 작성 불가로 본다. writable 만 보면 폐지 예정이라
     * 사이드바에서 내린 게시판에 계속 글이 쌓인다.
     */
    @Override
    public void validateWritable(Long categoryId) {
        Category category = categoryPersistencePort.findCategoryById(categoryId)
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_CATEGORY));

        if (!category.isActive() || !category.isWritable()) {
            throw new CoreException(ErrorType.INVALID_CATEGORY);
        }
    }
}
