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

import com.qfs.content.cfg.impl.ContentServerRestServicesConfig;
import com.qfs.content.service.IContentService;
import com.quartetfs.biz.pivot.security.IAuthorityComparator;
import com.quartetfs.biz.pivot.security.impl.AuthorityComparatorAdapter;
import com.quartetfs.fwk.ordering.impl.CustomComparator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Spring configuration of the <b>Content Service</b> backed by a local <b>Content Server</b>.
 * <p>
 * This configuration imports {@link ContentServerRestServicesConfig} to expose the content service.
 *
 * @author ActiveViam
 */
@Configuration
public class RolesConfig {

	/** Admin user */
	public static final String ROLE_ADMIN = "ROLE_ADMIN";

	/** Standard user role */
	public static final String ROLE_USER = "ROLE_USER";

	/** Role for technical components */
	public static final String ROLE_TECH = "ROLE_TECH";

	/** Content Server Root role */
	public static final String ROLE_CS_ROOT = IContentService.ROLE_ROOT;

	/**
	 * [Bean] Comparator for user roles
	 * <p>
	 * Defines the comparator used by:
	 * </p>
	 * <ul>
	 *   <li>com.quartetfs.biz.pivot.security.impl.ContextValueManager#setAuthorityComparator(IAuthorityComparator)</li>
	 *   <li>{@link com.qfs.jwt.service.IJwtService}</li>
	 * </ul>
	 * @return a comparator that indicates which authority/role prevails over another. <b>NOTICE -
	 *         an authority coming AFTER another one prevails over this "previous" authority.</b>
	 *         This authority ordering definition is essential to resolve possible ambiguity when,
	 *         for a given user, a context value has been defined in more than one authority
	 *         applicable to that user. In such case, it is what has been set for the "prevailing"
	 *         authority that will be effectively retained for that context value for that user.
	 */
	@Bean
	public IAuthorityComparator authorityComparator() {
		final CustomComparator<String> comp = new CustomComparator<>();
		comp.setFirstObjects(Arrays.asList(ROLE_USER));
		comp.setLastObjects(Arrays.asList(ROLE_ADMIN));
		return new AuthorityComparatorAdapter(comp);
	}

}
