package com.ysh.config.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<VisitorCookieFilter> visitorCookieFilter(@Value("${cookie.secure:false}") boolean isSecureCookie) {
        FilterRegistrationBean<VisitorCookieFilter> registrationBean = new FilterRegistrationBean<>(new VisitorCookieFilter(isSecureCookie));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setName("visitorCookieFilter");
        return registrationBean;

    }


}
