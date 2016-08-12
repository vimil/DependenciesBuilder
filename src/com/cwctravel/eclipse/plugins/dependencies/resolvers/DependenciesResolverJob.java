package com.cwctravel.eclipse.plugins.dependencies.resolvers;

import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import com.cwctravel.eclipse.plugins.dependencies.DependenciesBuilderPlugin;
import com.cwctravel.eclipse.plugins.dependencies.DependenciesUtil;

public class DependenciesResolverJob extends Job {
	private final IJavaProject javaProject;

	public DependenciesResolverJob(IJavaProject javaProject) {
		super("Refresh Dependencies");
		this.javaProject = javaProject;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if(javaProject != null) {
			try {
				javaProject.getProject().deleteMarkers("com.cwctravel.eclipse.plugins.dependencies.problemMarker", false, IResource.DEPTH_ONE);
				javaProject.getProject().deleteMarkers("org.eclipse.jdt.core.buildpath_problem", false, IResource.DEPTH_ONE);

				int status = DependenciesUtil.resolveDependencies(javaProject, monitor);
				if(status != IDependenciesResolver.RESOLVE_STATUS_NO_CHANGE) {
					javaProject.setRawClasspath(javaProject.getRawClasspath(), monitor);
				}
			}
			catch(JavaModelException e) {
				DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
			catch(IOException iE) {
				try {
					DependenciesBuilderPlugin.log(IStatus.ERROR, iE.getMessage(), iE);

					IMarker marker = javaProject.getProject().getFile("dependencies.xml").createMarker("com.cwctravel.eclipse.plugins.dependencies.problemMarker");
					marker.setAttribute(IMarker.MESSAGE, iE.getMessage());
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
				}
				catch(CoreException e) {
					DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}
			catch(CoreException e) {
				DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}
		}
		return Status.OK_STATUS;
	}

}
