package com.cwctravel.eclipse.plugins.dependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class DependenciesClasspathContainer implements IClasspathContainer {
	private final IJavaProject javaProject;
	private final IPath containerPath;

	public DependenciesClasspathContainer(IPath path, IJavaProject javaProject) {
		this.javaProject = javaProject;
		this.containerPath = path;
	}

	@Override
	public IClasspathEntry[] getClasspathEntries() {
		return getClasspathEntries(false);
	}

	public IClasspathEntry[] getClasspathEntries(boolean includeRuntimeEntries) {
		List<IClasspathEntry> classPathEntries = new ArrayList<IClasspathEntry>();
		IProject project = javaProject.getProject();

		List<DependencyInfo> dependencies = null;
		String dependenciesId = containerPath.segment(1);
		if(dependenciesId != null) {
			dependencies = DependenciesStore.getInstance().getDependencies(dependenciesId);
		}

		if(dependencies == null) {
			DependenciesParser dependenciesParser = DependenciesParser.getParser(project);
			try {
				dependenciesParser.parse();
				dependencies = dependenciesParser.getDependencies();
			}
			catch(CoreException e) {
				DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				throw new RuntimeException(e.getMessage(), e);
			}
			catch(IOException e) {
				DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				throw new RuntimeException(e.getMessage(), e);
			}

		}

		try {
			if(dependencies != null) {
				Set<IPath> processedProjectPaths = new HashSet<IPath>();
				for(DependencyInfo dependencyInfo: dependencies) {
					int dependencyScope = dependencyInfo.getScope();
					if(dependencyScope != DependenciesConstants.DEPENDENCY_SCOPE_NONE && (includeRuntimeEntries || dependencyScope != DependenciesConstants.DEPENDENCY_SCOPE_RUNTIME)) {
						boolean isAssociatedProjectAvailable = false;
						IPath dependencyPath = Path.fromOSString(dependencyInfo.getPath());
						if(dependencyInfo.hasAssociatedProject()) {
							IPath associatedProjectPath = Path.fromPortableString(dependencyInfo.getAssociatedProject());
							if(!processedProjectPaths.contains(associatedProjectPath)) {
								IProject associatedProject = ResourcesPlugin.getWorkspace().getRoot().getProject(associatedProjectPath.toString());
								if(DependenciesUtil.isJavaProjectAvailable(associatedProject)) {
									IClasspathEntry classPathEntry = JavaCore.newProjectEntry(associatedProjectPath);
									classPathEntries.add(classPathEntry);
									processedProjectPaths.add(associatedProjectPath);
									isAssociatedProjectAvailable = true;
								}
							}
							else {
								isAssociatedProjectAvailable = true;
							}
						}

						if(!isAssociatedProjectAvailable) {
							IClasspathEntry classPathEntry = JavaCore.newLibraryEntry(dependencyPath, null, null);
							classPathEntries.add(classPathEntry);
						}
					};
				}
			}
		}
		catch(CoreException e) {
			DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}

		return classPathEntries.toArray(new IClasspathEntry[0]);
	}

	@Override
	public String getDescription() {
		return "Dependencies";
	}

	@Override
	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	@Override
	public IPath getPath() {
		return DependenciesConstants.DEPENDENCIES_CLASSPATH_CONTAINER_ID;
	}

}
