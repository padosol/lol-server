package com.example.lolserver.esports.domain;

/** 팀의 이름·코드·로고 한 벌. 개명하면 통째로 바뀐다. */
public record TeamName(String teamCode, String name, String nameEng, String imageUrl, String darkImageUrl) {
}
