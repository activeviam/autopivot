/*
 * (C) Quartet FS 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;

import static com.qfs.QfsWebUtils.url;
import static com.qfs.server.cfg.impl.ActivePivotRestServicesConfig.PING_SUFFIX;
import static com.qfs.server.cfg.impl.ActivePivotRestServicesConfig.REST_API_URL_PREFIX;

import java.util.Arrays;

import javax.servlet.Filter;

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
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManagerBuilder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

import com.qfs.QfsWebUtils;
import com.qfs.content.cfg.impl.ContentServerRestServicesConfig;
import com.qfs.content.service.IContentService;
import com.qfs.jwt.service.IJwtService;
import com.qfs.pivot.servlet.impl.ContextValueFilter;
import com.qfs.security.cfg.ICorsFilterConfig;
import com.qfs.security.spring.impl.CompositeUserDetailsService;
import com.qfs.server.cfg.IActivePivotConfig;
import com.qfs.server.cfg.IJwtConfig;
import com.qfs.server.cfg.content.IActivePivotContentServiceConfig;
import com.qfs.server.cfg.impl.JwtRestServiceConfig;
import com.qfs.server.cfg.impl.VersionServicesConfig;
import com.qfs.servlet.handlers.impl.NoRedirectLogoutSuccessHandler;
import com.quartetfs.biz.pivot.security.IAuthorityComparator;
import com.quartetfs.biz.pivot.security.impl.AuthorityComparatorAdapter;
import com.quartetfs.biz.pivot.security.impl.UserDetailsServiceWrapper;
import com.quartetfs.fwk.ordering.impl.CustomComparator;
import com.quartetfs.fwk.security.IUserDetailsService;

/**
 * Generic implementation for security configuration of a server hosting ActivePivot, or Content
 * server or ActiveMonitor.
 * <p>
 * This class contains methods:
 * <ul>
 * <li>To define authorized users</li>,
 * <li>To enable anomymous user access</li>,
 * <li>To configure the JWT filter</li>,
 * <li>To configure the security for Version service</li>.
 * </ul>
 *
 * @author ActiveViam
 */
@EnableGlobalAuthentication
@EnableWebSecurity
@Configuration
public abstract class SecurityConfig {

	/** Set to true to allow anonymous access */
	public static final boolean useAnonymous = false;

	public static final String BASIC_AUTH_BEAN_NAME = "basicAuthenticationEntryPoint";

	/** Admin user */
	public static final String ROLE_ADMIN = "ROLE_ADMIN";
	
	/** Standard user role */
	public static final String ROLE_USER = "ROLE_USER";

	/** Role for technical components */
	public static final String ROLE_TECH = "ROLE_TECH";
	
	/** Content Server Root role */
	public static final String ROLE_CS_ROOT = IContentService.ROLE_ROOT;

	/** Encrypt Mode (new with Spring Security 5 */
	public static final String SPRING_ENCRYPT = "{bcrypt}";

	@Autowired
	protected IJwtConfig jwtConfig;

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
				.userDetailsService(userDetailsService()).and()
				// Required to allow JWT
				.authenticationProvider(jwtConfig.jwtAuthenticationProvider());
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
	 * [Bean] Create the users that can access the application
	 *
	 * @return {@link UserDetailsService user data}
	 */
	@Bean
	public UserDetailsService userDetailsService() {
		InMemoryUserDetailsManagerBuilder b = new InMemoryUserDetailsManagerBuilder()
				.withUser("admin").password(SPRING_ENCRYPT + new BCryptPasswordEncoder().encode("admin")).authorities(ROLE_USER, ROLE_ADMIN, ROLE_CS_ROOT).and()
				.withUser("user").password(SPRING_ENCRYPT + new BCryptPasswordEncoder().encode("user")).authorities(ROLE_USER).and();

		return new CompositeUserDetailsService(Arrays.asList(b.build(), technicalUserDetailsService()));
	}

	/**
	 * Creates a technical user to allow ActivePivot to connect
	 * to the content server.
	 *
	 * @return {@link UserDetailsService user data}
	 */
	protected UserDetailsManager technicalUserDetailsService() {
		return new InMemoryUserDetailsManagerBuilder()
				.withUser("pivot").password(SPRING_ENCRYPT + new BCryptPasswordEncoder().encode("pivot")).authorities(ROLE_TECH, ROLE_CS_ROOT).and()
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
	 * Common web security configuration for {@link HttpSecurity}.
	 *
	 * @author ActiveViam
	 */
	public static abstract class AWebSecurityConfigurer extends WebSecurityConfigurerAdapter {

		/** {@code true} to enable the logout URL */
		protected final boolean logout;
		/** The name of the cookie to clear */
		protected final String cookieName;

		@Autowired
		protected Environment env;

		@Autowired
		protected ApplicationContext context;

		/**
		 * This constructor does not enable the logout URL
		 */
		public AWebSecurityConfigurer() {
			this(null);
		}

		/**
		 * This constructor enables the logout URL
		 *
		 * @param cookieName the name of the cookie to clear
		 */
		public AWebSecurityConfigurer(String cookieName) {
			this.logout = cookieName != null;
			this.cookieName = cookieName;
		}

		@Override
		protected final void configure(final HttpSecurity http) throws Exception {
			Filter jwtFilter = context.getBean(IJwtConfig.class).jwtFilter();
			Filter corsFilter = context.getBean(ICorsFilterConfig.class).corsFilter();

			http
					// As of Spring Security 4.0, CSRF protection is enabled by default.
					.csrf().disable()
					// Configure CORS
					.addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
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
	 * Configuration for JWT.
	 * <p>
	 * The most important point is the {@code authenticationEntryPoint}. It must
	 * only send an unauthorized status code so that JavaScript clients can
	 * authenticate (otherwise the browser will intercepts the response).
	 *
	 * @author ActiveViam
	 * @see HttpStatusEntryPoint
	 */
	@Configuration
	@Order(2)
	public static class JwtSecurityConfigurer extends WebSecurityConfigurerAdapter {

		@Autowired
		protected ApplicationContext context;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			final Filter corsFilter = context.getBean(ICorsFilterConfig.class).corsFilter();
			final AuthenticationEntryPoint basicAuthenticationEntryPoint = context.getBean(
					BASIC_AUTH_BEAN_NAME,
					AuthenticationEntryPoint.class);
			http
					.antMatcher(JwtRestServiceConfig.REST_API_URL_PREFIX + "/**")
					// As of Spring Security 4.0, CSRF protection is enabled by default.
					.csrf().disable()
					// Configure CORS
					.addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
					.authorizeRequests()
					.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
					.antMatchers("/**").hasAnyAuthority(ROLE_USER)
					.and()
					.httpBasic().authenticationEntryPoint(basicAuthenticationEntryPoint);
		}

	}

	/**
	 * Special configuration for the Version service
	 * that everyone must be allowed to access.
	 *
	 * @author ActiveViam
	 * @see HttpStatusEntryPoint
	 */
	@Configuration
	@Order(3)
	public static class VersionSecurityConfigurer extends WebSecurityConfigurerAdapter {

		@Autowired
		protected ApplicationContext context;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			Filter corsFilter = context.getBean(ICorsFilterConfig.class).corsFilter();

			http
					.antMatcher(VersionServicesConfig.REST_API_URL_PREFIX + "/**")
					// As of Spring Security 4.0, CSRF protection is enabled by default.
					.csrf().disable()
					// Configure CORS
					.addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
					.authorizeRequests()
					.antMatchers("/**").permitAll();
		}

	}

	/**
	 * Only required if the content service is exposed.
	 * <p>
	 * Separated from {@link ActivePivotSecurityConfigurer} to skip the {@link ContextValueFilter}.
	 * <p>
	 * Must be done before ActivePivotSecurityConfigurer (because they match common URLs)
	 *
	 * @see IActivePivotContentServiceConfig
	 */
	@Configuration
	@Order(4)
	public static class ContentServerSecurityConfigurer extends AWebSecurityConfigurer {

		@Override
		protected void doConfigure(HttpSecurity http) throws Exception {
			final String url = ContentServerRestServicesConfig.NAMESPACE;
			http
				// Only theses URLs must be handled by this HttpSecurity
				.antMatcher(url + "/**")
				.authorizeRequests()
				// The order of the matchers matters
				.antMatchers(
					HttpMethod.OPTIONS,
					QfsWebUtils.url(ContentServerRestServicesConfig.REST_API_URL_PREFIX + "**"))
				.permitAll()
				.antMatchers(url + "/**")
				.hasAuthority(ROLE_USER)
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

		/**
		 * Constructor
		 */
		public ActivePivotSecurityConfigurer() {
			super(CookieUtil.COOKIE_NAME);
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
