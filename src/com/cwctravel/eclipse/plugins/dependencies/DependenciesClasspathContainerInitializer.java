package com.cwctravel.eclipse.plugins.dependencies;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class DependenciesClasspathContainerInitializer extends ClasspathContainerInitializer {

	@Override
	public void initialize(IPath path, IJavaProject javaProject) throws CoreException {
		IClasspathContainer container = JavaCore.getClasspathContainer(path, javaProject);
		if(!(container instanceof DependenciesClasspathContainer)) {
			container = new DependenciesClasspathContainer(path, javaProject);
			JavaCore.setClasspathContainer(path, new IJavaProject[] {javaProject}, new IClasspathContainer[] {container}, null);
			javaProject.getProject().getWorkspace().addResourceChangeListener(DependenciesStore.getInstance());
		}
		JavaCore.addElementChangedListener(DependenciesStore.getInstance());
	}

	@Override
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		return true;
	}

	@Override
	public IClasspathContainer getFailureContainer(IPath containerPath, IJavaProject project) {
		return super.getFailureContainer(containerPath, project);
	}

	@Override
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		return containerPath.uptoSegment(1).append(project.getElementName());
	}
}
