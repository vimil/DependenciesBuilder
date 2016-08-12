package com.cwctravel.eclipse.plugins.dependencies;

import org.eclipse.core.runtime.Path;

public class DependenciesConstants {
	public static final String DEPENDENCIES_PLUGIN_ID = "com.cwctravel.eclipse.plugins.dependencies.DependencyBuilder";
	public static final String DEPENDENCIES_NATURE_ID = "com.cwctravel.eclipse.plugins.dependencies.DependenciesNature";

	public static final String DEPENDENCIES_PATH_ATTR = "com.cwctravel.eclipse.plugins.dependencies.DEPENDENCIES_PATH";
	public static final String DEPENDENCIES_SCOPE_ATTR = "com.cwctravel.eclipse.plugins.dependencies.DEPENDENCIES_SCOPE";

	public static final String RESOLVER_CLASS_PROPERTY = "RESOLVER_CLASS";
	public static final String DEPENDENCY_ASSOCIATED_PROJECT_PROPERTY_PREFIX = "ASSOCIATED_PROJECT_";

	public static final String DEPENDENCY_SCOPE_LABEL_NONE = "none";
	public static final String DEPENDENCY_SCOPE_LABEL_TEST = "test";
	public static final String DEPENDENCY_SCOPE_LABEL_COMPILE = "compile";
	public static final String DEPENDENCY_SCOPE_LABEL_PROVIDED = "provided";
	public static final String DEPENDENCY_SCOPE_LABEL_RUNTIME = "runtime";
	public static final String DEPENDENCY_SCOPE_LABEL_SYSTEM = "system";

	public static final String ASSOCIATED_PROJECT_NONE = "<none>";

	public static final int DEPENDENCY_SCOPE_NONE = 0;
	public static final int DEPENDENCY_SCOPE_PROVIDED = 1;
	public static final int DEPENDENCY_SCOPE_COMPILE = 2;
	public static final int DEPENDENCY_SCOPE_TEST = 3;
	public static final int DEPENDENCY_SCOPE_SYSTEM = 4;
	public static final int DEPENDENCY_SCOPE_RUNTIME = 5;

	public static final Path DEPENDENCIES_CLASSPATH_CONTAINER_ID = new Path("com.cwctravel.eclipse.plugins.dependencies.DEPENDENCIES_CLASSPATH_CONTAINER");
}
