package com.ysh.config.web;

import com.ysh.common.annotation.VisitorId;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
public class VisitorIdArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(VisitorId.class) && parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter,
                                            @Nullable ModelAndViewContainer mavContainer,
                                            NativeWebRequest webRequest,
                                            @Nullable WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return null;
        }
        return request.getAttribute(VisitorCookieFilter.REQUEST_ATTR_NAME);
    }
}
