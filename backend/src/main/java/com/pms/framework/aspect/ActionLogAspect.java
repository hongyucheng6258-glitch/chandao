package com.pms.framework.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.common.annotation.LogOperation;
import com.pms.common.utils.SecurityUtil;
import com.pms.modules.system.entity.SysActionLog;
import com.pms.modules.system.entity.SysUser;
import com.pms.modules.system.mapper.SysActionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面: 拦截 @LogOperation 方法, 成功后落库
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ActionLogAspect {

    private final SysActionLogMapper actionLogMapper;
    private final ObjectMapper objectMapper;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint point, LogOperation logOperation) throws Throwable {
        Object result = point.proceed();
        try {
            Long userId = SecurityUtil.getUserIdOrNull();
            SysUser user = SecurityUtil.getLoginUser() == null ? null : SecurityUtil.getLoginUser().getUser();

            SysActionLog logEntry = new SysActionLog();
            logEntry.setObjectType(logOperation.objectType());
            logEntry.setAction(logOperation.action());
            logEntry.setObjectId(resolveObjectId(point, logOperation.objectId(), result));
            logEntry.setActorId(userId == null ? 0L : userId);
            logEntry.setActorName(user == null ? "" : user.getRealName());

            Map<String, Object> detail = new HashMap<>();
            for (Object arg : point.getArgs()) {
                if (arg != null && isSimpleArg(arg)) {
                    detail.put(arg.getClass().getSimpleName(), arg);
                }
            }
            logEntry.setDetail(objectMapper.writeValueAsString(detail));
            actionLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }
        return result;
    }

    private Long resolveObjectId(ProceedingJoinPoint point, String spel, Object result) {
        if (spel == null || spel.isBlank()) {
            return null;
        }
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = new DefaultParameterNameDiscoverer().getParameterNames(method);
            Object[] args = point.getArgs();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            context.setVariable("result", result);
            Object value = parser.parseExpression(spel).getValue(context);
            return value == null ? null : Long.valueOf(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSimpleArg(Object arg) {
        String pkg = arg.getClass().getPackageName();
        return pkg.startsWith("com.pms") || arg instanceof Number || arg instanceof String;
    }
}
