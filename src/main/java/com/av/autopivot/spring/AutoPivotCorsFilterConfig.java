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

import com.qfs.security.cfg.impl.ACorsFilterConfig;
import com.qfs.security.impl.SpringCorsFilter;
import org.springframework.context.annotation.Bean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class AutoPivotCorsFilterConfig extends ACorsFilterConfig {

	@Override
	public Collection<String> getAllowedOrigins() {
		return Arrays.asList();
	}

	@Bean
	@Override
	public SpringCorsFilter corsFilter() throws ServletException {
		final SpringCorsFilter corsFilter = new SpringCorsFilter() {
			private static final String ALREADY_FILTERED_ATTRIBUTE = "SpringCorsFilter.FILTERED";

			@Override
			public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
					throws ServletException, IOException {

				boolean hasAlreadyFilteredAttribute = request.getAttribute(ALREADY_FILTERED_ATTRIBUTE) != null;

				if (hasAlreadyFilteredAttribute) {
					filterChain.doFilter(request, response);
				} else {
					// Do invoke this filter...
					request.setAttribute(ALREADY_FILTERED_ATTRIBUTE, Boolean.TRUE);
					try {
						super.doFilter(request, response, filterChain);
					} finally {
						// Remove the "already filtered" request attribute for this request.
						request.removeAttribute(ALREADY_FILTERED_ATTRIBUTE);
					}
				}
			}
		};
		corsFilter.init(getCorsFilterConfig());
		return corsFilter;
	}
}
