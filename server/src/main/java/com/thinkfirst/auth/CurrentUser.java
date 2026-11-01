package com.thinkfirst.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    /** 取当前登录用户 ID；未登录时抛 401。 */
    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long id)) {
            throw new InvalidCredentialsException("请先登录");
        }
        return id;
    }
}
