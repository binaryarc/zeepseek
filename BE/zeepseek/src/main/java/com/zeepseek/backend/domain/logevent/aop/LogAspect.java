package com.zeepseek.backend.domain.logevent.aop;

import com.zeepseek.backend.domain.dong.service.DongService;
import com.zeepseek.backend.domain.logevent.annotation.Loggable;
import com.zeepseek.backend.domain.logevent.event.LogEvent;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class LogAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final DongService dongService; // dong 테이블 조회를 위한 서비스

    public LogAspect(ApplicationEventPublisher eventPublisher, DongService dongService) {
        this.eventPublisher = eventPublisher;
        this.dongService = dongService;
    }

    @Around("@annotation(com.zeepseek.backend.domain.logevent.annotation.Loggable)")
    public Object logEventTrigger(ProceedingJoinPoint joinPoint) throws Throwable {
        // 컨트롤러 메서드 실행
        Object result = joinPoint.proceed();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Loggable loggable = method.getAnnotation(Loggable.class);

        // 기본 extraData 구성
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("method", method.getName());
        extraData.put("args", joinPoint.getArgs());

        // @CookieValue, @PathVariable, @RequestParam 처리 (예시)
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
                if (annotation instanceof RequestBody) {
                    if (args[i] instanceof Map) {
                        Map<String, Object> bodyMap = (Map<String, Object>) args[i];
                        // @Loggable의 type 속성에 따라 분기
                        if ("dong_compare".equals(loggable.type())) {
                            // dong 관련 데이터 처리
                            List<Integer> dongLists = new ArrayList<>();
                            dongLists.add((Integer) bodyMap.get("dong1"));
                            dongLists.add((Integer) bodyMap.get("dong2"));
                            extraData.put("dongIds", dongLists);
                            log.info("동 비교 RequestBody 데이터 추출: {}", dongLists);
                        } else if ("property_compare".equals(loggable.type())) {
                            // property 관련 데이터 처리
                            List<Integer> propertyLists = new ArrayList<>();
                            propertyLists.add((Integer) bodyMap.get("prop1"));
                            propertyLists.add((Integer) bodyMap.get("prop2"));
                            extraData.put("propertyIds", propertyLists);
                            log.info("매물 비교 RequestBody 데이터 추출: {}", propertyLists);
                        }
                    }
                }
            }
        }

        // @RequestBody로 전달된 Map에서 dongName 추출 후 dong 테이블과 조인하여 dongId 조회
//        boolean foundDongName = false;
        for (Object arg : args) {
            if (arg instanceof Map) {
                Map<?, ?> mapArg = (Map<?, ?>) arg;
                if (mapArg.containsKey("dongName") && mapArg.get("dongName") != null) {
                    try {
                        String dongName = (String) mapArg.get("dongName");
                        Integer dongId = dongService.findDongIdByName(dongName);
                        extraData.put("dongId", dongId);
                        log.info("동 이름 변환완료: {} to {}", dongName, dongId);
                    } catch (Exception e) {
                        // 예외 발생 시 기본값 (-1) 할당
                        extraData.put("dongId", -1);
                    }
//                    foundDongName = true;
                    break;
                }
            }
        }
//        if (!foundDongName) {
//            extraData.put("dongId", -1);
//        }

        log.info("extra data: {}", extraData);
        // 로그 이벤트 발행
        eventPublisher.publishEvent(new LogEvent(this, loggable.action(), loggable.type(), extraData));

        return result;
    }
}
