package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.common.web.security.AuthenticatedMember;
import com.example.lolserver.community.adapter.in.web.response.ImageResponse;
import com.example.lolserver.community.application.command.UploadImageCommand;
import com.example.lolserver.community.application.model.readmodel.PostImageReadModel;
import com.example.lolserver.community.application.port.in.ImageUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 서버 경유 멀티파트 업로드.
 *
 * <p>presigned PUT 대신 이 방식을 택한 이유는, 지금 규모에서 5MB 파일이 앱을 통과하는 비용보다
 * 악성 파일을 <b>버킷에 올리기 전에</b> 걷어낼 수 있다는 이점이 크기 때문이다. presigned 는
 * "확정 API 를 부르지 않고 버킷만 채우는" 남용 경로가 구조적으로 열려 있고, EXIF 제거 같은
 * 정규화를 하려면 결국 사후 워커를 따로 만들어야 한다.
 *
 * <p>인증은 {@code SecurityConfig} 의 {@code /api/community/**} authenticated 규칙이 이미
 * 덮는다 — GET 만 permitAll 이므로 별도 규칙 추가가 필요 없다.
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityImageController {

    private final ImageUseCase imageUseCase;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageResponse>> uploadImage(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestPart("file") MultipartFile file) {
        UploadImageCommand command = UploadImageCommand.builder()
                .content(readBytes(file))
                .declaredContentType(file.getContentType())
                .build();

        PostImageReadModel readModel = imageUseCase.upload(member.memberId(), command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ImageResponse.from(readModel)));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Long imageId) {
        imageUseCase.delete(member.memberId(), imageId);
        return ResponseEntity.noContent().build();
    }

    private byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CoreException(ErrorType.IMAGE_FILE_REQUIRED);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CoreException(ErrorType.IMAGE_INVALID);
        }
    }
}
