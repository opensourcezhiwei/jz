package com.ruoyi.web.controller.admin.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.github.pagehelper.PageInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.zny.constants.Dictionary;
import com.ruoyi.system.zny.entity.*;
import com.ruoyi.system.zny.service.*;
import com.ruoyi.system.zny.utils.DateUtil;
import com.ruoyi.system.zny.utils.MD5Util;
import com.ruoyi.system.zny.utils.RandomUtil;
import com.ruoyi.system.zny.vo.CountLogVo;
import com.ruoyi.system.zny.vo.TotalCountVo;
import com.ruoyi.web.controller.admin.annotation.UserSyncLock;
import com.ruoyi.web.controller.admin.constants.AppConstants;
import com.ruoyi.web.controller.admin.sms.SmsService;
import io.netty.util.internal.StringUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.ruoyi.system.zny.constants.StatusCode.*;

@Api("用户相关")
@RestController
@RequestMapping(value = "/user")
public class UserController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserService userService;

    @Autowired
    private MoneyLogService moneyLogService;

    @Autowired
    private CountLogService countLogService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SignService signService;

    @Autowired
    private ProductActiveService productActiveService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private CountRecordService countRecordService;

    @Autowired
    private PromiseLogService promiseLogService;

    @Autowired
    CouponService couponService;


    @Value("${init.shareId:1380000}")
    private Long initShareId;

    @Operation(summary = "查询用户根据id")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "id", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/queryById")
    public R queryById(Long id, HttpServletRequest request) {
        try {
            if (id == null) {
                return R.fail("id不能为空");
            }
            User user = this.userService.selectById(id);
            if (user == null) {
                return R.fail("用户id不存在");
            }
            filterTel(user);
            return R.ok(user);
        } catch (Exception e) {
            logger.error("查询用户异常: ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "查询用户根据id")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "ids", value = "用户id的json[1,2,3]", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/queryByIds")
    public R queryByIds(String ids) {
        try {
            if (StringUtils.isNull(ids)) {
                return R.fail("id不能为空");
            }
            List<Long> idList = JSONArray.parseArray(ids, Long.class);
            Map<Long, User> userMap = this.userService.selectMapByIds(idList, true);
            if (userMap == null) {
                return R.fail("用户id不存在");
            }
            return R.ok(userMap);
        } catch (Exception e) {
            logger.error("查询用户异常: ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "发送短信")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "tel", value = "手机号码", required = true, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/sendSms")
    public R sendSms(String tel) {
        logger.info("sendSms tel = {}", tel);
        try {
            if (StringUtils.isNull(tel)) {
                return R.fail("手机不能为空");
            }
            String code = RandomUtil.generateNumByLength(6);
            return R.ok(this.smsService.send(tel, code));
        } catch (Exception e) {
            logger.error("sms 失败: ", e);
            return R.fail("验证码发送失败");
        }
    }

    @Operation(summary = "注册")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "tel", value = "手机号码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "loginPassword", value = "登录密码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "tradePassword", value = "交易密码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "realname", value = "真实名字", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "idCard", value = "身份证", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "shareId", value = "邀请码", required = true, dataType = "integer", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "code", value = "图形验证码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "kaptchaId", value = "sessionId图形的", required = true, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/register")
    @UserSyncLock(key = "#param.tel")
    public R register(User param, String code, String kaptchaId, HttpServletRequest request) {
        String ip = getIpAddr(request);
        logger.info("reigster param = {},code={},kaptchaId={}, ip = {}", param.getTel(), code, kaptchaId, ip);
        try {
            if (userService.isVerifyCode()) {
                if (StrUtil.isEmpty(code)) {
                    return R.fail("验证码为空");
                }
                if (!ObjectUtil.equals(code, verify_code_cache.getIfPresent(kaptchaId))) {
                    logger.error("code = {}, kaptchaId = {}, realCode = {}.", code, kaptchaId, verify_code_cache.getIfPresent(kaptchaId));
                    return R.fail("验证码错误");
                }
                verify_code_cache.put(kaptchaId, code);
            } else {
                if (ip_register_limit.getIfPresent(ip) != null) {
                    return R.fail("请稍后尝试注册");
                }
            }
            if (StringUtils.isNull(param.getTel())) {
                return R.fail("手机号码为空");
            }
            param.setTel(param.getTel().trim());
            User user = this.userService.selectByTel(param.getTel());
            if (user != null) {
                return R.fail("手机号已经存在");
            }
            if (StrUtil.isEmpty(param.getLoginPassword())) {
                return R.fail("登录密码为空");
            }
//			if(!IdcardUtil.isValidCard(param.getIdCard())){
//				return result(ERROR, "请输入正确身份证");
//			}
/*			if (StringUtil.isNull(param.getRealname())) {
				return result(ERROR, "真实名字为空");
			}
			if (StringUtil.isNull(param.getIdCard())) {
				return result(ERROR, "身份证为空");
			}*/
            if (param.getShareId() == null) {
                return R.fail("邀请码为空");
            }
            List<SysConfig> list = this.sysConfigService.selectByType(6);
            if (CollectionUtil.isNotEmpty(list)) {
                SysConfig config = list.get(0);
                if (Dictionary.STATUS.ENABLE == config.getStatus()) {
                    if (StringUtils.isNull(code)) {
                        return R.fail(ERROR, "验证码为空");
                    }
                    if (!this.smsService.validCode(param.getTel(), code)) {
                        return R.fail(ERROR, "验证码错误");
                    }
                }
            }
            User parent = this.userService.selectByShareId(param.getShareId());
            if (parent == null) {
                return R.fail(ERROR, "邀请码不存在");
            }
            if (!org.apache.commons.lang3.StringUtils.isBlank(kaptchaId)) {
                verify_code_cache.invalidate(kaptchaId);
            }
            User _parent = new User();
            _parent.setId(parent.getId());
            _parent.setChildren(parent.getChildren() + 1);
            param.setUsername(param.getTel());
            param.setLoginPassword(userService.passwdGenerate(param.getLoginPassword(), param.getTel()));
            if (StrUtil.isNotEmpty(param.getTradePassword())) {
                param.setTradePassword(userService.passwdGenerate(param.getTradePassword(), param.getTel()));
            }
            param.setParentId(parent.getId());
            if (parent.getParentId() != null) {
                param.setParent2Id(parent.getParentId());
            }
            if (parent.getParent2Id() != null) {
                param.setParent3Id(parent.getParent2Id());
            }
            param.setRegisterIp(this.getIpAddr(request));
            param.setRiskLevel(Dictionary.STATUS.ENABLE);
            param.setStatus(Dictionary.STATUS.ENABLE);
            param.setCertNum("E" + System.currentTimeMillis());
            param.setCertDate(new Date());
            this.userService.save(param);
            // 设置顶级代理id
            if (parent.getTopId() == null) {
                param.setTopId(param.getId());
            } else {
                param.setTopId(parent.getTopId());
            }
            param.setShareId(initShareId + param.getId());
            // 借款额度生成
            this.loanGenerate(param);
            this.userService.update(param);
            this.userService.update(_parent);
            // 注册送
            this.registerPrize(param);
            filterTel(param);
            ip_register_limit.put(ip, System.currentTimeMillis() + "");
            return R.ok(param);
        } catch (DuplicateKeyException duplicateKeyException) {
            logger.error("捕获到唯一约束: ", duplicateKeyException.getMessage());
            return R.fail(ERROR, "该手机号无法注册，请联系客服");
        } catch (Exception e) {
            logger.error("注册出错: ", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

    private void loanGenerate(User param) {
        List<SysConfig> configList = this.sysConfigService.selectByType(9);
        if (configList != null && configList.size() > 0) {
            SysConfig sysConfig = configList.get(0);
            if (!StringUtils.isNull(sysConfig.getDesc()) && StringUtils.isNumeric(sysConfig.getDesc()) && //
                    !StringUtils.isNull(sysConfig.getName()) && StringUtils.isNumeric(sysConfig.getName())) {
                Random random = new Random();
                Integer max = Integer.valueOf(sysConfig.getDesc());
                Integer min = Integer.valueOf(sysConfig.getName());
                Integer loanMoney = random.nextInt(max - min + 1) + min;
                loanMoney = loanMoney - (loanMoney % 100);
                param.setWork(loanMoney + "");
            }
        }
    }

    private void registerPrize(User param) {
        List<SysConfig> configList = this.sysConfigService.selectByType(3);
        if (CollectionUtil.isNotEmpty(configList)) {
            SysConfig config = configList.get(0);
            if (!StringUtils.isNull(config.getName())) {
                if (new BigDecimal(config.getName()).compareTo(new BigDecimal(0)) > 0) {
                    this.userService.updateProductMoney(param.getId(), new BigDecimal(config.getName()), IN, Dictionary.MoneyTypeEnum.REGISTER.getKey());
                }
            }
            if (!StringUtils.isNull(config.getDesc())) {
                if (new BigDecimal(config.getDesc()).compareTo(new BigDecimal(0)) > 0) {
                    this.userService.updateMoney(param.getId(), new BigDecimal(config.getDesc()), IN, Dictionary.MoneyTypeEnum.REGISTER.getKey(), Dictionary.MoneyTypeEnum.REGISTER.getValue());
                }
            }
            if (config.getSort() != null) {
                if (config.getSort() != 0) {
                    this.userService.updateProductCount(param.getId(), config.getSort(), IN, Dictionary.MoneyTypeEnum.REGISTER_COUNT.getKey());
                }
            }
            if (config.getVisitCount() != null) {
                if (config.getVisitCount() != 0) {
                    this.userService.updatePoint(param.getId(), config.getVisitCount(), null, IN, Dictionary.MoneyTypeEnum.REGISTER.getKey(), "注册送");
                }
            }
            // yst 独有
            if (StrUtil.isNotEmpty(config.getUrl()) && StringUtils.isNumeric(config.getUrl())) {
                if (new BigDecimal(config.getUrl()).compareTo(new BigDecimal(0)) > 0) {
                    this.userService.updatePromiseMoney(param.getId(), new BigDecimal(config.getUrl()), IN, Dictionary.MoneyTypeEnum.REGISTER.getKey(), "注册送");
                }
            }
        }
    }

    @Operation(summary = "快捷注册")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "tel", value = "手机号码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "loginPassword", value = "登录密码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "shareId", value = "邀请码", required = true, dataType = "integer", dataTypeClass = Integer.class), //
            @ApiImplicitParam(name = "code", value = "验证码", required = false, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/registerQuick")
    @UserSyncLock(key = "#param.tel")
    public R registerQuick(User param, String code, HttpServletRequest request) {
        logger.info("reigsterQuick param = {}", param.getTel());
        try {
            if (StringUtils.isNull(param.getTel())) {
                return R.fail(ERROR, "手机号码为空");
            }
            String ip = this.getIpAddr(request);
//			User ipQuery = new User();
//			ipQuery.setRegisterIp(ip);
//			List<User> ipList = userService.selectByCondition(ipQuery);
//			if(ipList.size() !=0 ) {
//				return result(ERROR, "请勿频繁注册");
//			}
            param.setTel(param.getTel().trim());
            User user = this.userService.selectByTel(param.getTel());
            if (user != null) {
                return R.fail(ERROR, "手机号已经存在");
            }
            if (StringUtils.isNull(param.getLoginPassword())) {
                return R.fail(ERROR, "登录密码为空");
            }
            if (param.getShareId() == null) {
                return R.fail(ERROR, "邀请码为空");
            }
            User parent = this.userService.selectByShareId(param.getShareId());
            if (parent == null) {
                return R.fail(ERROR, "邀请码不存在");
            }
            List<SysConfig> list = this.sysConfigService.selectByType(6);
            if (CollectionUtil.isNotEmpty(list)) {
                SysConfig config = list.get(0);
                if (Dictionary.STATUS.ENABLE == config.getStatus()) {
                    if (StringUtils.isNull(code)) {
                        return R.fail(ERROR, "验证码为空");
                    }
                    if (!this.smsService.validCode(param.getTel(), code)) {
                        return R.fail(ERROR, "验证码错误");
                    }
                }
            }
            parent.setChildren(parent.getChildren() + 1);
            param.setUsername(param.getTel());
            param.setLoginPassword(userService.passwdGenerate(param.getLoginPassword(), param.getTel()));
            param.setParentId(parent.getId());
            param.setParentId(parent.getId());
            if (parent.getParentId() != null) {
                param.setParent2Id(parent.getParentId());
            }
            if (parent.getParent2Id() != null) {
                param.setParent3Id(parent.getParent2Id());
            }
            param.setRegisterIp(ip);
            param.setRiskLevel(Dictionary.STATUS.ENABLE);
            param.setStatus(Dictionary.STATUS.ENABLE);
            param.setCertNum("E" + System.currentTimeMillis());
            this.userService.save(param);
            // 设置顶级代理id
            if (parent.getTopId() == null) {
                param.setTopId(param.getId());
            } else {
                param.setTopId(parent.getTopId());
            }
            param.setShareId(initShareId + param.getId());
            this.userService.update(param);
            this.userService.update(parent);
			/*if (parent.getChildren() == 20) {
				this.userService.updatePoint(parent.getId(),10,new BigDecimal(10),IN,"优质大米10斤");
				this.userService.updatePromiseMoney(parent.getId(), new BigDecimal(400), IN, Dictionary.MoneyTypeEnum.REGISTER.getKey(), "积分赠送");
				couponService.insertUserCoupon(parent.getId(),1L);
				couponService.insertUserCoupon(parent.getId(),1L);
			}else if(parent.getChildren() == 50) {
				this.userService.updatePoint(parent.getId(),30,new BigDecimal(30),IN,"优质大米30斤");
				this.userService.updatePromiseMoney(parent.getId(), new BigDecimal(1000), IN, Dictionary.MoneyTypeEnum.REGISTER.getKey(), "积分赠送");
				couponService.insertUserCoupon(parent.getId(),2L);
				couponService.insertUserCoupon(parent.getId(),2L);
				couponService.insertUserCoupon(parent.getId(),2L);
			}else if(parent.getChildren() == 100) {
				this.userService.updatePoint(parent.getId(),50,new BigDecimal(50),IN,"优质大米50斤");
				this.userService.updatePromiseMoney(parent.getId(), new BigDecimal(1200), IN, Dictionary.MoneyTypeEnum.REGISTER.getKey(), "积分赠送");
				couponService.insertUserCoupon(parent.getId(),3L);
				couponService.insertUserCoupon(parent.getId(),3L);
				couponService.insertUserCoupon(parent.getId(),3L);
				couponService.insertUserCoupon(parent.getId(),3L);
				couponService.insertUserCoupon(parent.getId(),3L);
			}else if(parent.getChildren() == 300) {
				this.userService.updatePoint(parent.getId(),80,new BigDecimal(80),IN,"优质大米80斤");
				this.userService.updatePromiseMoney(parent.getId(), new BigDecimal(2600), IN, Dictionary.MoneyTypeEnum.REGISTER.getKey(), "积分赠送");
				couponService.insertUserCoupon(parent.getId(),5L);
				couponService.insertUserCoupon(parent.getId(),5L);
				couponService.insertUserCoupon(parent.getId(),5L);
				couponService.insertUserCoupon(parent.getId(),5L);
				couponService.insertUserCoupon(parent.getId(),5L);
			}*/
            this.registerPrize(param);
            filterTel(param);
            return R.ok(param);
        } catch (Exception e) {
            logger.error("注册出错2: ", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

    /**
     * 采用spring提供的上传文件的方法
     */
//	@Operation(summary = "人脸注册")
//	@ApiImplicitParams({ //
//			@ApiImplicitParam(name = "username", value = "传入身份证", required = false, dataType = "string", dataTypeClass = String.class), //
//	})
//	@PostMapping(value = "/face/verify", consumes = "multipart/*", headers = "content-type=multipart/form-data")
//	public Map<String, Object> faceVerify(@RequestParam("username") String username, //
//			@RequestParam("app") Byte app, //
//			@RequestPart("file") MultipartFile file, // 头像文件
//			@RequestParam("video") MultipartFile videoFile) { // 视频文件
//		try {
//			if (StringUtil.isNull(username)) {
//				return result(ERROR, "username不能为空");
//			}
//			logger.info("/face/verify app = {}, username = {}", app, username);
//			UploadFiles upload = new UploadFiles();
//			if (file.isEmpty() || videoFile.isEmpty()) {
//				return result(ERROR, "file is empty!");
//			}
//			if (!file.getOriginalFilename().contains(".")) {
//				return result(ERROR, "file format error, must contains . suffix ");
//			}
//			if (!file.getContentType().contains("image")) {
//				return result(ERROR, "文件类型错误");
//			}
//			if (!videoFile.getContentType().contains("video")) {
//				logger.error("错误的视频文件类型 content-type = {}", videoFile.getContentType());
//				return result(ERROR, "视频文件类型错误");
//			}
//			String subffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
//			String fileName = username + System.currentTimeMillis() + subffix;
//			File filePath = new File(fileAddr + File.separator + username, fileName);
//			if (!filePath.getParentFile().exists()) {
//				filePath.getParentFile().mkdirs();
//			}
//			file.transferTo(new File(fileAddr + File.separator + username + File.separator + fileName));
//			logger.info("username = {}, type = {}, filePath = {}, 已更新", username, username, fileUrl + File.separator + username + File.separator + fileName);
//			upload.setApp(app);
//			upload.setCreateTime(new Date());
//			upload.setExtension(subffix);
//			upload.setFileName(fileName);
//			upload.setTel(username);
//			upload.setUrl(fileUrl + File.separator + username + File.separator + fileName);
//			this.uploadFileService.save(upload);
//			return result(SUCCESS, OK, upload);
//		} catch (Exception e) {
//			logger.error("上传出错 : ", e);
//			return result(MAYBE, NETWORK_IS_ERROR);
//		}
//	}
    @Operation(summary = "登录")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "tel", value = "手机号码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "shareId", value = "分享id", required = false, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/loginByShareId")
    public R loginByShareId(User param, HttpServletRequest request) {
        try {
            if (StringUtils.isNull(param.getTel())) {
                return R.fail(ERROR, "账号不能为空");
            }
            logger.info("loginByNo tel = {}, shareId = {}", param.getTel(), param.getShareId());
            User user = this.userService.selectByTel(param.getTel());
            if (user == null) {
                if (param.getShareId() == null) {
                    return R.fail(ERROR, "分享id不能为空");
                }
                user = new User();
                user.setStatus(Dictionary.STATUS.ENABLE);
                user.setTel(param.getTel());
                User parent = null;
                if (param.getShareId() != null) {
                    parent = this.userService.selectByShareId(param.getShareId());
                    if (parent == null) {
                        return R.fail(ERROR, "shareId不存在");
                    }
//					this.userService.updateProductMoney(parent.getId(), new BigDecimal(988), IN, Dictionary.MoneyTypeEnum.ACTIVITY_PRODUCT.getKey());
                }
                this.userService.save(user);
                user.setShareId(initShareId + user.getId());
                if (parent != null) {
                    user.setParentId(parent.getId());
                    user.setParent2Id(parent.getParentId());
                    if (parent.getTopId() != null) {
                        user.setTopId(parent.getTopId());
                    }
                    User _parent = new User();
                    _parent.setId(parent.getId());
                    _parent.setChildren(parent.getChildren() + 1);
                    this.userService.update(_parent);
                }
                this.userService.update(user);
                this.registerPrize(user);
            }
            String sessionId = RandomUtil.generateMixString(32);
            AppConstants.sessionIdMap.put(user.getTel(), sessionId);
            return R.ok(new HashMap<>().put(sessionId, user));
        } catch (Exception e) {
            logger.error("/user/loginByNo 出错: ", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

    @Operation(summary = "登录")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "tel", value = "手机号码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "shareId", value = "分享id", required = false, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/loginByNo")
    public R loginByNo(User param, HttpServletRequest request) {
        try {
            if (StringUtils.isNull(param.getTel())) {
                return R.fail(ERROR, "账号不能为空");
            }
            logger.info("loginByNo tel = {}, shareId = {}", param.getTel(), param.getShareId());
            User user = this.userService.selectByTel(param.getTel());
            if (user == null) {
                user = new User();
                user.setStatus(Dictionary.STATUS.ENABLE);
                user.setTel(param.getTel());
                User parent = null;
                if (param.getShareId() != null) {
                    parent = this.userService.selectByShareId(param.getShareId());
                    if (parent == null) {
                        return R.fail(ERROR, "shareId不存在");
                    }
//					this.userService.updateProductMoney(parent.getId(), new BigDecimal(988), IN, Dictionary.MoneyTypeEnum.ACTIVITY_PRODUCT.getKey());
                }
                this.userService.save(user);
                user.setShareId(initShareId + user.getId());
                if (parent != null) {
                    user.setParentId(parent.getId());
                    user.setParent2Id(parent.getParentId());
                    if (parent.getTopId() != null) {
                        user.setTopId(parent.getTopId());
                    }
                    User _parent = new User();
                    _parent.setId(parent.getId());
                    _parent.setChildren(parent.getChildren() + 1);
                    this.userService.update(_parent);
                }
                this.userService.update(user);
                this.registerPrize(user);
            }
            String sessionId = RandomUtil.generateMixString(32);
            AppConstants.sessionIdMap.put(user.getTel(), sessionId);
            return R.ok(new HashMap<>().put(sessionId, user));
        } catch (Exception e) {
            logger.error("/user/loginByNo 出错: ", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

    @RequestMapping("/code")
    public R getKaptchaImage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 获取验证码
        //    String code = (String) session.getAttribute(Constants.KAPTCHA_SESSION_KEY);
        //    String code = (String) session.getAttribute("Kaptcha_Code");
        // 清除浏览器的缓存
        response.setDateHeader("Expires", 0);
        // Set standard HTTP/1.1 no-cache headers.
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        // Set IE extended HTTP/1.1 no-cache headers (use addHeader).
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        // Set standard HTTP/1.0 no-cache header.
        response.setHeader("Pragma", "no-cache");
        // return a jpeg
        response.setContentType("image/jpeg");
        // 浏览器记忆功能-----当前过浏览器和服务器交互成功以后下载的图片和资源会进行缓存一次。下次刷新的时候就不会在到服务器去下载。
/*	    ServletOutputStream out = response.getOutputStream();
		ValidCodeUtil.generateImage(150, 50, out);*/
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String text = generateImage(130, 48, byteArrayOutputStream);
        String sessionId = MD5Util.string2MD5(RandomUtil.generateMixString(32));
        verify_code_cache.put(sessionId, text);
        return R.ok(new HashMap<>().put(sessionId, "data:image/png;base64," + Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())));
/*	    try {
	        out.flush();
	    } finally {
	        out.close();//关闭
	    }*/
        //return null;
    }

    public static String generateImage(int width, int height, OutputStream out) throws Exception, FileNotFoundException {
        BufferedImage image = new BufferedImage(width, height, 1);
        Graphics g = image.getGraphics();
        g.setColor(Color.CYAN);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLUE);
        g.drawRect(0, 0, width - 1, height - 1);
        String str = "1234567890";
        Random random = new Random();
        String text = saveString(width, height, str, random, g);
        saveLine(width, height, random, g);
        saveImage(image, "jpg", out);
        return text;
    }

    private static Color getrandomColor(Random random) {
        int colorIndex = random.nextInt(4);
        switch (colorIndex) {
            case 0:
                return Color.BLUE;
            case 1:
                return Color.BLACK;
            case 2:
                return Color.GREEN;
            case 3:
                return Color.red;
            case 4:
                return Color.DARK_GRAY;
            default:
                return Color.YELLOW;
        }
    }

    private static String saveString(int width, int height, String str, Random random, Graphics graphics) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 4; ++i) {
            int index = random.nextInt(str.length());
            char ch = str.charAt(index);
            Color color = getrandomColor(random);
            graphics.setColor(color);
            Font font = new Font("宋体", 4, height / 2);
            graphics.setFont(font);
            graphics.drawString(ch + "", i == 0 ? width / 4 * i + 2 : width / 4 * i, height - height / 4);
            result.append(ch);
        }
        return result.toString();
    }

    private static void saveLine(int width, int height, Random random, Graphics graphics) {
        Color lineColor = getrandomColor(random);

        for (int i = 0; i < 10; ++i) {
            int x1 = random.nextInt(width);
            int x2 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int y2 = random.nextInt(height);
            graphics.setColor(lineColor);
            graphics.drawLine(x1, x2, y1, y2);
        }

    }

    private static void saveImage(BufferedImage img, String jpg, OutputStream out) throws Exception {
        ImageIO.write(img, "JPEG", out);
    }
//
//    @Operation(summary = "发送图形验证码")
//    @ApiImplicitParams({ //
//    })
//    @PostMapping(value = "/verifyCode")
//    public R verifyCode() {
//        Captcha captcha = createCaptcha();
//        //request.getSession().setAttribute("Kaptcha_Code",captcha.text());
//        String sessionId = MD5Util.string2MD5(RandomUtil.generateMixString(32));
//        verify_code_cache.put(sessionId, captcha.text());
//        return R.ok(sessionId, captcha.toBase64());
//    }
//
//    private Captcha createCaptcha() {
//        Captcha captcha = new SpecCaptcha(130, 48, 4);
//        captcha.setCharType(2);
//        return captcha;
//    }


//    @Operation(summary = "登录")
//    @ApiImplicitParams({ //
//            @ApiImplicitParam(name = "tel", value = "手机号码", required = true, dataType = "string", dataTypeClass = String.class), //
//            @ApiImplicitParam(name = "loginPassword", value = "登录密码", required = true, dataType = "string", dataTypeClass = String.class), //
//            @ApiImplicitParam(name = "loginIp", value = "登录ip", required = false, dataType = "string", dataTypeClass = String.class), //
//    })
//    @PostMapping(value = "/login")
//    @UserSyncLock(key = "'login:'" + "+#param.tel")
//    public Object login(User param, String code, String kaptchaId, HttpServletRequest request) {
//        try {
//            if (StringUtil.isNull(param.getTel()) || StringUtil.isNull(param.getLoginPassword())) {
//                return result(ERROR, "账号不能为空");
//            }
//            User user = this.userService.login(param);
//            if (user == null) {
//                return result(ERROR, "账号密码错误");
//            }
//            if (user.getStatus() == Dictionary.STATUS.DISABLE) {
//                return result(ERROR, "账号被禁用");
//            }
//            if (this.userService.isVerifyCode()) {
//                if (StrUtil.isEmpty(code)) {
//                    return result(ERROR, "验证码为空");
//                }
//                if (!ObjectUtil.equals(code, verify_code_cache.getIfPresent(kaptchaId))) {
//                    logger.error("code = {}, kaptchaId = {}, realCode = {}.", code, kaptchaId, verify_code_cache.getIfPresent(kaptchaId));
//                    return result(ERROR, "验证码错误");
//                }
//            }
//            User _user = new User();
//            _user.setId(user.getId());
//            _user.setLoginIp(StringUtil.isNull(param.getLoginIp()) ? this.getIpAddr(request) : param.getLoginIp());
//            _user.setLoginCount(user.getLoginCount() + 1);
//            _user.setLoginTime(new Date());
//            this.userService.update(_user);
//            String sessionId = MD5Util.string2MD5(RandomUtil.generateMixString(32) + user.getId());
//            AppConstants.sessionIdMap.put(user.getTel(), sessionId);
//            logger.info("login param = {},{}, sessionId = {}", param.getTel(), param.getLoginPassword(), sessionId);
//            filterTel(user);
//            return R.ok(sessionId, user);
//        } catch (Exception e) {
//            logger.error("/user/login 出错: ", e);
//            return R.fail(MAYBE, "服务器出错");
//        }
//    }

    @Operation(summary = "签到")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/sign")
    @UserSyncLock(key = "#userId")
    public R sign(Long userId) {
        try {
            logger.info("sign id = {}", userId);
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail(ERROR, "用户不存在");
            }
            List<SysConfig> list = this.sysConfigService.selectByType(2);
            if (list == null || list.size() <= 0) {
                return R.fail(ERROR, "系统配置没有签到余额");
            }
            SysConfig config = list.get(0);
            SignRecord record = new SignRecord();
            Date now = new Date();
            record.setCreateDate(now);
            record.setUserId(user.getId());
            List<SignRecord> signList = this.signService.selectByCondition(record, DateUtil.getDayFirstSecond(0), DateUtil.getDayLastSecond(0));
            if (CollectionUtil.isNotEmpty(signList)) {
                return R.fail(ERROR, "今天已经签到过了");
            }
            Object result = null;
            if (!StringUtils.isNull(config.getName())) { // 签到送钱
                BigDecimal signMoney = new BigDecimal(config.getName());
                if (signMoney.compareTo(new BigDecimal(0)) > 0) {
                    user = this.userService.updateProductMoney(user.getId(), signMoney, IN, Dictionary.MoneyTypeEnum.SIGN.getKey());
                    record.setAmount(signMoney);
                    result = signMoney;
                }
            }
            if (StrUtil.isNotEmpty(config.getDesc())) { // 签到送积分
                int signPoint = Integer.parseInt(config.getDesc());
                if (signPoint > 0) {
                    this.userService.updatePoint(user.getId(), signPoint, new BigDecimal(0), IN, Dictionary.MoneyTypeEnum.SIGN.getKey());
                    record.setAmount(new BigDecimal(signPoint));
                    result = signPoint;
                }
            }
            if (config.getSort() != null && config.getSort() > 0) { // 送股权
                this.userService.updateProductCount(userId, config.getSort(), IN, Dictionary.MoneyTypeEnum.SIGN.getKey());
                record.setAmount(new BigDecimal(config.getSort()));
                result = config.getSort();
            }
            if (config.getVisitCount() != null && config.getVisitCount() > 0) { // 送假钱
                this.userService.updateMoney(userId, new BigDecimal(config.getVisitCount()), IN, Dictionary.MoneyTypeEnum.SIGN.getKey(), "签到");
                record.setAmount(new BigDecimal(config.getVisitCount()));
                result = config.getVisitCount();
            }
            if (StrUtil.isNotEmpty(config.getUrl())) { // 送promiseMoney
                this.userService.updatePromiseMoney(userId, new BigDecimal(config.getUrl()), IN, Dictionary.MoneyTypeEnum.SIGN.getKey(), "签到");
                record.setAmount(new BigDecimal(config.getUrl()));
                result = config.getUrl();
            }
            record.setCreateDate(now);
            record.setUserId(user.getId());
            this.signService.save(record);
            return R.ok(result);
        } catch (Exception e) {
            logger.error("签到出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "查询签到")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/sign/query")
    public R querySign(Long userId) {
        try {
            logger.info("sign query userId = {}", userId);
            if (userId == null) {
                return R.fail("用户参数不能为空");
            }
            SignRecord record = new SignRecord();
            record.setUserId(userId);
            List<SignRecord> list = this.signService.selectByCondition(record, null, null);
            return R.ok(list);
        } catch (Exception e) {
            logger.error("签到出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "团队佣金")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/team/rate")
    public R teamRate(Long userId) {
        try {
            if (userId == null) {
                return R.fail("用户不存在");
            }
/*			List<String> moneytType = new ArrayList<>();
			moneytType.add(Dictionary.MoneyTypeEnum.RATE_MONEY.getKey());
			moneytType.add(Dictionary.MoneyTypeEnum.RATE_MONEY2.getKey());
			moneytType.add(Dictionary.MoneyTypeEnum.RATE_MONEY3.getKey());*/

            BigDecimal self = this.moneyLogService.totalByType(null, userId, null, null, null, Dictionary.MoneyTypeEnum.RATE_MONEY.getKey(), null, null, null, null);
            BigDecimal parent = this.moneyLogService.totalByType(null, userId, null, null, null, Dictionary.MoneyTypeEnum.RATE_MONEY2.getKey(), null, null, null, null);
            BigDecimal parent2 = this.moneyLogService.totalByType(null, userId, null, null, null, Dictionary.MoneyTypeEnum.RATE_MONEY.getKey() + "3", null, null, null, null);
            //BigDecimal parent3 = this.moneyLogService.totalByType(null, userId, null, null, null, Dictionary.MoneyTypeEnum.RATE_MONEY.getKey() + "4", null, null, null, null);
            BigDecimal selfToday = this.moneyLogService.totalByType(null, userId, null, null, null, Dictionary.MoneyTypeEnum.RATE_MONEY.getKey(), null, null, DateUtil.getDayFirstSecond(0), null);
            BigDecimal parentToday = this.moneyLogService.totalByType(null, userId, null, null, null, Dictionary.MoneyTypeEnum.RATE_MONEY2.getKey(), null, null, DateUtil.getDayFirstSecond(0), null);
            BigDecimal parent2Today = this.moneyLogService.totalByType(null, userId, null, null, null, Dictionary.MoneyTypeEnum.RATE_MONEY.getKey() + "3", null, null, DateUtil.getDayFirstSecond(0), null);
            //BigDecimal parent3Today = this.moneyLogService.totalByType(null, null, null, null, userId, Dictionary.MoneyTypeEnum.RATE_MONEY.getKey() + "4", null, null, DateUtil.getDayFirstSecond(0), null);
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("self", self);
            resultMap.put("parent", parent);
            resultMap.put("parent2", parent2);
            //resultMap.put("parent3", parent3);
            resultMap.put("total", self.add(parent).add(parent2));
            resultMap.put("today", selfToday.add(parentToday).add(parent2Today));
            return R.ok(resultMap);
        } catch (Exception e) {
            logger.error("我的团队出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "团队佣金")
    @ApiImplicitParams({ //
    })
    @PostMapping(value = "/team/queryByParentId")
    public R queryByParentIdTeam(Long parentId, Long parent2Id, Long parent3Id, Integer pageNum, Integer pageSize) {
        try {
            if (parentId == null && parent2Id == null && parent3Id == null) {
                return R.fail("用户不存在");
            }
            User user = new User();
            user.setParentId(parentId);
            user.setParent2Id(parent2Id);
            user.setParent3Id(parent3Id);
            PageInfo<User> page = this.userService.selectPageByCondition(user, //
                    pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
                    pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
            filterTel(page.getList());
            return R.ok(page);
        } catch (Exception e) {
            logger.error("我的团队出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "我的团队")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/team")
    public R team(Long userId) {
        try {
            if (userId == null) {
                return R.fail("用户不存在");
            }
            List<User> list = this.userService.selectByParentId(userId);
            filterTel(list);
            return R.ok(list);
        } catch (Exception e) {
            logger.error("我的团队出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "我的团队详情")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/teamDetail")
    public R teamDetail(Long userId) {
        try {
            if (userId == null) {
                return R.fail("用户id不能为空");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail("用户不存在");
            }
            TotalCountVo count = this.userService.totalByParentId(userId, null, null);
            TotalCountVo count2 = this.userService.totalByParent2Id(userId, null, null);
            TotalCountVo count3 = this.userService.totalByParent3Id(userId, null, null);
            TotalCountVo total = new TotalCountVo();
            total.setCount(count.getCount() + count2.getCount() + count3.getCount());
            total.setMoney(count.getMoney().add(count2.getMoney()).add(count3.getMoney()));
            total.setMoney2(count.getMoney2().add(count2.getMoney2()).add(count3.getMoney2()));

            TotalCountVo todaycount = this.userService.totalByParentId(userId, DateUtil.getDayFirstSecond(0), null);
            TotalCountVo todaycount2 = this.userService.totalByParent2Id(userId, DateUtil.getDayFirstSecond(0), null);
            TotalCountVo todaycount3 = this.userService.totalByParent3Id(userId, DateUtil.getDayFirstSecond(0), null);

            TotalCountVo totalActive = this.userService.totalActiveByParentId(userId);

            TotalCountVo todaytotal = new TotalCountVo();
            todaytotal.setCount(todaycount.getCount() + todaycount2.getCount() + todaycount3.getCount());
            todaytotal.setMoney(todaycount.getMoney().add(todaycount2.getMoney()).add(todaycount3.getMoney()));
            todaytotal.setMoney2(todaycount.getMoney2().add(todaycount2.getMoney2()).add(todaycount3.getMoney2()));

            String parentName = "";
            if (user.getParentId() != null) {
                User parent = this.userService.selectById(user.getParentId());
                parentName = DesensitizedUtil.chineseName(parent.getRealname());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("totalActive", totalActive.getCount());
            result.put("parentName", parentName);

            TotalCountVo realVerify = this.userService.totalByParentIdAndVerify(userId, null, null);
            result.put("one", realVerify);
            result.put("two", count2);
            result.put("three", count3);
            result.put("z_today", todaytotal);
            result.put("z_today_one", todaycount);
            result.put("z_today_two", todaycount2);
            result.put("z_today_three", todaycount3);
            return R.ok(result);
        } catch (Exception e) {
            logger.error("我的团队出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "个人信息修改/后台资料编辑")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "headIcon", value = "头像", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "tradePassword", value = "交易密码", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "realname", value = "真实姓名(第一次赋值)", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "idCard", value = "身份证号码(第一次才能赋值)", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "idCardZheng", value = "身份证正面", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "idCardFan", value = "身份证反面", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "age", value = "年龄", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "status", value = "0:禁用,1:启用(后台传)", required = false, dataType = "byte", dataTypeClass = Byte.class), //
            @ApiImplicitParam(name = "riskLevel", value = "0:免风控,1:非免风控(后台传)", required = false, dataType = "byte", dataTypeClass = Byte.class), //
            @ApiImplicitParam(name = "sex", value = "1:男,2:女,0:保密(后台传)", required = false, dataType = "byte", dataTypeClass = Byte.class), //
            @ApiImplicitParam(name = "home", value = "家庭住址", required = false, dataType = "byte", dataTypeClass = Byte.class), //
            @ApiImplicitParam(name = "email", value = "邮箱", required = false, dataType = "byte", dataTypeClass = Byte.class), //
            @ApiImplicitParam(name = "work", value = "工作地址", required = false, dataType = "byte", dataTypeClass = Byte.class), //
    })
    @PostMapping(value = "/updateByTel")
    @UserSyncLock(key = "#userId")
    public R updateByTel(Long userId, User param, HttpServletRequest request) {
        try {
            if (userId == null) {
                return R.fail("用户不存在!");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail("用户不存在");
            }
            String sessionId = this.getSessionIdInHeader(request);
            if (StringUtils.isNull(sessionId)) {
                return R.fail("请重新登录");
            }
            String sessionIdInMap = AppConstants.sessionIdMap.get(user.getTel());
            sessionIdInMap = sessionIdInMap == null ? "" : sessionIdInMap;
            if (!sessionId.equals(sessionIdInMap)) {
                logger.error("tel = {}, sessionId = {}, sessionIdInMap = {}", userId, sessionId, sessionIdInMap);
                return R.fail("session不匹配请重新登录");
            }
            User _user = new User();
            _user.setId(user.getId());
            if (!StringUtils.isNull(param.getHeadIcon())) {
                _user.setHeadIcon(param.getHeadIcon());
            }
            if (!StringUtils.isNull(param.getRealname())) {
                logger.info("修改名字 tel = {}, realname = {} -> {}, ip = {}", user.getTel(), user.getRealname(), param.getRealname(), this.getIpAddr(request));
                // zsys出情况, 暂时不让改
                if (StringUtils.isNull(user.getRealname())) {
                    _user.setRealname(param.getRealname());
                }
//				user.setRealname(param.getRealname());
            }
            if (!StringUtils.isNull(param.getIdCard())) {
                if (!param.getIdCard().endsWith("****")) {
                    // 除了第一次, 不让修改
                    if (StringUtils.isNull(user.getIdCard())) {
                        _user.setIdCard(param.getIdCard());
                    }
                }
            }
            if (!StringUtils.isNull(param.getIdCardZheng())) {
                _user.setIdCardZheng(param.getIdCardZheng());
            }
            if (!StringUtils.isNull(param.getIdCardFan())) {
                _user.setIdCardFan(param.getIdCardFan());
            }
            if (param.getAge() != null) {
                _user.setAge(param.getAge());
            }
            if (param.getStatus() != null) {
                _user.setStatus(param.getStatus());
            }
            if (param.getRiskLevel() != null) {
                _user.setRiskLevel(param.getRiskLevel());
            }
            if (param.getSex() != null) {
                _user.setSex(param.getSex());
            }
            if (!StringUtils.isNull(param.getHome())) {
                _user.setHome(param.getHome());
            }
            if (!StringUtils.isNull(param.getEmail())) {
                _user.setEmail(param.getEmail());
            }
            if (!StringUtils.isNull(param.getWork())) {
                _user.setWork(param.getWork());
            }
            this.userService.update(_user);
            User result = this.userService.selectById(userId);
            filterTel(result);
            return R.ok(result);
        } catch (Exception e) {
            logger.error("更新个人资料出错:", e);
            return R.fail(MAYBE, "服务器出错");
        }
    }

    @Operation(summary = "实名认证")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "id", value = "用户的id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "tel", value = "手机号码", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "code", value = "短信验证码", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "tradePassword", value = "交易密码", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "realname", value = "真实姓名", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "idCard", value = "身份证", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "sex", value = "1:男,2:女,0:保密(后台传)", required = false, dataType = "byte", dataTypeClass = Byte.class), //
            @ApiImplicitParam(name = "home", value = "家庭住址", required = false, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/realVerify")
    public R realVerify(Long id, String tel, String code, String tradePassword, String realname, String idCard, Byte sex, String home) {
        try {
            logger.info("id = {}, tradePassword = {}, realname = {}, idCard = {}", id, tradePassword, realname, idCard);
            if (id == null || StringUtils.isNull(realname) || StringUtils.isNull(idCard)) {
                return R.fail("参数不能为空");
            }
            User user = this.userService.selectById(id);
            if (user == null) {
                return R.fail("用户不存在");
            }
            if (StrUtil.isEmpty(user.getTradePassword())) {
                return R.fail("交易密码为空,请修改交易密码");
            }
//			if (!tel.equals(user.getTel())) {
//				return result(ERROR, "手机号不匹配");
//			}
//			if (!this.smsService.validCode(user.getTel(), code)) {
//				return result(ERROR, "验证码不匹配");
//			}
            if (!StringUtils.isNull(user.getIdCard())) {
                return R.fail("已经实名过了");
            }

//			if(!IdcardUtil.isValidCard(idCard)){
//				return result(ERROR, "请输入正确身份证");
//			}
            if (StrUtil.isNotEmpty(tradePassword)) {
                if (!user.getTradePassword().equals(this.userService.passwdGenerate(tradePassword, user.getTel()))) {
                    return R.fail("交易密码错误");
                }
            }
            User _user = new User();
            _user.setId(user.getId());
            if (sex != null) {
                _user.setSex(sex);
            }
            if (!StringUtils.isNull(home)) {
                _user.setHome(home);
            }
            _user.setRealname(realname);
            _user.setIdCard(idCard);
            this.userService.update(_user);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("realVerify 出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "实名认证")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "id", value = "用户的id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "tel", value = "手机号码", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "code", value = "短信验证码", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "tradePassword", value = "交易密码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "realname", value = "真实姓名", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "idCard", value = "身份证", required = true, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/realVerifyRepeat")
    public R realVerifyRepeat(Long id, String tel, String code, String tradePassword, String realname, String idCard) {
        try {
            logger.info("id = {}, tradePassword = {}, realname = {}, idCard = {}", id, tradePassword, realname, idCard);
            if (id == null || StringUtils.isNull(tradePassword) || StringUtils.isNull(realname) || StringUtils.isNull(idCard)) {
                return R.fail("参数不能为空");
            }
            User user = this.userService.selectById(id);
            if (user == null) {
                return R.fail("用户不存在");
            }
//			if(!IdcardUtil.isValidCard(user.getIdCard())){
//				return result(ERROR, "请输入正确身份证");
//			}
//			if (!tel.equals(user.getTel())) {
//				return result(ERROR, "手机号不匹配");
//			}
//			if (!this.smsService.validCode(user.getTel(), code)) {
//				return result(ERROR, "验证码不匹配");
//			}
//			if (!StringUtil.isNull(user.getTradePassword()) || !StringUtil.isNull(user.getIdCard()) || !StringUtil.isNull(user.getRealname())) {
//				return result(ERROR, "已经实名过了");
//			}
            User _user = new User();
            _user.setId(user.getId());
            if (!StringUtils.isNull(tradePassword)) {
                _user.setTradePassword(MD5Util.string2MD5(tradePassword));
            }
            if (StringUtils.isNull(user.getRealname())) {
                _user.setRealname(realname);
            }
            if (StringUtils.isNull(user.getIdCard())) {
                _user.setIdCard(idCard);
            }
            this.userService.update(_user);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("forgetPassword 出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "忘记密码")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "tel", value = "手机号码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "code", value = "短信验证码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "password", value = "登录新密码", required = true, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/forgetPassword")
    public R forgetPassword(String tel, String password, String code) {
        try {
            if (StringUtils.isNull(tel) || StringUtils.isNull(password) || StringUtils.isNull(code)) {
                return R.fail("参数不能为空");
            }
            User user = this.userService.selectByTel(tel);
            if (user == null) {
                return R.fail("手机号不存在");
            }
            if (!this.smsService.validCode(user.getTel(), code)) {
                return R.fail("验证码不匹配");
            }
            user.setLoginPassword(MD5Util.string2MD5(password));
            this.userService.update(user);
            return R.ok(true);
        } catch (Exception e) {
            logger.error("forgetPassword 出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "校验交易密码")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "tradePassword", value = "登录密码", required = true, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/validTradePassword")
    public R validTradePassword(Long userId, String tradePassword) {
        try {
            if (userId == null) {
                return R.fail("用户不存在");
            }
            if (StringUtils.isNull(tradePassword)) {
                return R.fail("旧密码不能为空");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail("用户不存在");
            }
            tradePassword = userService.passwdGenerate(tradePassword, user.getTel());
            if (!tradePassword.equals(user.getTradePassword())) {
                return R.fail("密码错误");
            }
            return R.ok(true);
        } catch (Exception e) {
            logger.error("校验密码错误 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "登录密码修改")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "loginOldPassword", value = "登录旧密码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "loginPassword", value = "登录新密码", required = true, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/updateLoginPassword")
    public R updateLoginPassword(Long userId, String loginOldPassword, String loginPassword) {
        try {
            if (userId == null) {
                return R.fail("用户不存在");
            }
            User tmp = userService.selectById(userId);
            if (tmp == null) {
                return R.fail("用户不存在");
            }
            if (StringUtils.isNull(loginOldPassword)) {
                return R.fail("旧密码不能为空");
            }
            if (StringUtils.isNull(loginPassword)) {
                return R.fail("新密码不能为空");
            }
            User user = new User();
            user.setId(userId);
            user.setLoginPassword(userService.passwdGenerate(loginOldPassword, tmp.getTel()));
            List<User> list = this.userService.selectByCondition(user);
            if (list == null || list.size() <= 0) {
                return R.fail("密码错误");
            }
            User _user = new User();
            _user.setId(list.get(0).getId());
            _user.setLoginPassword(userService.passwdGenerate(loginPassword, tmp.getTel()));
            this.userService.update(_user);
            filterTel(user);
            return R.ok(user);
        } catch (Exception e) {
            logger.error("更新登录密码出错: ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "交易密码修改")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "tradeOldPassword", value = "交易密码", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "tradePassword", value = "交易新密码", required = true, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/updateTradePassword")
    public R updateTradePassword(Long userId, String tradeOldPassword, String tradePassword) {
        try {
            if (userId == null) {
                return R.fail("用户不存在");
            }
            User tmp = userService.selectById(userId);
            if (tmp == null) {
                return R.fail("用户不存在");
            }
            if (StringUtils.isNull(tradeOldPassword)) {
                return R.fail("交易旧密码不能为空");
            }
            if (StringUtils.isNull(tradePassword)) {
                return R.fail("交易密码不能为空");
            }
            User user = new User();
            user.setId(userId);
            user.setTradePassword(userService.passwdGenerate(tradeOldPassword, tmp.getTel()));
            List<User> list = this.userService.selectByCondition(user);
            if (list == null || list.size() <= 0) {
                return R.fail("交易密码错误");
            }
            User _user = new User();
            _user.setId(list.get(0).getId());
            user = list.get(0);
            _user.setTradePassword(userService.passwdGenerate(tradePassword, tmp.getTel()));
            this.userService.update(_user);
            filterTel(user);
            return R.ok(user);
        } catch (Exception e) {
            logger.error("更新交易密码出错 : ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "邀请好友")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/inviteFriend")
    public R inviteFriend(Long userId, User param) {
        try {
            if (userId == null) {
                return R.fail("用户id不存在");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail("用户不存在");
            }
            return R.ok(user.getShareId());
        } catch (Exception e) {
            logger.error("邀请好友出错: ", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "退出登录")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "手机号码, header放sessionId", required = true, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/logout")
    public R logout(Long userId) {
        try {
            if (userId == null) {
                return R.fail("id不能为空");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                logger.error("退出用户不存在");
                return R.fail("退出用户不存在");
            }
            AppConstants.sessionIdMap.remove(user.getTel());
            return R.ok(true);
        } catch (Exception e) {
            logger.error("logout 出错:", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "查询个人资料")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/queryByTel")
    public R queryByTel(Long userId) {
        try {
            if (userId == null) {
                return R.fail("用户不存在");
            }
            User user = this.userService.selectById(userId);
            if (user == null) {
                return R.fail("用户不存在");
            }
//			if (!StringUtil.isNull(user.getIdCard()) && user.getIdCard().length() > 6) {
//				user.setIdCard(user.getIdCard().substring(0, user.getIdCard().length() - 6) + "******");
//			}
            user.setIdCardZheng(null);
            user.setIdCardFan(null);
            filterTel(user);
            return R.ok(user);
        } catch (Exception e) {
            logger.error("查询个人资料", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "股权统计")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/totalByUserId")
    public R totalByUserId(Long userId) {
        try {
            TotalCountVo total = this.productActiveService.total(userId);
            return R.fail(total == null ? new TotalCountVo() : total);
        } catch (Exception e) {
            logger.error("查询个人资料", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "金额统计按类型")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "moneyTypes", value = "[type1, type2]", required = true, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "type", value = "IN转入 OUT转出", required = false, dataType = "string", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/totalByType")
    public R totalByType(Long userId, String moneyTypes, String type, //
                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime, //
                         @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        try {
            List<String> list = null;
            if (!StringUtils.isNull(moneyTypes)) {
                list = JSONArray.parseArray(moneyTypes, String.class);
            }
            BigDecimal amount = this.moneyLogService.totalByType(null, userId, null, type, list, startTime, endTime);
            return R.ok(amount == null ? 0 : amount);
        } catch (Exception e) {
            logger.error("查询个人资料", e);
            return R.ok("服务器出错");
        }
    }

    @Operation(summary = "资金变化统计")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "topId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "tel", value = "手机号码", required = false, dataType = "string", dataTypeClass = String.class), //
            @ApiImplicitParam(name = "userId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "moneyTypes", value = "[type1, type2]", required = true, dataType = "String", dataTypeClass = String.class), //
    })
    @PostMapping(value = "/totalMoneyByType")
    public R totalMoneyByType(Long topId, Long userId, String tel, String moneyTypes, Integer pageNum, Integer pageSize) {
        try {
/*			if(userId == null && tel == null) {
				return result(ERROR,"参数错误");
			}*/
            User user = null;
            if (userId != null) {
                user = userService.selectById(userId);
            } else if (StringUtils.isNotEmpty(tel)) {
                user = userService.selectByTel(tel);
            }
            MoneyLog record = new MoneyLog();
            record.setUserId(userId);
            List<String> typeList = JSONArray.parseArray(moneyTypes, String.class);
            BigDecimal amount = this.moneyLogService.totalByType(topId, user == null ? null : user.getId(), null, typeList, null, null);
//			PageInfo<MoneyLogVo> page = this.moneyLogService.selectPageByCondition(topId, record, null, null, typeList, //
//					pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
//					pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
            return R.ok(amount);
        } catch (Exception e) {
            logger.error("totalMoneyByType", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "股权类型统计")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "topId", value = "用户id", required = false, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
            @ApiImplicitParam(name = "moneyTypes", value = "[type1, type2]", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/totalCountByType")
    public R totalCountByType(Long topId, Long userId, String moneyTypes, Integer pageNum, Integer pageSize) {
        try {
            CountLog record = new CountLog();
            record.setUserId(userId);
            List<String> typeList = JSONArray.parseArray(moneyTypes, String.class);
//			BigDecimal amount = this.countLogService.totalByType(topId, userId, null, typeList);
            PageInfo<CountLogVo> page = this.countLogService.selectPageByCondition(topId, record, null, typeList, //
                    pageNum == null ? DEFAULT_PAGE_NUM : pageNum, //
                    pageSize == null ? DEFAULT_PAGE_SIZE : pageSize);
            return R.ok(page);
        } catch (Exception e) {
            logger.error("totalCountByType", e);
            return R.fail("服务器出错");
        }
    }

    @Operation(summary = "购买过的最高产品")
    @ApiImplicitParams({ //
            @ApiImplicitParam(name = "userId", value = "用户id", required = true, dataType = "long", dataTypeClass = Long.class), //
    })
    @PostMapping(value = "/buyHighest")
    public R buyHighest(Long userId) {
        try {
            ProductActive active = this.productActiveService.selectHighest(userId);
            return R.ok(active == null ? 0 : active.getProductId());
        } catch (Exception e) {
            logger.error("查询个人资料", e);
            return R.fail("服务器出错");
        }
    }


    public static void filterTel(List<User> list) {
        if (list != null && list.size() > 0) {
            for (User user : list) {
                if (!StringUtils.isNull(user.getTel()) && user.getTel().length() >= 11) {
                    user.setTel(user.getTel().substring(0, 3) + "****" + user.getTel().substring(7));
                }
                if (!StringUtils.isNull(user.getUsername()) && user.getUsername().length() >= 11) {
                    user.setUsername(user.getUsername().substring(0, 3) + "****" + user.getUsername().substring(7));
                }
                user.setLoginPassword("");
                user.setTradePassword("");
                user.setIdCard(DesensitizedUtil.idCardNum(user.getIdCard(), 1, 2));
                user.setIdCardFan("");
                user.setIdCardZheng("");
                //user.setRealname(DesensitizedUtil.chineseName(user.getRealname()));
                user.setRegisterIp("");
                user.setLoginIp("");
            }
        }
    }

    public static void filterTel(User user) {
        if (!StringUtils.isNull(user.getTel()) && user.getTel().length() >= 11) {
            user.setTel(user.getTel().substring(0, 3) + "****" + user.getTel().substring(7));
        }
        if (!StringUtils.isNull(user.getUsername()) && user.getUsername().length() >= 11) {
            user.setUsername(user.getUsername().substring(0, 3) + "****" + user.getUsername().substring(7));
        }
        user.setLoginPassword("");
        user.setTradePassword("");
        user.setIdCard(DesensitizedUtil.idCardNum(user.getIdCard(), 1, 2));
        user.setIdCardFan("");
        user.setIdCardZheng("");
        //user.setRealname(DesensitizedUtil.chineseName(user.getRealname()));
        user.setRegisterIp("");
        user.setLoginIp("");
    }

    Cache<String, String> verify_code_cache = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    Cache<String, String> ip_register_limit = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();
}
