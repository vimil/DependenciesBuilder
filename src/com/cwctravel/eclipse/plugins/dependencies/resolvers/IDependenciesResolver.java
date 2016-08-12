package com.cwctravel.eclipse.plugins.dependencies.resolvers;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IDependenciesResolver {
	public static final String ID = "com.cwctravel.eclipse.plugins.dependencies.dependencyResolvers";

	public static final int RESOLVE_STATUS_NO_CHANGE = 0;
	public static final int RESOLVE_STATUS_RESOLVED = 1;
	public static final int RESOLVE_STATUS_UNRESOLVED = 2;
	public static final IDependenciesResolver NULL_RESOLVER = new IDependenciesResolver() {

		@Override
		public int resolve(List<IPath> dependencies, IProgressMonitor progressMonitor) throws IOException {
			return RESOLVE_STATUS_NO_CHANGE;
		}

		@Override
		public String getName() {
			return "<none>";
		}

		@Override
		public void preResolve() {

		}

		@Override
		public void postResolve() {

		}

	};

	public void preResolve();

	public int resolve(List<IPath> dependencies, IProgressMonitor progressMonitor) throws IOException;

	public void postResolve();

	public String getName();
}
