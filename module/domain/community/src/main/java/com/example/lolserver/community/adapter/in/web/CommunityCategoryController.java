package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.community.adapter.in.web.response.CategoryTreeResponse;
import com.example.lolserver.community.application.model.readmodel.CategoryTreeReadModel;
import com.example.lolserver.community.application.port.in.CategoryQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityCategoryController {

    private final CategoryQueryUseCase categoryQueryUseCase;

    /**
     * 인증 불필요(공개). 미지원 로케일은 서버가 ko 로 폴백하므로 400 을 던지지 않는다.
     *
     * <p>서버 캐시가 없는 대신 응답 캐시 헤더를 둔다. 프론트의 React Query staleTime
     * 30분과 함께 실제 DB 히트를 줄이는 방어선이다.
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryTreeResponse>> getCategories(
            @RequestParam(name = "locale", required = false, defaultValue = "ko") String locale) {

        CategoryTreeReadModel tree = categoryQueryUseCase.getCategoryTree(locale);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(ApiResponse.success(CategoryTreeResponse.from(tree)));
    }
}
