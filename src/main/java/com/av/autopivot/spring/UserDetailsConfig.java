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
import com.qfs.security.spring.impl.CompositeUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManagerBuilder;
import org.springframework.security.provisioning.UserDetailsManager;

import java.util.Arrays;

import static com.av.autopivot.spring.RolesConfig.*;

/**
 * Spring configuration of the <b>Content Service</b> backed by a local <b>Content Server</b>.
 * <p>
 * This configuration imports {@link ContentServerRestServicesConfig} to expose the content service.
 *
 * @author ActiveViam
 */
@Configuration
public class UserDetailsConfig {

	/** Password encoder */
	static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

	/**
	 * [Bean] Create the users that can access the application (noop password encoder)
	 *
	 * @return {@link UserDetailsService user data}
	 */
	@Bean
	public UserDetailsService userDetailsService() {
		InMemoryUserDetailsManagerBuilder b = new InMemoryUserDetailsManagerBuilder()
				.withUser("admin").password(PASSWORD_ENCODER.encode("admin")).authorities(ROLE_USER, ROLE_ADMIN, ROLE_CS_ROOT).and()
				.withUser("user").password(PASSWORD_ENCODER.encode("user")).authorities(ROLE_USER).and();
		return new CompositeUserDetailsService(Arrays.asList(b.build(), technicalUserDetailsService()));
	}

	/**
	 * Creates a technical user to allow ActivePivot to connect
	 * to the content server. (noop password encoder)
	 *
	 * @return {@link UserDetailsService user data}
	 */
	protected UserDetailsManager technicalUserDetailsService() {
		return new InMemoryUserDetailsManagerBuilder()
				.withUser("pivot").password(PASSWORD_ENCODER.encode("pivot")).authorities(ROLE_TECH, ROLE_CS_ROOT).and()
				.build();
	}
}
