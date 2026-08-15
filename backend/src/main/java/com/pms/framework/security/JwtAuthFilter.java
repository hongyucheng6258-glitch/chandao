package com.pms.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.result.Result;
import com.pms.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器: 校验 token + Redis 会话双校验(支持强制下线)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String TOKEN_KEY_PREFIX = "login:token:";

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws IOException, jakarta.servlet.ServletException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            String redisKey = TOKEN_KEY_PREFIX + userId;
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached == null || !cached.toString().equals(token)) {
                writeError(response, "登录状态已失效, 请重新登录");
                return;
            }
            Object loginUserObj = redisTemplate.opsForValue().get("login:user:" + userId);
            if (!(loginUserObj instanceof LoginUser loginUser)) {
                writeError(response, "登录状态已失效, 请重新登录");
                return;
            }
            // 滑动续期: 剩余不足一半时刷新
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.MILLISECONDS);
            if (ttl != null && ttl > 0 && ttl < jwtUtil.getExpireMillis() / 2) {
                redisTemplate.expire(redisKey, jwtUtil.getExpireMillis(), TimeUnit.MILLISECONDS);
                redisTemplate.expire("login:user:" + userId, jwtUtil.getExpireMillis(), TimeUnit.MILLISECONDS);
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (io.jsonwebtoken.JwtException e) {
            writeError(response, "token 无效或已过期");
        }
    }

    private void writeError(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(Result.fail(401, msg)));
    }
}
