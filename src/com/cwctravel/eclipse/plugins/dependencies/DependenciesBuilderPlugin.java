package com.cwctravel.eclipse.plugins.dependencies;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class DependenciesBuilderPlugin extends AbstractUIPlugin {
	private static BundleContext context;
	private static DependenciesBuilderPlugin instance;

	public static final String TOMCAT_CONTEXT_ICON_ID = "com.cwctravel.eclipse.plugins.dependencies.icons.tomcatContext";
	public static final String TOMCAT_ENABLE_CONTEXT_ICON_ID = "com.cwctravel.eclipse.plugins.dependencies.icons.enableContext";
	public static final String TOMCAT_DISABLE_CONTEXT_ICON_ID = "com.cwctravel.eclipse.plugins.dependencies.icons.disableContext";

	public static DependenciesBuilderPlugin getInstance() {
		return instance;
	}

	public DependenciesBuilderPlugin() {
		instance = this;
	}

	static BundleContext getContext() {
		return context;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		super.initializeImageRegistry(registry);
		Bundle bundle = Platform.getBundle("com.cwctravel.eclipse.plugins.dependencies");

		ImageDescriptor tomcatContextImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/tomcat-context.png"), null));
		registry.put(TOMCAT_CONTEXT_ICON_ID, tomcatContextImage);

		ImageDescriptor enableTomcatContextImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/enable-context.png"), null));
		registry.put(TOMCAT_ENABLE_CONTEXT_ICON_ID, enableTomcatContextImage);

		ImageDescriptor disableTomcatContextImage = ImageDescriptor.createFromURL(FileLocator.find(bundle, new Path("icons/disable-context.png"), null));
		registry.put(TOMCAT_DISABLE_CONTEXT_ICON_ID, disableTomcatContextImage);
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		DependenciesBuilderPlugin.context = bundleContext;
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		super.stop(bundleContext);
		DependenciesBuilderPlugin.context = null;
	}

	public static void log(int severity, String message, Throwable t) {
		getInstance().getLog().log(new Status(severity, DependenciesConstants.DEPENDENCIES_PLUGIN_ID, message, t));
	}

}
