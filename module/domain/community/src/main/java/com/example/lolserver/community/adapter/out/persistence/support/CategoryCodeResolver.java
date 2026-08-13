package com.example.lolserver.community.adapter.out.persistence.support;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.adapter.out.persistence.entity.CommunityCategoryEntity;
import com.example.lolserver.community.adapter.out.persistence.repository.CommunityCategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 게시판 코드({@code GENERAL})와 대리키({@code community_category.id}) 사이를 옮긴다.
 *
 * <p>DB 는 category_id 로 참조하지만 도메인·API 는 계속 code 를 쓴다(V33). 이 변환을
 * 영속성 어댑터 안에 가두는 것이 이 클래스의 존재 이유다 — 대리키는 저장 방식의 산물이고,
 * 게시글의 카테고리를 가리키는 업무상 이름은 여전히 code 다. 위 계층으로 id 가 새어나가면
 * 응답 DTO 와 프론트까지 대리키를 알아야 한다.
 *
 * <p>캐시를 두지 않는 이유는 {@code CategoryService} 와 같다 — 8행짜리 테이블이라
 * 캐시 이득이 측정되지 않고, 대신 무효화 책임이 붙는다. 대신 목록 변환은 행마다 조회하지
 * 않도록 {@link #codesByIds()} 로 한 번에 받아 메모리에서 맞춘다.
 */
@Component
@RequiredArgsConstructor
public class CategoryCodeResolver {

    private final CommunityCategoryJpaRepository categoryJpaRepository;

    /**
     * 저장용 code -> id. 존재하지 않으면 던진다.
     *
     * <p>쓰기 경로에서는 이미 {@code CategoryQueryUseCase.validateWritable} 이 앞서 돌지만,
     * 여기서 한 번 더 막는다. 그 검증을 거치지 않는 호출자가 생기면 FK 위반이
     * {@code DataIntegrityViolationException} 500 으로 나가기 때문이다.
     */
    public Long toId(String code) {
        return findId(code)
                .orElseThrow(() -> new CoreException(ErrorType.INVALID_CATEGORY));
    }

    /**
     * 필터용 code -> id. 없으면 비어 있는 값을 돌려준다.
     *
     * <p>{@link #toId} 와 달리 던지지 않는다. 목록 필터에 없는 코드가 들어오는 것은
     * 잘못된 요청이라기보다 "결과 0건" 이고, code 시절 동작도 그랬다.
     */
    public Optional<Long> findId(String code) {
        return categoryJpaRepository.findByCode(code)
                .map(CommunityCategoryEntity::getId);
    }

    /** 단건 id -> code. 없는 id 면 null 이다(FK 가 있으므로 정상 경로에서는 나오지 않는다). */
    public String toCode(Long id) {
        if (id == null) {
            return null;
        }
        return categoryJpaRepository.findById(id)
                .map(CommunityCategoryEntity::getCode)
                .orElse(null);
    }

    /** 목록 변환용 id -> code 전체 맵. 페이지당 한 번만 조회한다. */
    public Map<Long, String> codesByIds() {
        return categoryJpaRepository.findAll().stream()
                .collect(Collectors.toMap(
                        CommunityCategoryEntity::getId,
                        CommunityCategoryEntity::getCode,
                        (first, second) -> first));
    }

    /** {@link #codesByIds()} 결과에서 code 를 꺼낸다. 조회 시점 이후 사라진 id 는 null 이다. */
    public static String codeOf(Map<Long, String> codes, Long categoryId) {
        return categoryId != null ? codes.get(categoryId) : null;
    }
}
