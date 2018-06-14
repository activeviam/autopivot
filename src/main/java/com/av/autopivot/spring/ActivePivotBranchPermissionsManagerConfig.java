/*
 * (C) Quartet FS 2007-2018
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;

import static com.av.autopivot.spring.SecurityConfig.ROLE_ADMIN;
import static com.av.autopivot.spring.SecurityConfig.ROLE_USER;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.qfs.content.service.IContentService;
import com.qfs.content.service.impl.PrefixedContentService;
import com.qfs.security.IBranchPermissions;
import com.qfs.security.IBranchPermissionsManager;
import com.qfs.security.impl.BranchPermissions;
import com.qfs.security.impl.CachedBranchPermissionsManager;
import com.qfs.security.impl.ContentServiceBranchPermissionsManager;
import com.qfs.server.cfg.IActivePivotBranchPermissionsManagerConfig;
import com.qfs.server.cfg.content.IActivePivotContentServiceConfig;


/**
 * Sandbox configuration class creating the manager of branch permissions.
 * @author ActiveViam
 *
 */
@Configuration
public class ActivePivotBranchPermissionsManagerConfig implements IActivePivotBranchPermissionsManagerConfig {

	/** ActivePivot content service spring configuration */
	@Autowired
	protected IActivePivotContentServiceConfig apCSConfig;

	@Override
	@Bean
	public IBranchPermissionsManager branchPermissionsManager() {
		return new CachedBranchPermissionsManager(underlyingBranchPermissionsManager());
	}

	/**
	 * Create and set up the underlying branch permissions manager, which can be used as is, or can
	 * later be wrapped in a cached manager.
	 *
	 * @return The underlying branch permissions manager.
	 */
	protected ContentServiceBranchPermissionsManager underlyingBranchPermissionsManager() {
		// The following list of roles set the users authorized to create branches
		// Here, admins and users can create new branches.
		final Set<String> allowedBranchCreators = new HashSet<>(Arrays.asList(ROLE_ADMIN, ROLE_USER));

		// create the branch permissions manager
		final ContentServiceBranchPermissionsManager contentServiceBranchPermissionsManager =
				new ContentServiceBranchPermissionsManager(prefixedContentService(), allowedBranchCreators);

		// define the default permissions for this manager, which will be used to get the
		// permissions for branches not registered in the manager.
		final BranchPermissions defaultPermissions = new BranchPermissions(
				// Only admins can edit branches, unless specified so
				Collections.singleton(ROLE_ADMIN),
				// All users can see any branch by default
				IBranchPermissions.ALL_USERS_ALLOWED);

		// set the default permissions in the manager
		contentServiceBranchPermissionsManager.setDefaultPermissions(defaultPermissions);

		// define open permissions (no read/write restriction for any user) for the master branch
		contentServiceBranchPermissionsManager.useMasterWithoutRestriction();

		return contentServiceBranchPermissionsManager;
	}

	/**
	 * Creates a content service with a prefix, to put the branch permissions in a specific
	 * directory.
	 *
	 * @return the prefixed content service.
	 */
	protected PrefixedContentService prefixedContentService() {
		final IContentService contentService = apCSConfig.contentService().withRootPrivileges();
		final String path = branchPermissionsPath();
		if (!contentService.exists(path)) {
			contentService.createDirectory(path, IContentService.ROLE_ROOT_LIST, IContentService.ROLE_ROOT_LIST, false);
		}
		final PrefixedContentService prefixedContentService = new PrefixedContentService(path, contentService);
		return prefixedContentService;
	}

	/**
	 * Provides a path for the branch permissions. All branch permissions managed by the
	 * {@link IBranchPermissionsManager} will be stored under this path.
	 * <p>
	 * Override this method if you wish to manually manage multiple permission managers (for
	 * different pivot instances, for example) within the same Content Server.
	 *
	 * @return The path under which to store the branch permissions for this manager.
	 */
	protected String branchPermissionsPath() {
		final String path = "/branches";
		return path;
	}

}
