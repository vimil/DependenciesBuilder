package com.cwctravel.eclipse.plugins.dependencies;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class DependenciesNature implements IProjectNature {
	private IProject project;

	@Override
	public void configure() throws CoreException {
		if(project.hasNature(DependenciesConstants.DEPENDENCIES_NATURE_ID)) {
			return;
		}

		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] oldClasspathEntries = javaProject.getRawClasspath();
		if(!hasDependenciesClasspathContainer(oldClasspathEntries)) {
			IClasspathEntry[] newClassPathEntries = new IClasspathEntry[oldClasspathEntries.length + 1];
			System.arraycopy(oldClasspathEntries, 0, newClassPathEntries, 0, oldClasspathEntries.length);
			newClassPathEntries[oldClasspathEntries.length] = JavaCore.newContainerEntry(DependenciesConstants.DEPENDENCIES_CLASSPATH_CONTAINER_ID);
			javaProject.setRawClasspath(newClassPathEntries, null);
		}
	}

	private boolean hasDependenciesClasspathContainer(IClasspathEntry[] classpathEntries) throws JavaModelException {
		for(IClasspathEntry oldClasspathEntry: classpathEntries) {
			if(DependenciesConstants.DEPENDENCIES_CLASSPATH_CONTAINER_ID.toString().equals(oldClasspathEntry.getPath().segment(0))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void deconfigure() throws CoreException {

		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] oldClasspathEntries = javaProject.getRawClasspath();
		if(hasDependenciesClasspathContainer(oldClasspathEntries)) {
			IClasspathEntry[] newClassPathEntries = new IClasspathEntry[oldClasspathEntries.length - 1];
			for(int i = 0, j = 0; i < oldClasspathEntries.length; i++) {
				if(!DependenciesConstants.DEPENDENCIES_CLASSPATH_CONTAINER_ID.toString().equals(oldClasspathEntries[i].getPath().segment(0))) {
					newClassPathEntries[j] = oldClasspathEntries[i];
					j++;
				}
			}
			javaProject.setRawClasspath(newClassPathEntries, null);
		}
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

}
