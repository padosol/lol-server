package com.example.lolserver.community.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 조회수 중복 방지 정책값. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "community.view")
public class CommunityViewProperties {

    /** 같은 조회자가 같은 글을 다시 봐도 조회수를 올리지 않는 기간. */
    private Duration dedupTtl = Duration.ofHours(24);
}
