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

import com.activeviam.fwk.ActiveViamRuntimeException;
import com.qfs.content.cfg.impl.ContentServerRestServicesConfig;
import com.qfs.content.service.IContentService;
import com.qfs.content.service.impl.InMemoryContentService;
import com.qfs.content.snapshot.impl.ContentServiceSnapshotter;
import com.qfs.pivot.content.IActivePivotContentService;
import com.qfs.pivot.content.impl.ActivePivotContentServiceBuilder;
import com.qfs.server.cfg.content.IActivePivotContentServiceConfig;
import com.qfs.util.impl.QfsFiles;
import com.quartetfs.biz.pivot.context.IContextValue;
import com.quartetfs.biz.pivot.definitions.ICalculatedMemberDescription;
import com.quartetfs.biz.pivot.definitions.IKpiDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration of the <b>Content Service</b> backed by a local <b>Content Server</b>.
 * <p>
 * This configuration imports {@link ContentServerRestServicesConfig} to expose the content service.
 *
 * @author ActiveViam
 */
@Configuration
public class ContentServiceConfig implements IActivePivotContentServiceConfig {

	private static final Logger logger = LoggerFactory.getLogger(ContentServiceConfig.class);

	/** Role needed to create calculated members */
	private static final String CALCULATED_MEMBER_ROLE = RolesConfig.ROLE_USER;

	/** Role needed to create KPIs */
	private static final String KPI_ROLE = RolesConfig.ROLE_USER;


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


	private static final String UI_FOLDER = "/ui";
	private static final String CS_INIT_FILE = "contentserver-init.json";

	@Bean
	public void initActiveUIFolder() {
		final var service = contentService().withRootPrivileges();

		if (service.get(UI_FOLDER) == null) {

			try {
				new ContentServiceSnapshotter(service).importSubtree(
						UI_FOLDER, QfsFiles.getResourceAsStream(CS_INIT_FILE));
				logger.info("Initializing the contentServer with the file: [{}].", CS_INIT_FILE);
			} catch (final Exception e) {
				logger.error("Failed to initialize the /ui folder in the contentServer with the file: [{}].", CS_INIT_FILE, e);

				throw new ActiveViamRuntimeException(
						"Failed to initialize the /ui folder in the contentServer.", e);
			}
		}
	}

}
