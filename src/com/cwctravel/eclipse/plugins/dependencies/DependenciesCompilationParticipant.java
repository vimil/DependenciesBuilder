package com.cwctravel.eclipse.plugins.dependencies;

import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;

import com.cwctravel.eclipse.plugins.dependencies.resolvers.IDependenciesResolver;

@SuppressWarnings("restriction")
public class DependenciesCompilationParticipant extends CompilationParticipant {

	@Override
	public int aboutToBuild(IJavaProject javaProject) {
		int result = READY_FOR_BUILD;
		IProject project = javaProject.getProject();
		try {
			project.deleteMarkers("com.cwctravel.eclipse.plugins.dependencies.problemMarker", false, IResource.DEPTH_ONE);
			project.deleteMarkers("org.eclipse.jdt.core.buildpath_problem", false, IResource.DEPTH_ONE);

			int status = DependenciesUtil.resolveDependencies(javaProject, null);
			if(status != IDependenciesResolver.RESOLVE_STATUS_NO_CHANGE) {
				try {
					javaProject.setRawClasspath(javaProject.getRawClasspath(), null);
				}
				catch(JavaModelException e) {
					DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
				result = NEEDS_FULL_BUILD;
			}

			if(!verifyRequiredProjectsAvailable(javaProject, project)) {
				result = NEEDS_FULL_BUILD;
			}

		}
		catch(IOException iE) {
			try {
				DependenciesBuilderPlugin.log(IStatus.ERROR, iE.getMessage(), iE);

				IMarker marker = project.getFile("dependencies.xml").createMarker("com.cwctravel.eclipse.plugins.dependencies.problemMarker");
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
		return result;
	}

	private boolean verifyRequiredProjectsAvailable(IJavaProject javaProject, IProject project) throws JavaModelException, CoreException {
		PerProjectInfo perProjectInfo = JavaModelManager.getJavaModelManager().getPerProjectInfo(project, false);
		if(perProjectInfo != null) {
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			IClasspathEntry[] resolvedClasspath = perProjectInfo.getResolvedClasspath();
			boolean requiredProjectsNotAvailable = false;
			if(resolvedClasspath != null) {
				for(IClasspathEntry classpathEntry: resolvedClasspath) {
					if(classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
						IProject requiredProject = workspaceRoot.getProject(classpathEntry.getPath().toString());
						if(!DependenciesUtil.isJavaProjectAvailable(requiredProject)) {
							requiredProjectsNotAvailable = true;
							break;
						}
					}
				}
			}

			if(requiredProjectsNotAvailable) {
				perProjectInfo.resetResolvedClasspath();
				return false;
			}
		}
		return true;
	}

	@Override
	public void buildStarting(BuildContext[] files, boolean isBatch) {}

	@Override
	public boolean isActive(IJavaProject javaProject) {
		try {
			IProjectDescription description = javaProject.getProject().getDescription();
			String[] natures = description.getNatureIds();

			for(int i = 0; i < natures.length; ++i) {
				if(DependenciesConstants.DEPENDENCIES_NATURE_ID.equals(natures[i])) {
					return true;
				}
			}
		}
		catch(CoreException e) {
			DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
		}

		return false;
	}

	@Override
	public void cleanStarting(IJavaProject project) {
		DependenciesUtil.resetDependenciesCache(project);
	}

}
