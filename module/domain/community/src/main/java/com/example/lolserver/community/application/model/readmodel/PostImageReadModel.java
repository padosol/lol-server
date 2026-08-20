package com.example.lolserver.community.application.model.readmodel;

import com.example.lolserver.community.domain.PostImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PostImageReadModel {

    private final Long imageId;
    private final String url;
    private final Integer width;
    private final Integer height;
    private final long sizeBytes;

    public static PostImageReadModel of(PostImage image) {
        return PostImageReadModel.builder()
                .imageId(image.getId())
                .url(image.getUrl())
                .width(image.getWidth())
                .height(image.getHeight())
                .sizeBytes(image.getSizeBytes())
                .build();
    }
}
