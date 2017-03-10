/*
 * (C) Quartet FS 2015
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;


import static com.qfs.content.service.IContentService.PATH_SEPARATOR;
import static com.qfs.content.service.IContentService.ROLE_ROOT;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.qfs.content.service.IContentEntry;
import com.qfs.content.service.IContentService;
import com.qfs.content.service.impl.PrefixedContentService;
import com.qfs.fwk.services.ServiceException;
import com.qfs.pivot.builder.discovery.impl.CubeFormatterFactory;
import com.qfs.server.cfg.IActivePivotContentServiceConfig;
import com.qfs.util.impl.QfsFiles;
import com.quartetfs.biz.pivot.cube.formatter.ICubeFormatterFactory;
import com.quartetfs.fwk.QuartetRuntimeException;
import com.quartetfs.fwk.Registry;
import com.quartetfs.fwk.types.IPlugin;

/**
 *
 * i18n configuration
 *
 * @author ActiveViam
 */
@Configuration
public class I18nConfig {

	/** Logger **/
	private static Logger LOGGER = Logger.getLogger(I18nConfig.class.getName());
	
	/** The directory containing the languages in the content service */
	protected static final String I18N_FOLDER = "i18n";

	/** The resource folder that contains language files */
	protected static final String I18N_RESOURCE_FOLDER = I18N_FOLDER;


	/** ActivePivot content service of the application */
	@Autowired
	protected IActivePivotContentServiceConfig apContentServiceConfig;

	
	/**
	 * [Bean action] Initializes the cube formatters
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
				LOGGER.warning(
						"There is already a factory registered for key: "
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
	 * @return void
	 */
	@Bean
	public Void pushTranslations() {
		final IContentService rootContentService = apContentServiceConfig.activePivotContentService().getContentService().withRootPrivileges();
		if (!rootContentService.exists(I18N_FOLDER)) {
			rootContentService.createDirectory(I18N_FOLDER, ROLE_ROOT, ROLE_ROOT, false);
		}

		// Load the languages from files to content service
		try {
			push(new PrefixedContentService(I18N_FOLDER, rootContentService), I18N_RESOURCE_FOLDER);
		} catch (Exception e) {
			throw new QuartetRuntimeException(e);
		}
		
		return null;
	}


	/**
	 * Loads the language data from files to the content service
	 *
	 * @param contentService The content service
	 * @param languageFolder The source folder
	 * @throws ServiceException If the push to the content service failed.
	 * @throws IOException If we failed to read from the languageFolder
	 */
	protected static void push(PrefixedContentService contentService, String languageFolder) throws ServiceException, IOException {

		final URI root = getDirectory(languageFolder);

		// The following code snippet reads the languages in the given languageFolder and push them
		// to the content server (remote or local) which this ActivePivot server connects to
		// The pseudo code is as follow
		//		for (File file : root.listFiles()) {
		//			if (!file.isDirectory()) {
		//				String name = file.getName();
		//				if (contentService.exists(name)) {
		//					contentService.remove(name);
		//				}
		//
		//				contentService.createFile(name, read(file), ROLE_ROOT, ROLE_ROOT, false);
		//			}
		//		}
		// We cannot use the nested loop in our case because the given contextValuesFolder can point to a directory
		// in a .jar. If it is the case we cannot simply create a File object on the languageFolder (API not supported
		// yet on JDK 8). Instead we have to implement a file visitor to perform the workflow of the pseudo code.

		QfsFiles.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				final String fileName = path.getFileName().toString();
				if (fileName.equals(languageFolder)) {
					throw new IllegalArgumentException(languageFolder + " is not a directory");
				}

				if (contentService.exists(fileName)) {
					contentService.remove(fileName);
				}

				contentService.createFile(fileName, read(QfsFiles.getResourceAsStream(path)), ROLE_ROOT, ROLE_ROOT, false);
				return FileVisitResult.CONTINUE;
			}
		});

	}
	
	
	private static URI getDirectory(final String directory) {
		final URI dirUri;
		try {
			dirUri = QfsFiles.getResourceUrl(directory).toURI();
		} catch (URISyntaxException | MalformedURLException e) {
			throw new QuartetRuntimeException("Cannot find the directory " + directory, e);
		}
		if (dirUri == null) {
			throw new IllegalArgumentException("No directory named " + dirUri);
		}

		return dirUri;
	}
	
	
	/**
	 * Read the given {@code in}
	 *
	 * @param in The inputstream to read
	 * @return the file content
	 * @throws IOException if could not read the file
	 */
	protected static String read(InputStream in) throws IOException {
		try (Scanner s = new Scanner(in, "UTF-8")) {
			return s.useDelimiter("\\Z").next();
		}
	}

}