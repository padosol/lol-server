package com.example.lolserver.community.application.model.readmodel;

import java.util.List;

/**
 * 카테고리 조회의 최종 형태. 그룹핑과 정렬을 서버가 끝낸 트리다.
 *
 * <p>평평한 두 배열(groups + categories)로 내리지 않는 이유: 프론트가 groupCode 로
 * 카테고리를 그룹에 다시 붙이는 과정은 서버가 이미 아는 관계를 재구성하는 것이고
 * 매칭 실수의 여지를 남긴다. 트리로 잃는 것은 라벨 맵을 만들 때의 flatMap 한 번뿐이다.
 */
public record CategoryTreeReadModel(List<BoardGroupReadModel> groups) {
}
