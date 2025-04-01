package com.zeepseek.backend.domain.logevent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Loggable {
    // 액션 이름을 정의할 수 있으며, 필요 시 기본 값을 설정할 수 있습니다.
    String action() default "";
    String type() default "";
}