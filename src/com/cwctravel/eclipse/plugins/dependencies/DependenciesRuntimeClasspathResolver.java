package com.cwctravel.eclipse.plugins.dependencies;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntryResolver;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

public class DependenciesRuntimeClasspathResolver implements IRuntimeClasspathEntryResolver {

	@Override
	public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry runtimeClasspathEntry,
			ILaunchConfiguration launchConfiguration) throws CoreException {
		IJavaProject javaProject = runtimeClasspathEntry.getJavaProject();
		IClasspathEntry classpathEntry = runtimeClasspathEntry.getClasspathEntry();

		if(runtimeClasspathEntry.getClasspathEntry().isExported()) {
			IClasspathContainer container = JavaCore.getClasspathContainer(classpathEntry.getPath(), javaProject);
			if(container != null) {
				IRuntimeClasspathEntry[] result = resolveRuntimeClasspathEntries(container);
				return result;
			}
		}
		else {
			String launchConfigProjectName = launchConfiguration.getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", (String)null);

			if(launchConfigProjectName != null && javaProject.getElementName().equals(launchConfigProjectName)) {
				IClasspathContainer container = JavaCore.getClasspathContainer(classpathEntry.getPath(), javaProject);
				if(container != null) {
					IRuntimeClasspathEntry[] result = resolveRuntimeClasspathEntries(container);
					return result;
				}

			}
		}
		return new IRuntimeClasspathEntry[0];
	}

	private IRuntimeClasspathEntry[] resolveRuntimeClasspathEntries(IClasspathContainer container) {
		IClasspathEntry[] resolvedClasspathEntries;

		if(container instanceof DependenciesClasspathContainer) {
			resolvedClasspathEntries = ((DependenciesClasspathContainer)container).getClasspathEntries(true);
		}
		else {
			resolvedClasspathEntries = container.getClasspathEntries();
		}

		List<IRuntimeClasspathEntry> result = new ArrayList<IRuntimeClasspathEntry>();

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		for(int i = 0; i < resolvedClasspathEntries.length; i++) {
			IClasspathEntry resolvedClasspathEntry = resolvedClasspathEntries[i];
			int entryKind = resolvedClasspathEntry.getEntryKind();
			IPath classPathEntryPath = resolvedClasspathEntry.getPath();
			if(entryKind == IClasspathEntry.CPE_LIBRARY) {
				result.add(JavaRuntime.newArchiveRuntimeClasspathEntry(classPathEntryPath));
			}
			else if(entryKind == IClasspathEntry.CPE_PROJECT) {
				IProject associatedProject = workspaceRoot.getProject(classPathEntryPath.toString());
				IJavaProject associatedJavaProject = JavaCore.create(associatedProject);

				try {
					result.addAll(resolveRuntimeClasspathEntriesForProject(workspaceRoot, associatedJavaProject));
				}
				catch(JavaModelException e) {
					DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}

			}
		}

		return result.toArray(new IRuntimeClasspathEntry[0]);
	}

	private List<IRuntimeClasspathEntry> resolveRuntimeClasspathEntriesForProject(IWorkspaceRoot workspaceRoot, IJavaProject javaProject) throws JavaModelException {
		List<IRuntimeClasspathEntry> result = new ArrayList<IRuntimeClasspathEntry>();
		IClasspathEntry[] associatedClasspathEntries = javaProject.getResolvedClasspath(true);
		if(associatedClasspathEntries != null) {
			for(IClasspathEntry classpathEntry: associatedClasspathEntries) {
				if(classpathEntry.isExported()) {
					IPath classPathEntryPath = classpathEntry.getPath();
					int entryKind = classpathEntry.getEntryKind();

					if(entryKind == IClasspathEntry.CPE_LIBRARY) {
						result.add(JavaRuntime.newArchiveRuntimeClasspathEntry(classPathEntryPath));
					}
					else if(entryKind == IClasspathEntry.CPE_PROJECT) {
						IProject associatedProject = workspaceRoot.getProject(classPathEntryPath.toString());
						if(associatedProject != null) {
							IJavaProject associatedJavaProject = JavaCore.create(associatedProject);
							result.addAll(resolveRuntimeClasspathEntriesForProject(workspaceRoot, associatedJavaProject));
						}
					}
				}
			}
		}
		result.add(JavaRuntime.newProjectRuntimeClasspathEntry(javaProject));
		return result;
	}

	@Override
	public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry arg0, IJavaProject arg1) throws CoreException {
		return new IRuntimeClasspathEntry[0];
	}

	@Override
	public IVMInstall resolveVMInstall(IClasspathEntry arg0) throws CoreException {
		return null;
	}

}
