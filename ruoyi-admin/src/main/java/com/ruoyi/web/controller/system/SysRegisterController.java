package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.system.service.ISysConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 注册验证
 *
 * @author ruoyi
 */
@Api("注册管理")
@RestController
public class SysRegisterController extends BaseController {
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @ApiOperation("注册用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "用户名称", dataType = "string", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "用户密码", dataType = "string", dataTypeClass = String.class),
            @ApiImplicitParam(name = "code", value = "验证码", dataType = "string", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cardNumbers", value = "银行卡号", dataType = "string", dataTypeClass = String.class),
            @ApiImplicitParam(name = "dateOfBirth", value = "出生日期", dataType = "string", dataTypeClass = Date.class),
            @ApiImplicitParam(name = "address", value = "住址", dataType = "string", dataTypeClass = String.class),
            @ApiImplicitParam(name = "invitationCode", value = "邀请码", dataType = "string", dataTypeClass = String.class)
    })
    @PostMapping("/register")
    public R<String> register(@RequestBody RegisterBody user) {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser")))) {
            return R.fail("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return R.ok(msg);
    }
}
