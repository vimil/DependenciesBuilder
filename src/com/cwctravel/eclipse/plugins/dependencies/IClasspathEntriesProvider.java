package com.cwctravel.eclipse.plugins.dependencies;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public interface IClasspathEntriesProvider {
	public List<String> getClasspathEntries(IProject project, Set<Integer> scopes) throws IOException, CoreException;
}
