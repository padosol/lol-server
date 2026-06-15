package com.example.lolserver.duo.adapter.out.lock;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.duo.application.port.out.DuoLockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class DuoLockAdapter implements DuoLockPort {

    private static final long LOCK_WAIT_TIME_SECONDS = 3L;
    private static final long LOCK_LEASE_TIME_SECONDS = 10L;

    private final RedissonClient redissonClient;

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired;
        try {
            acquired = lock.tryLock(
                    LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock acquisition interrupted - key: {}", lockKey, e);
            throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED);
        }
        if (!acquired) {
            log.warn("Lock acquisition failed - key: {}", lockKey);
            throw new CoreException(ErrorType.LOCK_ACQUISITION_FAILED);
        }
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
