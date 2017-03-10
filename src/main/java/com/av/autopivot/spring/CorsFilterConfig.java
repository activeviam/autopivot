/*
 * (C) Quartet FS 2015
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.qfs.security.cfg.impl.ACorsFilterConfig;

/**
 * Spring configuration for CORS filter for the sandbox
 * <p>
 * In the sandbox, the CORS filter will allow request from any server. User should modify the method
 * {@link #getAllowedOrigins()} to allow only authorized url(s).
 *
 * @author Quartet FS
 */
@Configuration
public class CorsFilterConfig extends ACorsFilterConfig {

	@Override
	public List<String> getAllowedOrigins() {
		// Should not be empty in production. Empty means allow all origins.
		// You should put the url(s) of JavaScript code which must access to ActivePivot REST services
		return Arrays.asList();
	}

}
