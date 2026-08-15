package com.pms.common.utils;

import com.pms.framework.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户
 */
public class SecurityUtil {

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long getUserId() {
        LoginUser u = getLoginUser();
        if (u == null) {
            throw new IllegalStateException("未登录");
        }
        return u.getUser().getId();
    }

    public static Long getUserIdOrNull() {
        LoginUser u = getLoginUser();
        return u == null ? null : u.getUser().getId();
    }
}
