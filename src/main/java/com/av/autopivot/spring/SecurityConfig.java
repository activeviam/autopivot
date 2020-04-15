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

import static com.qfs.QfsWebUtils.url;
import static com.qfs.server.cfg.impl.ActivePivotRestServicesConfig.PING_SUFFIX;
import static com.qfs.server.cfg.impl.ActivePivotRestServicesConfig.REST_API_URL_PREFIX;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.Filter;

import com.activeviam.security.cfg.ICorsConfig;
import com.qfs.server.cfg.impl.JwtRestServiceConfig;
import com.qfs.server.cfg.impl.VersionServicesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManagerBuilder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

import com.qfs.content.cfg.impl.ContentServerRestServicesConfig;
import com.qfs.content.service.IContentService;
import com.qfs.jwt.service.IJwtService;
import com.qfs.security.spring.impl.CompositeUserDetailsService;
import com.qfs.server.cfg.IActivePivotConfig;
import com.qfs.server.cfg.IJwtConfig;
import com.qfs.servlet.handlers.impl.NoRedirectLogoutSuccessHandler;
import com.quartetfs.biz.pivot.security.IAuthorityComparator;
import com.quartetfs.biz.pivot.security.impl.AuthorityComparatorAdapter;
import com.quartetfs.biz.pivot.security.impl.UserDetailsServiceWrapper;
import com.quartetfs.fwk.ordering.impl.CustomComparator;
import com.quartetfs.fwk.security.IUserDetailsService;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Generic implementation for security configuration of a server hosting ActivePivot, or Content
 * server or ActiveMonitor.
 * <p>
 * This class contains methods:
 * <ul>
 * <li>To define authorized users</li>,
 * <li>To enable anonymous user access</li>,
 * <li>To configure the JWT filter</li>,
 * <li>To configure the security for Version service</li>.
 * </ul>
 *
 * @author ActiveViam
 */
@EnableGlobalAuthentication
@EnableWebSecurity
@Configuration
public abstract class SecurityConfig implements ICorsConfig {

	public static final String COOKIE_NAME = "AP_JESSIONID";

	public static final String BASIC_AUTH_BEAN_NAME = "basicAuthenticationEntryPoint";

	/** Set to true to allow anonymous access. */
	public static final boolean useAnonymous = false;

	/** Admin user */
	public static final String ROLE_ADMIN = "ROLE_ADMIN";
	
	/** Standard user role */
	public static final String ROLE_USER = "ROLE_USER";

	/** Role for technical components */
	public static final String ROLE_TECH = "ROLE_TECH";
	
	/** Content Server Root role */
	public static final String ROLE_CS_ROOT = IContentService.ROLE_ROOT;

	@Autowired
	protected IJwtConfig jwtConfig;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Returns the default {@link AuthenticationEntryPoint} to use
	 * for the fallback basic HTTP authentication.
	 *
	 * @return The default {@link AuthenticationEntryPoint} for the
	 *         fallback HTTP basic authentication.
	 */
	@Bean(name=BASIC_AUTH_BEAN_NAME)
	public AuthenticationEntryPoint basicAuthenticationEntryPoint() {
		return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth
				.eraseCredentials(false)
				// Add an LDAP authentication provider instead of this to support LDAP
				.userDetailsService(userDetailsService())
				.passwordEncoder(passwordEncoder()).and()
				// Required to allow JWT
				.authenticationProvider(jwtConfig.jwtAuthenticationProvider());
	}

	@Override
	public List<String> getAllowedOrigins() {
		return Collections.singletonList(CorsConfiguration.ALL);
	}

	/**
	 * [Bean] Spring standard way of configuring CORS.
	 *
	 * <p>This simply forwards the configuration of {@link ICorsConfig} to Spring security system.
	 *
	 * @return the configuration for the application.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		final CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(getAllowedOrigins());
		configuration.setAllowedHeaders(getAllowedHeaders());
		configuration.setExposedHeaders(getExposedHeaders());
		configuration.setAllowedMethods(getAllowedMethods());
		configuration.setAllowCredentials(true);

		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	/**
	 * User details service wrapped into an ActiveViam interface.
	 * <p>
	 * This bean is used by {@link AutoPivotConfig}
	 *
	 * @return a user details service
	 */
	@Bean
	public IUserDetailsService avUserDetailsService() {
		return new UserDetailsServiceWrapper(userDetailsService());
	}

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
	
	/**
	 * [Bean] Comparator for user roles
	 * <p>
	 * Defines the comparator used by:
	 * </p>
	 * <ul>
	 *   <li>com.quartetfs.biz.pivot.security.impl.ContextValueManager#setAuthorityComparator(IAuthorityComparator)</li>
	 *   <li>{@link IJwtService}</li>
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

	/**
	 * Common configuration for {@link HttpSecurity}.
	 *
	 * @author ActiveViam
	 */
	public abstract static class AWebSecurityConfigurer extends WebSecurityConfigurerAdapter {

		/** {@code true} to enable the logout URL. */
		protected final boolean logout;

		/** The name of the cookie to clear. */
		protected final String cookieName;

		@Autowired
		protected Environment env;

		@Autowired
		protected ApplicationContext context;

		/**
		 * This constructor does not enable the logout URL.
		 */
		public AWebSecurityConfigurer() {
			this(null);
		}

		/**
		 * This constructor enables the logout URL.
		 *
		 * @param cookieName the name of the cookie to clear
		 */
		public AWebSecurityConfigurer(String cookieName) {
			this.logout = cookieName != null;
			this.cookieName = cookieName;
		}

		/**
		 * {@inheritDoc}
		 * <p>This configures a new firewall accepting `%` in URLs, as none of the core services encode
		 * information in URL. This prevents from double-decoding exploits.<br>
		 * The firewall is also configured to accept `\` - backslash - as none of ActiveViam APIs offer
		 * to manipulate files from URL parameters.<br>
		 * Yet, nor `/` and `.` - slash and point - are accepted, as it may trick the REGEXP matchers
		 * used for security. Support for those two characters can be added at your own risk, by
		 * extending this method. As far as ActiveViam APIs are concerned, `/` and `.` in URL parameters
		 * do not represent any risk. `;` - semi-colon - is also not supported, for various APIs end up
		 * target an actual database, and because this character is less likely to be used.
		 * </p>
		 */
		@Override
		public void configure(WebSecurity web) throws Exception {
			super.configure(web);

			final StrictHttpFirewall firewall = new StrictHttpFirewall();
			firewall.setAllowUrlEncodedPercent(true);
			firewall.setAllowBackSlash(true);

			firewall.setAllowUrlEncodedSlash(false);
			firewall.setAllowUrlEncodedPeriod(false);
			firewall.setAllowSemicolon(false);
			web.httpFirewall(firewall);
		}

		@Override
		protected final void configure(final HttpSecurity http) throws Exception {
			final Filter jwtFilter = context.getBean(IJwtConfig.class).jwtFilter();

			http
					// As of Spring Security 4.0, CSRF protection is enabled by default.
					.csrf().disable()
					.cors().and()
					// To allow authentication with JWT (Required for ActiveUI)
					.addFilterAfter(jwtFilter, SecurityContextPersistenceFilter.class);

			if (logout) {
				// Configure logout URL
				http.logout()
						.permitAll()
						.deleteCookies(cookieName)
						.invalidateHttpSession(true)
						.logoutSuccessHandler(new NoRedirectLogoutSuccessHandler());
			}

			if (useAnonymous) {
				// Handle anonymous users. The granted authority ROLE_USER
				// will be assigned to the anonymous request
				http.anonymous().principal("guest").authorities(ROLE_USER);
			}

			doConfigure(http);
		}

		/**
		 * Applies the specific configuration for the endpoint.
		 * @see #configure(HttpSecurity)
		 */
		protected abstract void doConfigure(HttpSecurity http) throws Exception;
	}


	/**
	 * Configuration for ActiveUI.
	 *
	 * @author ActiveViam
	 * @see HttpStatusEntryPoint
	 */
	@Configuration
	@Order(1)
	public static class ActiveUISecurityConfigurer extends AWebSecurityConfigurer {

		@Override
		protected void doConfigure(HttpSecurity http) throws Exception {
			// Permit all on ActiveUI resources and the root (/) that redirects to ActiveUI index.html.
			final String pattern = "^(.{0}|\\/|\\/" + ActiveUIResourceServerConfig.NAMESPACE + "(\\/.*)?)$";
			http
					// Only theses URLs must be handled by this HttpSecurity
					.regexMatcher(pattern)
					.authorizeRequests()
					// The order of the matchers matters
					.regexMatchers(HttpMethod.OPTIONS, pattern)
					.permitAll()
					.regexMatchers(HttpMethod.GET, pattern)
					.permitAll();

			// Authorizing pages to be embedded in iframes to have ActiveUI in ActiveMonitor UI
			http.headers().frameOptions().disable();
		}

	}

	/**
	 * To expose the JWT REST service
	 *
	 * @author ActiveViam
	 */
	@Configuration
	@Order(2) // Must be done before ContentServerSecurityConfigurer (because they match common URLs)
	public class JwtSecurityConfigurer extends WebSecurityConfigurerAdapter {

		@Autowired
		protected ApplicationContext context;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			final AuthenticationEntryPoint basicAuthenticationEntryPoint = context.getBean(
					BASIC_AUTH_BEAN_NAME,
					AuthenticationEntryPoint.class);
			http
					.antMatcher(JwtRestServiceConfig.REST_API_URL_PREFIX + "/**")
					// As of Spring Security 4.0, CSRF protection is enabled by default.
					.csrf().disable()
					.cors().and()
					.authorizeRequests()
					.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
					.antMatchers("/**").hasAnyAuthority(ROLE_USER)
					.and()
					.httpBasic().authenticationEntryPoint(basicAuthenticationEntryPoint);
		}

	}

	/**
	 * To expose the Version REST service used by ActiveUI.
	 *
	 * @author ActiveViam
	 * @see com.qfs.versions.service.IVersionRestService
	 */
	@Configuration
	@Order(3)
	public class VersionSecurityConfigurer extends WebSecurityConfigurerAdapter {

		@Autowired
		protected ApplicationContext context;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
					.antMatcher(VersionServicesConfig.REST_API_URL_PREFIX + "/**")
					// As of Spring Security 4.0, CSRF protection is enabled by default.
					.cors().and()
					.csrf().disable()
					.authorizeRequests()
					.antMatchers("/**").permitAll();
		}

	}

	/**
	 * To expose the Content REST service and Ping REST service.
	 *
	 * @author ActiveViam
	 *
	 */
	@Configuration
	@Order(5)
	public static class ContentServerSecurityConfigurer extends AWebSecurityConfigurer {

		/** Constructor. */
		public ContentServerSecurityConfigurer() {
			super(COOKIE_NAME);
		}

		@Override
		protected void doConfigure(final HttpSecurity http) throws Exception {
			// The order of antMatchers does matter!
			http.authorizeRequests()
					.antMatchers(HttpMethod.OPTIONS, "/**")
					.permitAll()
					// Ping service used by ActiveUI (not protected)
					.antMatchers(ContentServerRestServicesConfig.REST_API_URL_PREFIX + ContentServerRestServicesConfig.PING_SUFFIX)
					.permitAll()
					.antMatchers("/**")
					.hasAnyAuthority(ROLE_USER, ROLE_TECH)
					.and()
					.httpBasic();
		}
	}
	
	
	/**
	 * Configure security for ActivePivot web services
	 *
	 * @author ActiveViam
	 *
	 */
	@Configuration
	public static class ActivePivotSecurityConfigurer extends AWebSecurityConfigurer {

		@Autowired
		protected IActivePivotConfig activePivotConfig;

		/** Constructor */
		public ActivePivotSecurityConfigurer() {
			super(COOKIE_NAME);
		}

		@Override
		protected void doConfigure(HttpSecurity http) throws Exception {
			http.authorizeRequests()
					// The order of the matchers matters
					.antMatchers(HttpMethod.OPTIONS, REST_API_URL_PREFIX + "/**")
					.permitAll()
					// The REST ping service is temporarily authenticated (see PIVOT-3149)
					.antMatchers(url(REST_API_URL_PREFIX, PING_SUFFIX))
					.hasAnyAuthority(ROLE_USER, ROLE_TECH)
					// REST services
					.antMatchers(REST_API_URL_PREFIX + "/**")
					.hasAnyAuthority(ROLE_USER)
					// One has to be a user for all the other URLs
					.antMatchers("/**")
					.hasAuthority(ROLE_USER)
					.and()
					.httpBasic()
					// SwitchUserFilter is the last filter in the chain. See FilterComparator class.
					.and()
					.addFilterAfter(activePivotConfig.contextValueFilter(), SwitchUserFilter.class);
		}

		@Bean(name = BeanIds.AUTHENTICATION_MANAGER)
		@Override
		public AuthenticationManager authenticationManagerBean() throws Exception {
			return super.authenticationManagerBean();
		}

	}

}
