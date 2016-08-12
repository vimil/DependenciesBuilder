package com.cwctravel.eclipse.plugins.dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.internal.launching.launchConfigurations.AntClasspathProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

@SuppressWarnings("restriction")
public class DependenciesClasspathProvider extends AntClasspathProvider {

	@Override
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration) throws CoreException {
		IRuntimeClasspathEntry[] result = super.computeUnresolvedClasspath(configuration);
		List<IRuntimeClasspathEntry> dependenciesRuntimeClasspathEntries = new ArrayList<IRuntimeClasspathEntry>();

		List<DependencyInfo> dependenciesList = null;
		int dependenciesScope = DependencyInfo.getScope(configuration.getAttribute(DependenciesConstants.DEPENDENCIES_SCOPE_ATTR, (String)null));
		try {
			DependenciesParser dependenciesParser = new DependenciesParser(configuration);
			dependenciesParser.parse();
			dependenciesList = dependenciesParser.getDependencies();
		}
		catch(IOException iE) {
			throw new CoreException(new Status(IStatus.ERROR, DependenciesConstants.DEPENDENCIES_PLUGIN_ID, iE.getMessage(), iE));
		}

		if(dependenciesList != null) {
			for(DependencyInfo dependencyInfo: dependenciesList) {
				if(dependenciesScope == DependenciesConstants.DEPENDENCY_SCOPE_NONE || dependenciesScope == dependencyInfo.getScope()) {
					boolean classpathEntryAdded = false;

					String associatedProject = dependencyInfo.getAssociatedProject();
					if(associatedProject != null && !DependenciesConstants.ASSOCIATED_PROJECT_NONE.equals(associatedProject)) {
						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(associatedProject);
						if(DependenciesUtil.isJavaProjectAvailable(project)) {
							IJavaProject javaProject = JavaCore.create(project);
							IRuntimeClasspathEntry runtimeClasspathEntry = JavaRuntime.newProjectRuntimeClasspathEntry(javaProject);
							dependenciesRuntimeClasspathEntries.add(runtimeClasspathEntry);
							classpathEntryAdded = true;
						}
					}

					if(!classpathEntryAdded) {
						IRuntimeClasspathEntry runtimeClasspathEntry = JavaRuntime.newStringVariableClasspathEntry(dependencyInfo.getPath());
						dependenciesRuntimeClasspathEntries.add(runtimeClasspathEntry);
					}
				}
			}
		}

		if(dependenciesRuntimeClasspathEntries != null && !dependenciesRuntimeClasspathEntries.isEmpty()) {
			IRuntimeClasspathEntry[] newResult = new IRuntimeClasspathEntry[result.length + dependenciesRuntimeClasspathEntries.size()];
			System.arraycopy(result, 0, newResult, 0, result.length);
			for(int i = result.length, j = 0; i < newResult.length; i++, j++) {
				newResult[i] = dependenciesRuntimeClasspathEntries.get(j);
			}
			result = newResult;
		}

		return result;
	}
}
