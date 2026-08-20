package com.example.lolserver.community.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.community.application.command.UploadImageCommand;
import com.example.lolserver.community.application.model.readmodel.PostImageReadModel;
import com.example.lolserver.community.application.port.out.ImagePersistencePort;
import com.example.lolserver.community.application.port.out.ImageProcessorPort;
import com.example.lolserver.community.application.port.out.ImageRateLimitPort;
import com.example.lolserver.community.application.port.out.ImageStoragePort;
import com.example.lolserver.community.application.port.out.ProcessedImage;
import com.example.lolserver.community.application.port.out.StoredImageLocation;
import com.example.lolserver.community.config.CommunityImageProperties;
import com.example.lolserver.community.domain.PostImage;
import com.example.lolserver.community.domain.vo.ImageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final String STORAGE_KEY = "local/community/2026/08/uuid.jpg";
    private static final String URL = "https://cdn.example.com/" + STORAGE_KEY;

    @Mock
    private ImagePersistencePort imagePersistencePort;

    @Mock
    private ImageStoragePort imageStoragePort;

    @Mock
    private ImageProcessorPort imageProcessorPort;

    @Mock
    private ImageRateLimitPort imageRateLimitPort;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        // 정책값은 기본값 그대로 쓴다 — 이 테스트가 검증하는 건 순서와 분기이지 숫자가 아니다.
        imageService = new ImageService(imagePersistencePort, imageStoragePort,
                imageProcessorPort, imageRateLimitPort, new CommunityImageProperties());
    }

    @Nested
    @DisplayName("업로드")
    class Upload {

        @DisplayName("DB INSERT 가 S3 PUT 보다 먼저 일어난다 — 그래야 DB 가 S3 의 상위집합이 된다")
        @Test
        void upload_insertsBeforePut() {
            givenUploadSucceeds();

            imageService.upload(MEMBER_ID, command());

            InOrder order = inOrder(imagePersistencePort, imageStoragePort);
            order.verify(imagePersistencePort).save(any(PostImage.class));
            order.verify(imageStoragePort).store(anyString(), any(), anyString());
        }

        @DisplayName("PUT 이 끝나면 PENDING 으로 올려 저장한다")
        @Test
        void upload_marksPendingAfterPut() {
            givenUploadSucceeds();

            PostImageReadModel result = imageService.upload(MEMBER_ID, command());

            assertThat(result.getUrl()).isEqualTo(URL);
            then(imagePersistencePort).should(org.mockito.Mockito.times(2))
                    .save(any(PostImage.class));
        }

        @DisplayName("S3 PUT 이 실패하면 행은 UPLOADING 인 채로 남는다 — 정리 배치가 잡는다")
        @Test
        void upload_putFails_leavesRowUploading() {
            given(imageRateLimitPort.tryAcquire(anyLong(), anyInt())).willReturn(true);
            given(imageProcessorPort.process(any())).willReturn(processed());
            given(imageStoragePort.allocate(anyString()))
                    .willReturn(new StoredImageLocation(STORAGE_KEY, URL));
            given(imagePersistencePort.save(any(PostImage.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            org.mockito.BDDMockito.willThrow(new CoreException(ErrorType.IMAGE_STORAGE_FAILED))
                    .given(imageStoragePort).store(anyString(), any(), anyString());

            assertThatThrownBy(() -> imageService.upload(MEMBER_ID, command()))
                    .isInstanceOf(CoreException.class);

            // PENDING 으로 올리는 두 번째 save 는 일어나지 않는다.
            then(imagePersistencePort).should(org.mockito.Mockito.times(1))
                    .save(any(PostImage.class));
        }

        @DisplayName("분당 한도를 넘으면 디코드조차 하지 않고 거절한다")
        @Test
        void upload_rateLimited_rejectsBeforeProcessing() {
            given(imageRateLimitPort.tryAcquire(anyLong(), anyInt())).willReturn(false);

            assertThatThrownBy(() -> imageService.upload(MEMBER_ID, command()))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_UPLOAD_RATE_LIMITED);

            then(imageProcessorPort).should(never()).process(any());
        }

        @DisplayName("빈 파일은 IMAGE_FILE_REQUIRED 로 거절한다")
        @Test
        void upload_emptyContent_throws() {
            given(imageRateLimitPort.tryAcquire(anyLong(), anyInt())).willReturn(true);

            UploadImageCommand empty = UploadImageCommand.builder()
                    .content(new byte[0])
                    .build();

            assertThatThrownBy(() -> imageService.upload(MEMBER_ID, empty))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_FILE_REQUIRED);
        }

        @DisplayName("용량 상한을 넘으면 IMAGE_SIZE_EXCEEDED 로 거절한다")
        @Test
        void upload_tooLarge_throws() {
            given(imageRateLimitPort.tryAcquire(anyLong(), anyInt())).willReturn(true);

            // command() 의 본문은 4바이트다.
            CommunityImageProperties tight = new CommunityImageProperties();
            tight.setMaxSizeBytes(2L);
            ImageService service = new ImageService(imagePersistencePort, imageStoragePort,
                    imageProcessorPort, imageRateLimitPort, tight);

            assertThatThrownBy(() -> service.upload(MEMBER_ID, command()))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_SIZE_EXCEEDED);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @DisplayName("스토리지를 먼저 지우고 row 를 지운다 — 반대면 삭제 실패 시 파일이 추적 불가능해진다")
        @Test
        void delete_removesStorageBeforeRow() {
            given(imagePersistencePort.findById(5L)).willReturn(Optional.of(pending()));

            imageService.delete(MEMBER_ID, 5L);

            InOrder order = inOrder(imageStoragePort, imagePersistencePort);
            order.verify(imageStoragePort).delete(STORAGE_KEY);
            order.verify(imagePersistencePort).deleteAllByIds(List.of(5L));
        }

        @DisplayName("존재하지 않는 이미지는 404 다")
        @Test
        void delete_notFound_throws() {
            given(imagePersistencePort.findById(5L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> imageService.delete(MEMBER_ID, 5L))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_NOT_FOUND);
        }

        @DisplayName("남의 이미지를 지우려 하면 스토리지를 건드리지 않는다")
        @Test
        void delete_otherMember_doesNotTouchStorage() {
            given(imagePersistencePort.findById(5L)).willReturn(Optional.of(pending()));

            assertThatThrownBy(() -> imageService.delete(OTHER_ID, 5L))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("errorType", ErrorType.FORBIDDEN);

            then(imageStoragePort).should(never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("첨부 확정")
    class Attach {

        @DisplayName("요청한 id 중 하나라도 없으면 전부 실패시킨다")
        @Test
        void attach_missingId_throws() {
            given(imagePersistencePort.findAllByIds(List.of(1L, 2L)))
                    .willReturn(List.of(pending()));

            assertThatThrownBy(() -> imageService.attach(MEMBER_ID, POST_ID, List.of(1L, 2L)))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_NOT_FOUND);
        }

        @DisplayName("빈 목록이면 조회조차 하지 않는다")
        @Test
        void attach_empty_doesNothing() {
            imageService.attach(MEMBER_ID, POST_ID, List.of());

            then(imagePersistencePort).should(never()).findAllByIds(anyList());
        }

        @DisplayName("글당 상한을 넘으면 IMAGE_COUNT_EXCEEDED 다")
        @Test
        void attach_tooMany_throws() {
            CommunityImageProperties tight = new CommunityImageProperties();
            tight.setMaxCountPerPost(1);
            ImageService service = new ImageService(imagePersistencePort, imageStoragePort,
                    imageProcessorPort, imageRateLimitPort, tight);

            assertThatThrownBy(() -> service.attach(MEMBER_ID, POST_ID, List.of(1L, 2L)))
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("errorType", ErrorType.IMAGE_COUNT_EXCEEDED);
        }

        @DisplayName("수정은 전체 교체다 — 빠진 것은 DETACHED, 새로 온 것은 ATTACHED")
        @Test
        void replace_appliesSymmetricDifference() {
            PostImage kept = attached(1L);
            PostImage removed = attached(2L);
            PostImage added = pending();
            given(imagePersistencePort.findByPostId(POST_ID))
                    .willReturn(List.of(kept, removed));
            given(imagePersistencePort.findAllByIds(List.of(3L))).willReturn(List.of(added));

            imageService.replace(MEMBER_ID, POST_ID, List.of(1L, 3L));

            assertThat(removed.getStatus()).isEqualTo(ImageStatus.DETACHED);
            assertThat(kept.getStatus()).isEqualTo(ImageStatus.ATTACHED);
            assertThat(added.getStatus()).isEqualTo(ImageStatus.ATTACHED);
        }

        @DisplayName("글이 지워지면 붙어 있던 이미지는 DETACHED 로만 바뀐다 — 파일은 남는다")
        @Test
        void detachByPostId_keepsFiles() {
            PostImage image = attached(1L);
            given(imagePersistencePort.findByPostId(POST_ID)).willReturn(List.of(image));

            imageService.detachByPostId(POST_ID);

            assertThat(image.getStatus()).isEqualTo(ImageStatus.DETACHED);
            then(imageStoragePort).should(never()).delete(anyString());
        }

        @DisplayName("첨부 목록 조회는 DETACHED 를 걸러낸다 — post_id 가 이력으로 남아 있기 때문")
        @Test
        void getPostImages_excludesDetached() {
            PostImage detached = attached(1L);
            detached.detach();
            given(imagePersistencePort.findByPostId(POST_ID))
                    .willReturn(List.of(detached, attached(2L)));

            assertThat(imageService.getPostImages(POST_ID)).hasSize(1);
        }
    }

    private void givenUploadSucceeds() {
        given(imageRateLimitPort.tryAcquire(anyLong(), anyInt())).willReturn(true);
        given(imageProcessorPort.process(any())).willReturn(processed());
        given(imageStoragePort.allocate(anyString()))
                .willReturn(new StoredImageLocation(STORAGE_KEY, URL));
        given(imagePersistencePort.save(any(PostImage.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private static UploadImageCommand command() {
        return UploadImageCommand.builder()
                .content(new byte[] {1, 2, 3, 4})
                .declaredContentType("image/png")
                .build();
    }

    private static ProcessedImage processed() {
        return new ProcessedImage(new byte[] {1, 2, 3, 4}, "image/jpeg", "jpg", 800, 600);
    }

    private static PostImage pending() {
        PostImage image = PostImage.uploading(MEMBER_ID, STORAGE_KEY, URL,
                "image/jpeg", 4L, 800, 600);
        image.markUploaded();
        return image;
    }

    private static PostImage attached(Long id) {
        PostImage image = PostImage.builder()
                .id(id)
                .memberId(MEMBER_ID)
                .postId(POST_ID)
                .storageKey(STORAGE_KEY)
                .url(URL)
                .contentType("image/jpeg")
                .sizeBytes(4L)
                .status(ImageStatus.ATTACHED)
                .build();
        return image;
    }
}
