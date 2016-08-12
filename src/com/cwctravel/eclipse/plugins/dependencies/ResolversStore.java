package com.cwctravel.eclipse.plugins.dependencies;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.cwctravel.eclipse.plugins.dependencies.resolvers.IDependenciesResolver;

public class ResolversStore {
	private static List<IDependenciesResolver> _resolvers;

	public static synchronized List<IDependenciesResolver> getResolvers() {
		if(_resolvers == null) {
			try {
				IExtensionRegistry reg = Platform.getExtensionRegistry();
				IExtensionPoint ep = reg.getExtensionPoint(IDependenciesResolver.ID);
				IExtension[] extensions = ep.getExtensions();

				_resolvers = new ArrayList<IDependenciesResolver>();
				for(int i = 0; i < extensions.length; i++) {
					IExtension ext = extensions[i];
					IConfigurationElement[] ce = ext.getConfigurationElements();
					for(int j = 0; j < ce.length; j++) {
						IDependenciesResolver resolver = (IDependenciesResolver)ce[j].createExecutableExtension("class");
						_resolvers.add(resolver);
					}
				}
			}
			catch(CoreException cE) {
				DependenciesBuilderPlugin.log(IStatus.ERROR, cE.getMessage(), cE);
			}
		}
		return new ArrayList<IDependenciesResolver>(_resolvers);
	}

	public static IDependenciesResolver getResolverForProject(IProject project) {
		IDependenciesResolver resolver = null;
		IEclipsePreferences preferences = new ProjectScope(project).getNode(DependenciesConstants.DEPENDENCIES_PLUGIN_ID);
		String resolverClassName = preferences.get(DependenciesConstants.RESOLVER_CLASS_PROPERTY, null);
		if(resolverClassName != null) {
			List<IDependenciesResolver> resolvers = getResolvers();
			for(IDependenciesResolver currentResolver: resolvers) {
				if(currentResolver.getClass().getName().equals(resolverClassName)) {
					resolver = currentResolver;
					break;
				}
			}
		}

		if(resolver == null) {
			return IDependenciesResolver.NULL_RESOLVER;
		}
		return resolver;
	}
}
