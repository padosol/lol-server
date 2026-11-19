package com.example.lolserver.esports.adapter.out.persistence.support;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * DB 제약 위반을 로스터 오류로 바꾼다. 공통 advice 에는 {@link DataIntegrityViolationException} 처리기가 없어
 * 그대로 두면 500 이 나간다. 다른 모듈 응답을 바꾸지 않도록 esports 쓰기 어댑터 안에서만 바꾼다.
 *
 * <p>Postgres 메시지에 제약 이름이 들어 있다 ({@code violates exclusion constraint "ex_contract_no_overlap"}).
 */
public final class EsportsConstraintTranslator {

    private EsportsConstraintTranslator() {
    }

    public static CoreException translate(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        if (message.contains("\"ex_contract_") || message.contains("\"ex_team_name_")) {
            return new CoreException(ErrorType.CONTRACT_PERIOD_CONFLICT);
        }
        if (message.contains("\"pk_") || message.contains("\"uk_")) {
            return new CoreException(ErrorType.ESPORTS_ID_DUPLICATED);
        }
        return new CoreException(ErrorType.INVALID_INPUT);
    }
}
