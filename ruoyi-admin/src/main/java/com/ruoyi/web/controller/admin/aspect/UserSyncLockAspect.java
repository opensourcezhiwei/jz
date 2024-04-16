package com.ruoyi.web.controller.admin.aspect;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ruoyi.system.zny.constants.StatusCode.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.MAYBE;

@Aspect
@Component
public class UserSyncLockAspect {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    Cache<String,Long> mutexCache = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.SECONDS)
            .build();

    ExpressionParser parser = new SpelExpressionParser();

    @Pointcut("execution(* com.ruoyi.web.controller.*.*(..)) && @annotation(com.ruoyi.web.controller.admin.annotation.UserSyncLock)")
    public void around(){

    }


    @Around("around()")
    public R requestHandle(ProceedingJoinPoint jp) {
        try {
            UserSyncLock lockAnno = this.getAnnotation(jp);
            String key = lockAnno.key();
            MethodSignature methodSignature = (MethodSignature) jp.getSignature();
            String[] nameArr = methodSignature.getParameterNames();
            Object[] objArr = jp.getArgs();

            EvaluationContext ctx = new StandardEvaluationContext();
            for (int i = 0; i < nameArr.length; i++) {
                ctx.setVariable(nameArr[i], objArr[i]);
            }

            Object excpressResult = parser.parseExpression(key).getValue(ctx);
            String lockKey = null;
            if (excpressResult == null) {
                logger.error("接口参数为NULL，锁获取失效，请检查接口参数，该参数必须携带. {}, {}, {}" , methodSignature.getDeclaringType(), methodSignature.getName(),key);
            }else {
                lockKey = excpressResult.toString();
                Long value = mutexCache.asMap().putIfAbsent(lockKey,  System.currentTimeMillis());
                if(value != null &&  System.currentTimeMillis() - value < 3 * 1000) {
                    //锁未释放
                    logger.error("Get lock fail . key = [{}] . value = [{}] ", lockKey, value);
                    return R.fail(ERROR, "请稍后操作");
                }
            }

            try{
                return R.ok(jp.proceed());
            }finally {
            }
        }
        catch (javax.validation.ConstraintViolationException e) {
            final String[] msg = {""};
            e.getConstraintViolations().stream().findFirst().ifPresent(obj->{
                msg[0] = obj.getMessage();
            });
            logger.error("参数不符 {}",e.getMessage());
            return R.fail(ERROR, msg[0]);
        }
        catch (Throwable e) {
            logger.error("RedisDistributeLock error", e);
        }
        return R.fail(MAYBE, "服务器出错");
    }


    protected Map<String, Object> result(String code, Object msg) {
        return this.result(code, msg, (Object)null);
    }

    protected Map<String, Object> result(String code, Object msg, Object data) {
        Map<String, Object> resultMap = new HashMap();
        resultMap.put("status", code);
        resultMap.put("msg", msg);
        resultMap.put("data", data);
        return resultMap;
    }
    private UserSyncLock getAnnotation(JoinPoint joinPoint) throws Exception {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();

        if (method != null) {
            return method.getAnnotation(UserSyncLock.class);
        }
        return null;
    }
}
