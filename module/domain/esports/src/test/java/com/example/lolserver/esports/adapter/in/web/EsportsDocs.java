package com.example.lolserver.esports.adapter.in.web;

import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/** e스포츠 컨트롤러 RestDocs 스니펫 공통 설정 (요청·응답 본문 pretty print). */
public final class EsportsDocs {

    private EsportsDocs() {
    }

    public static RestDocumentationResultHandler doc(String identifier) {
        return document(identifier, preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint()));
    }
}
