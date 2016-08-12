package com.cwctravel.eclipse.plugins.dependencies;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.cwctravel.eclipse.plugins.dependencies.resolvers.IDependenciesResolver;

public class DependenciesUtil {

	public static void resetDependenciesCache(IJavaProject javaProject) {
		IProject project = javaProject.getProject();
		DependenciesParser.removeParser(project);
	}

	public static int resolveDependencies(IJavaProject javaProject, IProgressMonitor progressMonitor) throws IOException {
		int status = IDependenciesResolver.RESOLVE_STATUS_NO_CHANGE;

		IDependenciesResolver resolver = ResolversStore.getResolverForProject(javaProject.getProject());
		if(resolver != null) {
			IProject project = javaProject.getProject();

			List<DependencyInfo> dependencies = null;
			DependenciesParser dependenciesParser = DependenciesParser.getParser(project);
			try {
				dependenciesParser.parse();
				dependencies = dependenciesParser.getDependencies();

				if(dependencies != null) {
					resolver.preResolve();

					try {
						List<IPath> dependencyPathsToResolve = new ArrayList<IPath>();
						for(DependencyInfo dependencyInfo: dependencies) {
							String associatedProject = dependencyInfo.getAssociatedProject();
							if(!DependenciesUtil.isJavaProjectAvailable(associatedProject)) {
								dependencyPathsToResolve.add(Path.fromOSString(dependencyInfo.getPath()));
							}
						}
						status = resolver.resolve(dependencyPathsToResolve, progressMonitor);
					}
					finally {
						resolver.postResolve();
					}
				}
			}
			catch(CoreException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return status;
	}

	public static boolean hasDependenciesNature(IProject project) {
		try {
			if(project != null) {
				IProjectDescription description = project.getDescription();
				String[] natures = description.getNatureIds();
				for(int i = 0; i < natures.length; ++i) {
					if(DependenciesConstants.DEPENDENCIES_NATURE_ID.equals(natures[i])) {
						return true;
					}
				}
			}
			return false;
		}
		catch(CoreException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static IProject getSelectedProject(ISelection selection) {
		IProject project = null;
		if(selection instanceof IStructuredSelection) {
			for(Iterator<?> it = ((IStructuredSelection)selection).iterator(); it.hasNext() && project == null;) {
				Object element = it.next();

				if(element instanceof IProject) {
					project = (IProject)element;
				}
				else if(element instanceof IAdaptable) {
					project = (IProject)((IAdaptable)element).getAdapter(IProject.class);
				}
			}
		}
		return project;
	}

	public static List<IJavaProject> getJavaProjectsInWorkspace() throws CoreException {
		List<IJavaProject> result = new ArrayList<IJavaProject>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for(IProject project: projects) {
			if(DependenciesUtil.isJavaProjectAvailable(project)) {
				result.add(JavaCore.create(project));
			}
		}
		return result;
	}

	public static boolean isJavaProjectAvailable(String projectName) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		return isJavaProjectAvailable(project);
	}

	public static boolean isJavaProjectAvailable(IProject project) throws CoreException {
		return project != null && project.isOpen() && project.hasNature(JavaCore.NATURE_ID);
	}

	public static String resolvePath(final IResource project, String path) {
		return resolvePath(project, path, true);
	}

	public static String resolvePath(final IResource project, String path, boolean checkIfExists) {
		String result = null;
		if(path != null) {
			if(new File(path).exists()) {
				result = path;
			}
			else {
				IPath resolvedPath = URIUtil.toPath(project.getPathVariableManager().resolveURI(URIUtil.toURI(path)));
				result = resolvedPath.toOSString();
				if(checkIfExists && !new File(result).exists()) {
					result = null;
				}
			}
		}
		return result;
	}

	public static List<String> getClasspathEntries(List<DependencyInfo> dependencies) throws CoreException, JavaModelException {
		Set<Integer> scopes = new HashSet<Integer>();
		scopes.add(DependenciesConstants.DEPENDENCY_SCOPE_COMPILE);
		scopes.add(DependenciesConstants.DEPENDENCY_SCOPE_RUNTIME);
		scopes.add(DependenciesConstants.DEPENDENCY_SCOPE_SYSTEM);
		return getClasspathEntries(dependencies, scopes);

	}

	public static List<String> getClasspathEntries(List<DependencyInfo> dependencies, Set<Integer> scopes) throws CoreException, JavaModelException {
		List<String> classpathEntries = new ArrayList<String>();
		Set<String> classPathEntriesSet = new HashSet<String>();
		for(DependencyInfo dependencyInfo: dependencies) {
			String classPathEntryPath = null;
			int scope = dependencyInfo.getScope();

			if(scopes.contains(scope)) {
				if(dependencyInfo.hasAssociatedProject()) {
					IPath associatedProjectPath = Path.fromPortableString(dependencyInfo.getAssociatedProject());
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IProject associatedProject = root.getProject(associatedProjectPath.toPortableString());
					if(isJavaProjectAvailable(associatedProject)) {
						IJavaProject associatedJavaProject = JavaCore.create(associatedProject);
						IFolder folder = root.getFolder(associatedJavaProject.getOutputLocation());
						classPathEntryPath = folder.getLocation().toOSString();
					}
					else {
						classPathEntryPath = dependencyInfo.getPath();
					}
				}
				else {
					classPathEntryPath = dependencyInfo.getPath();
				}

				if(!classPathEntriesSet.contains(classPathEntryPath)) {
					classpathEntries.add(classPathEntryPath);
					classPathEntriesSet.add(classPathEntryPath);
				}
			}
		}
		return classpathEntries;
	}

	public static List<DependencyInfo> copyOf(List<DependencyInfo> dependencies) {
		List<DependencyInfo> result = null;
		if(dependencies != null) {
			result = new ArrayList<DependencyInfo>();
			for(DependencyInfo dependencyInfo: dependencies) {
				result.add(new DependencyInfo(dependencyInfo.getRawPath(), dependencyInfo.getPath(), dependencyInfo.getAssociatedProject(), dependencyInfo.getScope()));
			}
		}
		return result;
	}

}
