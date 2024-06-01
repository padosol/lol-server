package com.example.lolserver.aspect;


import com.example.lolserver.aspect.log.LogTrace;
import com.example.lolserver.aspect.log.TraceStatus;
import com.example.lolserver.aspect.log.impl.ThreadLocalLogTrace;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Slf4j
@Component
public class LoggingAspect {

    private LogTrace logTrace = new ThreadLocalLogTrace();

    @Around("execution(* com.example.lolserver.web..*(..))")
    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {

        TraceStatus status = logTrace.begin(String.valueOf(joinPoint));

        try {
            return joinPoint.proceed();
        } finally {
            logTrace.end(status);
        }
    }
}
