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

import com.qfs.server.cfg.impl.JwtConfig;
import com.qfs.util.impl.QfsProperties;

import javax.servlet.SessionCookieConfig;

/**
 * Utility class to configure the cookies &#x1f36a;.
 * <p>
 * For now the configuration consists of
 * <ul>
 * <li>The name of the cookie</li>
 * <li>The life time of the cookie (either defined by the property
 * {@link JwtConfig#EXPIRATION_PROPERTY} in src/main/resources/jwt.properties or using the default
 * value {@link JwtConfig#DEFAULT_EXPIRATION})</li>
 * </ul>
 * <p>
 * This class is used, in particular, to align the lifetime of the cookie with that of the JWT
 * token. Must be done when there are several servers (AP, Content server, ActiveMonitor) with the
 * same URL but running on different ports. Cookies ignore the port (See RFC 6265).
 *
 * @author ActiveViam
 */
public class CookieUtil {

	/**
	 * Configures the cookies &#x1f36a;
	 *
	 * @param config cookie configuration
	 * @param cookieName the name of the cookies
	 * @see SessionCookieConfig#setName(String)
	 * @see SessionCookieConfig#setMaxAge(int)
	 */
	public static void configure(SessionCookieConfig config, String cookieName) {

		// Change the name of the cookie
		config.setName(cookieName);

		// Change the lifetime of the cookie session: it should not be greater than the lifetime of
		// the tokens
		String expiration = QfsProperties.loadProperties("jwt.properties").getProperty(JwtConfig.EXPIRATION_PROPERTY);

		int maxAge = null != expiration ? Integer.parseInt(expiration)
				: JwtConfig.DEFAULT_EXPIRATION;
		config.setMaxAge(maxAge);
	}

}
