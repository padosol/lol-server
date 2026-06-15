package com.example.lolserver.duo.application.port.out;

import java.util.function.Supplier;

/**
 * 듀오 컨텍스트의 분산 락 포트.
 *
 * <p>락 획득에 실패하면 구현체는 {@code CoreException(ErrorType.LOCK_ACQUISITION_FAILED)} 를 던진다.
 */
public interface DuoLockPort {

    <T> T executeWithLock(String lockKey, Supplier<T> action);
}
