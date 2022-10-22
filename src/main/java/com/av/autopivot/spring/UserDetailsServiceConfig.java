/*
 * (C) ActiveViam 2017-2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.av.autopivot.spring;

import com.qfs.security.spring.impl.CompositeUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManagerBuilder;
import org.springframework.security.provisioning.UserDetailsManager;

import java.util.Arrays;

import static com.av.autopivot.spring.SecurityConfig.*;


/**
 * Spring configuration that defines the users and their associated roles in the application.
 *
 * @author ActiveViam
 */
@Configuration
public class UserDetailsServiceConfig {


	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	/**
	 * [Bean] Create the users that can access the application.
	 *
	 * @return {@link UserDetailsService user data}
	 */
	/**
	 * [Bean] Create the users that can access the application (noop password encoder)
	 *
	 * @return {@link UserDetailsService user data}
	 */
	@Bean
	public UserDetailsService userDetailsService() {
		InMemoryUserDetailsManagerBuilder b = new InMemoryUserDetailsManagerBuilder()
				.withUser("admin").password(passwordEncoder().encode("admin")).authorities(ROLE_USER, ROLE_ADMIN, ROLE_CS_ROOT).and()
				.withUser("user").password(passwordEncoder().encode("user")).authorities(ROLE_USER).and();
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
				.withUser("pivot").password(passwordEncoder().encode("pivot")).authorities(ROLE_TECH, ROLE_CS_ROOT).and()
				.build();
	}

}
