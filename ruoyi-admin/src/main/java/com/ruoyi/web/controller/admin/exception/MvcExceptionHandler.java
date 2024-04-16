package com.ruoyi.web.controller.admin.exception;

import com.ruoyi.common.core.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import static com.ruoyi.common.constant.HttpStatus.ERROR;
import static com.ruoyi.system.zny.constants.StatusCode.MAYBE;

@RestControllerAdvice
public class MvcExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(MvcExceptionHandler.class);
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R handle(Exception exception) {
        logger.error("Api BAD_REQUEST exception occur. ", exception.getMessage());
        return R.fail(ERROR, "请求错误");
    }

    @ExceptionHandler(value = Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R handle(Throwable exception) {
        logger.error("Api BAD_REQUEST exception occur. ", exception.getMessage());
        return R.fail(MAYBE, "服务器异常");
    }

    @ExceptionHandler(value = javax.validation.ConstraintViolationException.class)
    public R handle(javax.validation.ConstraintViolationException exception) {
        logger.error("参数不符 {}",exception.getMessage());
        final String[] msg = {""};
        exception.getConstraintViolations().stream().findFirst().ifPresent(obj->{
            msg[0] = obj.getMessage();
        });
        return R.fail(ERROR, msg[0]);
    }
    protected Map<String, Object> result(String code, Object msg, Object data) {
        Map<String, Object> resultMap = new HashMap();
        resultMap.put("status", code);
        resultMap.put("msg", msg);
        resultMap.put("data", data);
        return resultMap;
    }
    protected Map<String, Object> result(String code, Object msg) {
        return this.result(code, msg, (Object)null);
    }
}