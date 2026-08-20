package com.example.lolserver.community.adapter.in.web.response;

import com.example.lolserver.community.application.model.readmodel.PostImageReadModel;

/**
 * {@code url} 은 CloudFront 를 통한 <b>영구 URL</b>이다. presigned URL 이 아니다 —
 * presigned GET 은 최대 7일(SigV4)이면 만료돼 본문에 박아 둔 옛 글의 이미지가 통째로 깨진다.
 */
public record ImageResponse(
        Long imageId,
        String url,
        Integer width,
        Integer height,
        long sizeBytes
) {
    public static ImageResponse from(PostImageReadModel readModel) {
        return new ImageResponse(
                readModel.getImageId(),
                readModel.getUrl(),
                readModel.getWidth(),
                readModel.getHeight(),
                readModel.getSizeBytes()
        );
    }
}
