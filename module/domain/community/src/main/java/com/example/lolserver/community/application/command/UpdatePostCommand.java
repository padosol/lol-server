package com.example.lolserver.community.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostCommand {
    private String title;
    private String content;
    private Long categoryId;
    /** 이 글에 첨부할 이미지 id. 없으면 첨부 없음. */
    private List<Long> imageIds;
}
