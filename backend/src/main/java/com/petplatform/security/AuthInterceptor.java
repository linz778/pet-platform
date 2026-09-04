package com.petplatform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petplatform.common.api.Result;
import com.petplatform.common.api.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 登录与角色校验拦截器。放行路径在 WebMvcConfig 中配置。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null) {
            writeError(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        LoginUser user = jwtUtil.parseToken(token);
        if (user == null) {
            writeError(response, ResultCode.UNAUTHORIZED);
            return false;
        }
        UserContext.set(user);

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null && Arrays.stream(requireRole.value()).noneMatch(r -> r.equals(user.getRole()))) {
            writeError(response, ResultCode.FORBIDDEN);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        JwtProperties props = jwtUtil.getProperties();
        String header = request.getHeader(props.getHeader());
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = props.getPrefix();
        if (prefix != null && !prefix.isBlank() && header.startsWith(prefix)) {
            return header.substring(prefix.length()).trim();
        }
        return header.trim();
    }

    private void writeError(HttpServletResponse response, ResultCode code) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Result<Void> body = Result.failed(code);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
