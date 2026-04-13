package io.github.jukyeong.kvstore.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* io.github.jukyeong.kvstore.controller.*.*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().getName();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("API {} completed in {}ms", method, elapsed);
            return result;
        } catch (Exception e) {
            log.warn("API {} failed: {}", method, e.getMessage());
            throw e;
        }
    }
}
