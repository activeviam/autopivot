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

import com.qfs.content.service.impl.InMemoryContentService;
import com.quartetfs.biz.pivot.context.IContextValue;
import com.quartetfs.biz.pivot.definitions.ICalculatedMemberDescription;
import com.quartetfs.biz.pivot.definitions.IKpiDescription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.qfs.content.cfg.impl.ContentServerRestServicesConfig;
import com.qfs.content.service.IContentService;
import com.qfs.pivot.content.IActivePivotContentService;
import com.qfs.pivot.content.impl.ActivePivotContentServiceBuilder;
import com.qfs.server.cfg.content.IActivePivotContentServiceConfig;

/**
 * Spring configuration of the <b>Content Service</b> backed by a local <b>Content Server</b>.
 * <p>
 * This configuration imports {@link ContentServerRestServicesConfig} to expose the content service.
 *
 * @author ActiveViam
 */
@Configuration
public class ContentServiceConfig implements IActivePivotContentServiceConfig {

	/** Role needed to create calculated members */
	private static final String CALCULATED_MEMBER_ROLE = SecurityConfig.ROLE_USER;

	/** Role needed to create KPIs */
	private static final String KPI_ROLE = SecurityConfig.ROLE_USER;


	/**
	 * Service used to store the ActivePivot descriptions and the entitlements (i.e.
	 * {@link IContextValue context values}, {@link ICalculatedMemberDescription calculated members}
	 * and {@link IKpiDescription KPIs}).
	 *
	 * @return the {@link IActivePivotContentService content service} used by the Sandbox
	 *         application
	 */
	@Bean
	@Override
	public IActivePivotContentService activePivotContentService() {
		return new ActivePivotContentServiceBuilder()
				.with(contentService())
				.withCacheForEntitlements(-1)

				// Setup directories and permissions
				.needInitialization(CALCULATED_MEMBER_ROLE, KPI_ROLE)
				.build();
	}

	/**
	 * [Bean] Content Service bean
	 * @return in memory content service
	 */
	@Override
	@Bean
	public IContentService contentService() {
		return new InMemoryContentService();
	}

}
