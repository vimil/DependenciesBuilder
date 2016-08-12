package com.cwctravel.eclipse.plugins.dependencies;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class DefaultClasspathEntriesProvider implements IClasspathEntriesProvider {

	@Override
	public List<String> getClasspathEntries(IProject project, Set<Integer> scopes) throws IOException, CoreException {
		List<String> result = null;

		DependenciesParser dependenciesParser = DependenciesParser.getParser(project);
		dependenciesParser.parse();

		List<DependencyInfo> dependencies = dependenciesParser.getDependencies();
		if(dependencies != null) {
			if(scopes == null) {
				List<String> classpathEntries = DependenciesUtil.getClasspathEntries(dependencies);
				result = classpathEntries;
			}
			else {
				List<String> classpathEntries = DependenciesUtil.getClasspathEntries(dependencies, scopes);
				result = classpathEntries;
			}
		}
		return result;
	}

}
