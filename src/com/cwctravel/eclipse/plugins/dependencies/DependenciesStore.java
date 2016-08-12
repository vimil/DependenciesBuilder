package com.cwctravel.eclipse.plugins.dependencies;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.service.prefs.BackingStoreException;

import com.cwctravel.eclipse.plugins.dependencies.resolvers.IDependenciesResolver;

public class DependenciesStore implements IElementChangedListener, IResourceChangeListener {
	private static DependenciesStore _instance;
	private static Map<String, List<DependencyInfo>> dependenciesMap;
	private static Map<String, IDependenciesResolver> resolverMap;

	private static class UpdateDependenciesFileJob extends WorkspaceJob {
		private final IProject project;
		private final List<DependencyInfo> dependencies;
		private final IDependenciesResolver resolver;

		public UpdateDependenciesFileJob(IProject project, IDependenciesResolver resolver, List<DependencyInfo> dependencies) {
			super("Updating dependencies...");
			this.project = project;
			this.dependencies = dependencies;
			this.resolver = resolver;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			storeDependencies(project, resolver, dependencies);
			return Status.OK_STATUS;
		}

		private void storeDependencies(IProject project, IDependenciesResolver resolver, List<DependencyInfo> dependencies) throws CoreException {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("<dependencies>");
			if(dependencies != null) {
				for(DependencyInfo dependencyInfo: dependencies) {
					stringBuilder.append("\n\t<dependency path=\"" + dependencyInfo.getRawPath() + "\" " + "scope=\"" + DependencyInfo.getScopeLabel(dependencyInfo.getScope()) + "\"/>");
				}
			}
			stringBuilder.append("\n</dependencies>");

			ByteArrayInputStream bAIS = new ByteArrayInputStream(stringBuilder.toString().getBytes());
			IFile dependenciesFile = project.getFile(Path.fromPortableString("dependencies.xml"));
			if(!dependenciesFile.exists()) {
				dependenciesFile.create(bAIS, true, null);
			}
			else {
				project.getWorkspace().validateEdit(new IFile[] {dependenciesFile}, null);
				dependenciesFile.setContents(bAIS, false, true, null);
			}

			if(resolver != null) {
				try {
					IEclipsePreferences preferences = new ProjectScope(project).getNode(DependenciesConstants.DEPENDENCIES_PLUGIN_ID);
					preferences.put(DependenciesConstants.RESOLVER_CLASS_PROPERTY, resolver.getClass().getName());

					String[] propertyNames = preferences.keys();
					for(String propertyName: propertyNames) {
						if(propertyName.startsWith(DependenciesConstants.DEPENDENCY_ASSOCIATED_PROJECT_PROPERTY_PREFIX)) {
							preferences.remove(propertyName);
						}
					}

					if(dependencies != null) {
						for(DependencyInfo dependencyInfo: dependencies) {
							if(dependencyInfo.hasAssociatedProject()) {
								preferences.put(DependenciesConstants.DEPENDENCY_ASSOCIATED_PROJECT_PROPERTY_PREFIX + dependencyInfo.getRawPath(), dependencyInfo.getAssociatedProject());
							}
						}
					}
					preferences.flush();
				}
				catch(BackingStoreException e) {
					DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
				}
			}
		}
	}

	public static class RefreshClassPathJob extends WorkspaceJob {
		private final IJavaProject javaProject;

		public RefreshClassPathJob(IJavaProject javaProject) {
			super("Refreshing classpath...");
			this.javaProject = javaProject;
		}

		@Override
		public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
			refreshClasspath(javaProject);
			return Status.OK_STATUS;
		}

		private void refreshClasspath(IJavaProject javaProject) {
			try {
				if(DependenciesUtil.isJavaProjectAvailable(javaProject.getProject())) {
					javaProject.setRawClasspath(javaProject.getRawClasspath(), null);
					javaProject.getResolvedClasspath(true);
				}
			}
			catch(CoreException e) {
				DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
			}

		}
	}

	private DependenciesStore() {
		dependenciesMap = Collections.synchronizedMap(new HashMap<String, List<DependencyInfo>>());
		resolverMap = Collections.synchronizedMap(new HashMap<String, IDependenciesResolver>());
	}

	@Override
	public void elementChanged(ElementChangedEvent event) {
		String containerId = DependenciesConstants.DEPENDENCIES_CLASSPATH_CONTAINER_ID.toString();

		Object source = event.getSource();
		if(source instanceof IJavaElementDelta) {
			IJavaElementDelta javaElementDelta = (IJavaElementDelta)source;
			IJavaElementDelta[] affectedChildren = javaElementDelta.getAffectedChildren();
			for(IJavaElementDelta affectedChild: affectedChildren) {
				if((affectedChild.getFlags() & IJavaElementDelta.F_CLASSPATH_CHANGED) != 0) {
					IJavaElement javaElement = affectedChild.getElement();
					if(javaElement instanceof IJavaProject) {
						try {
							IJavaProject javaProject = (IJavaProject)javaElement;
							IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
							for(IClasspathEntry classpathEntry: classpathEntries) {
								if(containerId.equals(classpathEntry.getPath().segment(0))) {
									String dependenciesId = classpathEntry.getPath().segment(1);
									if(dependenciesId != null) {
										List<DependencyInfo> dependencies = dependenciesMap.get(dependenciesId);
										if(dependencies != null) {
											IDependenciesResolver resolver = resolverMap.get(dependenciesId);
											IProject project = javaProject.getProject();
											new UpdateDependenciesFileJob(project, resolver, dependencies).schedule();
										}
										dependenciesMap.remove(dependenciesId);
									}
									break;
								}
							}
						}
						catch(JavaModelException e) {
							DependenciesBuilderPlugin.log(IStatus.ERROR, e.getMessage(), e);
						}
					}
				}
			}
		}
	}

	public void setDependencies(String dependenciesId, List<DependencyInfo> dependencies) {
		dependenciesMap.put(dependenciesId, dependencies);
	}

	public List<DependencyInfo> getDependencies(String dependenciesId) {
		List<DependencyInfo> result = null;
		if(dependenciesId != null) {
			List<DependencyInfo> dependencies = dependenciesMap.get(dependenciesId);
			if(dependencies != null) {
				result = new ArrayList<DependencyInfo>();
				for(DependencyInfo dependencyInfo: dependencies) {
					result.add((DependencyInfo)dependencyInfo.clone());
				}
			}
		}

		return result;
	}

	public IDependenciesResolver getResolver(String dependenciesId) {
		return resolverMap.get(dependenciesId);
	}

	public static synchronized DependenciesStore getInstance() {
		if(_instance == null) {
			_instance = new DependenciesStore();
		}
		return _instance;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta resourceDelta = event.getDelta();
		if(resourceDelta != null) {
			IResourceDelta[] affectedChildren = resourceDelta.getAffectedChildren();
			for(IResourceDelta affectedChild: affectedChildren) {
				IResource resource = affectedChild.getResource();
				if(resource instanceof IProject) {
					IProject project = (IProject)resource;
					IResourceDelta dependenciesDelta = affectedChild.findMember(Path.fromPortableString("dependencies.xml"));
					if(dependenciesDelta != null) {
						IJavaProject javaProject = JavaCore.create(project);
						if(javaProject != null) {
							new RefreshClassPathJob(javaProject).schedule();
						}
					}
				}
			}
		}
	}

	public void setResolver(String dependenciesId, IDependenciesResolver resolver) {
		resolverMap.put(dependenciesId, resolver);

	}
}
