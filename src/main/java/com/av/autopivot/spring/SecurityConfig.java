/*
 * (C) Quartet FS 2015-2016
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;

import static com.qfs.server.cfg.impl.ActivePivotRemotingServicesConfig.ID_GENERATOR_REMOTING_SERVICE;
import static com.qfs.server.cfg.impl.ActivePivotRemotingServicesConfig.LICENSING_REMOTING_SERVICE;
import static com.qfs.server.cfg.impl.ActivePivotRemotingServicesConfig.LONG_POLLING_REMOTING_SERVICE;
import static com.qfs.server.cfg.impl.ActivePivotRestServicesConfig.PING_SUFFIX;
import static com.qfs.server.cfg.impl.ActivePivotRestServicesConfig.REST_API_URL_PREFIX;
import static com.qfs.server.cfg.impl.ActivePivotServicesConfig.ID_GENERATOR_SERVICE;
import static com.qfs.server.cfg.impl.ActivePivotServicesConfig.LICENSING_SERVICE;
import static com.qfs.server.cfg.impl.ActivePivotServicesConfig.LONG_POLLING_SERVICE;
import static com.qfs.server.cfg.impl.CxfServletConfig.CXF_WEB_SERVICES;

import java.util.Arrays;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
import org.springframework.security.provisioning.InMemoryUserDetailsManagerBuilder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

import com.qfs.content.cfg.impl.ContentServerRestServicesConfig;
import com.qfs.content.service.IContentService;
import com.qfs.jwt.impl.JwtFilter;
import com.qfs.jwt.service.IJwtService;
import com.qfs.pivot.servlet.impl.ContextValueFilter;
import com.qfs.security.cfg.ICorsFilterConfig;
import com.qfs.security.spring.impl.CompositeUserDetailsService;
import com.qfs.server.cfg.IActivePivotConfig;
import com.qfs.server.cfg.IJwtConfig;
import com.qfs.server.cfg.impl.JwtRestServiceConfig;
import com.qfs.server.cfg.impl.VersionServicesConfig;
import com.qfs.servlet.handlers.impl.NoRedirectLogoutSuccessHandler;
import com.quartetfs.biz.pivot.security.IAuthorityComparator;
import com.quartetfs.biz.pivot.security.IUserDetailsService;
import com.quartetfs.biz.pivot.security.impl.AuthorityComparatorAdapter;
import com.quartetfs.biz.pivot.security.impl.ContextValueManager;
import com.quartetfs.biz.pivot.security.impl.UserDetailsServiceWrapper;
import com.quartetfs.fwk.ordering.impl.CustomComparator;

/**
 * Common security configuration.
 *
 * @author Quartet FS
 */
@EnableGlobalAuthentication
@EnableWebSecurity
@Configuration
public class SecurityConfig {

	/** Set to true to allow anonymous access */
	public static final boolean useAnonymous = false;

	public static final String ROLE_USER = "ROLE_USER";
	public static final String ROLE_ADMIN = "ROLE_ADMIN";
	public static final String ROLE_TECH = "ROLE_TECH";
	public static final String ROLE_CS_ROOT = IContentService.ROLE_ROOT;

	/**
	 * ROLE_KPI is added to users, to give them permission
	 * to read kpis created by other users in the content server
	 * In order to "share" kpis created in the content server, the kpi
	 * reader role is set to : ROLE_KPI
	 */
	public static final String ROLE_KPI = "ROLE_KPI";

	public static final String PIVOT_USER = "pivot";

	public static final String[] PIVOT_USER_ROLES = { ROLE_TECH, ROLE_CS_ROOT };

	public static final String COOKIE_NAME = "AP_JSESSIONID";
	
	protected static final AuthenticationEntryPoint authenticationEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

	@Autowired
	protected IJwtConfig jwtConfig;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth
				.eraseCredentials(false)
				// Add an LDAP authentication provider instead of this to support LDAP
				.userDetailsService(userDetailsService()).and()
				// Required to allow JWT
				.authenticationProvider(jwtConfig.jwtAuthenticationProvider());
	}

	@Bean
	public UserDetailsService userDetailsService() {
		InMemoryUserDetailsManagerBuilder b = new InMemoryUserDetailsManagerBuilder()
				.withUser("admin").password("admin").authorities(ROLE_USER, ROLE_ADMIN, ROLE_KPI, ROLE_CS_ROOT).and()
				.withUser("manager1").password("manager1").authorities(ROLE_USER, ROLE_KPI).and()
				.withUser("manager2").password("manager2").authorities(ROLE_USER, ROLE_KPI).and();

		return new CompositeUserDetailsService(Arrays.asList(b.build(),
				technicalUserDetailsService()));
	}

	protected UserDetailsManager technicalUserDetailsService() {
		return new InMemoryUserDetailsManagerBuilder()
				// Technical user for ActivePivot Live access
				.withUser("live").password("live").authorities(ROLE_TECH).and()
				// Technical user for ActivePivot server
				.withUser(PIVOT_USER).password("pivot").authorities(PIVOT_USER_ROLES).and()
				.build();
	}

	@Bean
	public JwtFilter jwtFilter(AuthenticationManager authenticationManagerBean) {
		return new JwtFilter(authenticationManagerBean);
	}

	/**
	 * Defines the comparator used by the
	 * {@link ContextValueManager#setAuthorityComparator(IAuthorityComparator) ContextValueManager}
	 * and the {@link IJwtService}.
	 *
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
	 * The authorities of the sentinel technical user
	 * @return the authorities of the sentinel technical user
	 */
	protected static String[] sentinelGrantedAuthorities() {
		return new String[] {
				ROLE_KPI, // ability to read kpis created by users in the content server
				ROLE_USER
		};
	}

	/**
	 * Common configuration for {@link HttpSecurity}.
	 *
	 * @author Quartet FS
	 */
	public static abstract class AWebSecurityConfigurer extends WebSecurityConfigurerAdapter {

		/** {@code true} to enable the logout URL */
		protected final boolean logout;
		/** The name of the cookie to clear */
		protected final String cookieName;

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
			JwtFilter jwtFilter = context.getBean(JwtFilter.class);
			Filter corsFilter = context.getBean(ICorsFilterConfig.class).corsFilter();

			http
					// As of Spring Security 4.0, CSRF protection is enabled by default.
					.csrf().disable()
					// Configure CORS
					.addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
					// To allow authentication with JWT (Required for Live JS)
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
	 * Configuration for JWT.
	 * <p>
	 * The most important point is the {@code authenticationEntryPoint}. It must
	 * only send an unauthorized status code so that JavaScript clients can
	 * authenticate (otherwise the browser will intercepts the response).
	 *
	 * @author Quartet FS
	 * @see HttpStatusEntryPoint
	 */
	public static abstract class AJwtSecurityConfigurer extends WebSecurityConfigurerAdapter {

		@Autowired
		protected ApplicationContext context;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			Filter corsFilter = context.getBean(ICorsFilterConfig.class).corsFilter();

			http
					.antMatcher(JwtRestServiceConfig.NAMESPACE + "/**")
					// As of Spring Security 4.0, CSRF protection is enabled by default.
					.csrf().disable()
					// Configure CORS
					.addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
					.authorizeRequests()
					.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
					.antMatchers("/**").hasAnyAuthority(ROLE_USER)
					.and()
					.httpBasic().authenticationEntryPoint(authenticationEntryPoint);
		}

	}
	
	
	public static abstract class AVersionSecurityConfigurer extends WebSecurityConfigurerAdapter {

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
	 * User details service wrapped into a Quartet interface.
	 * <p>
	 * This bean is used by {@link SentinelAPExtensionServiceConfiguration}
	 *
	 * @return a user details service
	 */
	@Bean
	public IUserDetailsService qfsUserDetailsService() {
		return new UserDetailsServiceWrapper(userDetailsService());
	}

	/**
	 * To expose the login page of ActiveUI.
	 */
	@Configuration
	@Order(1)
	// Must be done before ActivePivotSecurityConfigurer (because they match common URLs)
	public static class LiveSecurityConfigurer extends AWebSecurityConfigurer {

		@Override
		protected void doConfigure(HttpSecurity http) throws Exception {
			final String url = "/ui/**";
			http
				// Only theses URLs must by handled by this HttpSecurity
				.antMatcher(url)
				.authorizeRequests()
				// The order of the matchers matters
				.antMatchers(HttpMethod.OPTIONS, url).permitAll()
				.antMatchers(HttpMethod.GET, url).permitAll()
				// Ping service used by AP Live 4 (not protected)
			    .antMatchers(REST_API_URL_PREFIX + PING_SUFFIX).permitAll();

			// Authorizing pages to be embedded in iframes to have ActivePivot Live in Sentinel UI
			http.headers().frameOptions().disable();
		}

	}

	/**
	 * Only required if ActiveUI use the embedded content server.
	 * <p>
	 * Separated from {@link ActivePivotSecurityConfigurer} to skip the {@link ContextValueFilter}.
	 */
	@Configuration
	@Order(2)
	// Must be done before ActivePivotSecurityConfigurer (because they match common URLs)
	public static class JwtSecurityConfigurer extends AJwtSecurityConfigurer { }

	
	@Configuration
	@Order(3)
	public static class VersionSecurityConfig extends AVersionSecurityConfigurer { }
	
	/**
	 * Only required if the content service is exposed.
	 * <p>
	 * Separated from {@link ActivePivotSecurityConfigurer} to skip the {@link ContextValueFilter}.
	 *
	 * @see LocalContentServiceConfig
	 */
	@Configuration
	@Order(4)
	// Must be done before ActivePivotSecurityConfigurer (because they match common URLs)
	public static class ContentServerSecurityConfigurer extends AWebSecurityConfigurer {

		@Override
		protected void doConfigure(HttpSecurity http) throws Exception {
			final String url = ContentServerRestServicesConfig.NAMESPACE;
			http
					// Only theses URLs must by handled by this HttpSecurity
					.antMatcher(url + "/**")
					.authorizeRequests()
					// The order of the matchers matters
					.antMatchers(HttpMethod.OPTIONS, ContentServerRestServicesConfig.REST_API_URL_PREFIX + "/**").permitAll()
					.antMatchers(url + "/**").hasAuthority(ROLE_USER)
					.and().httpBasic();
		}

	}

	@Configuration
	public static class ActivePivotSecurityConfigurer extends AWebSecurityConfigurer {

		@Autowired
		protected IActivePivotConfig activePivotConfig;

		public ActivePivotSecurityConfigurer() {
			super(COOKIE_NAME);
		}

		@Override
		protected void doConfigure(HttpSecurity http) throws Exception {
			http
					.authorizeRequests()
					// The order of the matchers matters
					.antMatchers(HttpMethod.OPTIONS, REST_API_URL_PREFIX + "/**").permitAll()
					// Web services used by AP live 3.4
					.antMatchers(CXF_WEB_SERVICES + '/' + ID_GENERATOR_SERVICE + "/**").hasAnyAuthority(ROLE_USER, ROLE_TECH)
					.antMatchers(CXF_WEB_SERVICES + '/' + LONG_POLLING_SERVICE + "/**").hasAnyAuthority(ROLE_USER, ROLE_TECH)
					.antMatchers(CXF_WEB_SERVICES + '/' + LICENSING_SERVICE + "/**").hasAnyAuthority(ROLE_USER, ROLE_TECH)
					// Spring remoting services used by AP live 3.5
					.antMatchers(ID_GENERATOR_REMOTING_SERVICE + "/**").hasAnyAuthority(ROLE_USER, ROLE_TECH)
					.antMatchers(LONG_POLLING_REMOTING_SERVICE + "/**").hasAnyAuthority(ROLE_USER, ROLE_TECH)
					.antMatchers(LICENSING_REMOTING_SERVICE + "/**").hasAnyAuthority(ROLE_USER, ROLE_TECH)
					// One has to be a user for all the other URLs
					.antMatchers("/**").hasAuthority(ROLE_USER)
					.and().httpBasic()
					// SwitchUserFilter is the last filter in the chain. See FilterComparator class.
					.and().addFilterAfter(activePivotConfig.contextValueFilter(), SwitchUserFilter.class);
		}

		@Bean(name = BeanIds.AUTHENTICATION_MANAGER)
		@Override
		public AuthenticationManager authenticationManagerBean() throws Exception {
			return super.authenticationManagerBean();
		}

	}
	
}
