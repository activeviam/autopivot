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
import com.qfs.content.service.IContentEntry;
import com.qfs.content.service.IContentService;
import com.qfs.content.service.impl.PrefixedContentService;
import com.qfs.fwk.services.ServiceException;
import com.qfs.pivot.builder.discovery.impl.CubeFormatterFactory;
import com.qfs.pivot.content.IActivePivotContentService;
import com.qfs.server.cfg.content.IActivePivotContentServiceConfig;
import com.quartetfs.biz.pivot.cube.formatter.ICubeFormatterFactory;
import com.quartetfs.fwk.Registry;
import com.quartetfs.fwk.types.IPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.logging.Logger;

import static com.qfs.content.service.IContentService.PATH_SEPARATOR;
import static com.qfs.content.service.IContentService.ROLE_ROOT;

/**
 * Initialize the cube formatters
 */
//@Configuration
public class CustomI18nConfig {

    /**
     * Logger
     **/
    protected static Logger LOGGER = Logger.getLogger(CustomI18nConfig.class.getSimpleName());

    /**
     * The directory containing the languages in the content service
     */
    protected static final String I18N_FOLDER = "i18n";

    /**
     * The resource folder that contains language files
     */
    protected static final String I18N_RESOURCE_FOLDER = I18N_FOLDER;

    @Autowired
    protected IActivePivotContentServiceConfig apContentServiceConfig;

    /**
     * [Bean action] Initializes the cube formatters
     *
     * @return void
     */
    @Bean
    @DependsOn(value = "pushTranslations")
    public Void initializeCubeFormatters() {
        final IContentService rootContentService = apContentServiceConfig
                .activePivotContentService().getContentService().withRootPrivileges();
        // Scan the content service to retrieve languages
        IContentEntry languageDir = rootContentService.get(I18N_FOLDER);
        if (null == languageDir)
            return null;

        final IPlugin<ICubeFormatterFactory> factories = Registry.getPlugin(ICubeFormatterFactory.class);
        final Map<String, IContentEntry> languages = rootContentService.listDir(I18N_FOLDER, 1, false);
        for (Entry<String, IContentEntry> e : languages.entrySet()) {
            if (e.getValue().isDirectory())
                continue;

            final String path = e.getKey();
            // i18n/fr-Fr -->  fr-Fr
            final String lang = path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1);

            if (factories.valueOf(lang) != null)
                LOGGER.warning("There is already a factory registered for key: "
                        + lang
                        + ". Will replace with newly created one.");

            // The content service language folder
            IContentService langDirectory = new PrefixedContentService(path, rootContentService);

            // Create a cube formatter factory with real time notification
            // Each time the language file is modified, the factory should reload its cache
            final CubeFormatterFactory factory = new CubeFormatterFactory(lang, langDirectory, -1, true);
            factories.add(factory);
        }

        return null;
    }

    /**
     * [Bean action] Loads the language data from files to the content service
     *
     * @return void
     */
    @Bean
    public Void pushTranslations() {
        pushTranslations(apContentServiceConfig.activePivotContentService());
        return null;
    }

    /**
     * Loads the language data from files to the content service
     *
     * @param apContentService The AP content service to push the translations to.
     */
    public static void pushTranslations(IActivePivotContentService apContentService) {
        final IContentService rootContentService = apContentService.getContentService().withRootPrivileges();
        if (!rootContentService.exists(I18N_FOLDER)) {
            rootContentService.createDirectory(I18N_FOLDER, Collections.singletonList(ROLE_ROOT), Collections.singletonList(ROLE_ROOT), false);
        }

        // Load the languages from files to content service


        try {
            push(new PrefixedContentService(I18N_FOLDER, rootContentService), I18N_RESOURCE_FOLDER);
        } catch (Exception e) {
            throw new ActiveViamRuntimeException(e);
        }
    }

    /**
     * Loads the language data from files to the content service
     *
     * @param contentService The content service
     * @param languageFolder The source folder
     * @throws ServiceException If the push to the content service failed.
     * @throws IOException      If we failed to read from the languageFolder
     */
    protected static void push(PrefixedContentService contentService, String languageFolder)
            throws ServiceException, IOException {

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        LOGGER.info("Scanning for languages in: " + languageFolder);
        Resource[] resources = resolver.getResources(languageFolder + "/*");
        LOGGER.info("Found " + resources.length + " matching resources");
        for (Resource resource : resources) {
            LOGGER.fine(resource.getDescription());
            String name = resource.getFilename();
            LOGGER.info("Found language file " + name);
            if (contentService.exists(name)) {
                contentService.remove(name);
            }
            contentService.createFile(name, read(resource.getInputStream()), Collections.singletonList(ROLE_ROOT), Collections.singletonList(ROLE_ROOT), false);
        }
    }

    protected static String read(InputStream stream) throws IOException {
        try (Scanner s = new Scanner(stream, "UTF-8")) {
            return s.useDelimiter("\\Z").next();
        }
    }

}
