package com.ruoyi.web.controller.admin.constants;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ruoyi.system.zny.entity.AdminUser;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AppConstants {

	public static final Map<String, String> sessionIdMap = new ConcurrentHashMap<>();


	public static Cache<String, AdminUser> adminSessionCache = CacheBuilder.newBuilder()
			.expireAfterAccess(2, TimeUnit.HOURS)
			.build();

}
