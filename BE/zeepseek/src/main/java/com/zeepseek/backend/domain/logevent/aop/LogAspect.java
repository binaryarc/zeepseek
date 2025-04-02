package com.zeepseek.backend.domain.logevent.aop;

import com.zeepseek.backend.domain.logevent.annotation.Loggable;
import com.zeepseek.backend.domain.logevent.event.LogEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class LogAspect {

    private final ApplicationEventPublisher eventPublisher;

    public LogAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Around("@annotation(com.zeepseek.backend.domain.logevent.annotation.Loggable)")
    public Object logEventTrigger(ProceedingJoinPoint joinPoint) throws Throwable {
        // 메서드 실행 전 데이터를 수집할 수 있음
        Object result = joinPoint.proceed(); // 메서드 실행

        // 메서드에 붙은 @Loggable 어노테이션 정보를 가져옴
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Loggable loggable = method.getAnnotation(Loggable.class);

        // 추가 데이터는 필요에 따라 구성 (여기서는 예시로, 메서드 이름과 파라미터 값을 담습니다)
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("method", method.getName());
        extraData.put("args", joinPoint.getArgs());

        // 메서드 파라미터에서 @PathVariable("propertyId")를 찾아 extraData에 추가
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof CookieValue) {
                    CookieValue cv = (CookieValue) annotation;
                    String cookieName = cv.value();
                    if ("userId".equals(cookieName)) {
                        extraData.put("userId", args[i]);
                    }
                    if ("age".equals(cookieName)) {
                        extraData.put("age", args[i]);
                    }
                    if ("gender".equals(cookieName)) {
                        extraData.put("gender", args[i]);
                    }
                    // 다른 쿠키 이름도 필요하면 추가 조건문 작성
                }

                if (annotation instanceof PathVariable) {
                    PathVariable pv = (PathVariable) annotation;
                    String value = pv.value();
                    if ("propertyId".equals(value)) {
                        extraData.put("propertyId", args[i]);
                    }
                    if ("dongId".equals(value)) {
                        extraData.put("dongId", args[i]);
                    }
                    if ("userId".equals(value)) {
                        extraData.put("userId", args[i]);
                    }
                }
            }
        }


        // 로그 이벤트 발행
        eventPublisher.publishEvent(new LogEvent(this, loggable.action(), loggable.type(), extraData));

        return result;
    }
}
