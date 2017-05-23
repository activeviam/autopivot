/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
