package com.ysh.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 메서드 파라미터에 visitor UUID를 주입
 *
 * 동작 원리:
 *   VisitorCookieFilter가 request attribute "visitorId"에 UUID를 저장 →
 *   VisitorIdArgumentResolver가 해당 attribute를 꺼내 파라미터로 주입.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface VisitorId {}
