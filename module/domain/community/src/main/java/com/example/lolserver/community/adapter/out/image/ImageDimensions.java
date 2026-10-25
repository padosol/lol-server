package com.example.lolserver.community.adapter.out.image;

/** 디코드 전에 헤더에서 읽어낸 크기. */
record ImageDimensions(int width, int height) {

    long pixels() {
        return (long) width * height;
    }
}
