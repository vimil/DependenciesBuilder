package com.cwctravel.eclipse.plugins.dependencies;

public class DependencyInfo {
	private String rawPath;
	private String path;
	private String associatedProject;

	private int scope;

	public DependencyInfo(String rawPath, String path, String associatedProject, int scope) {
		this.rawPath = rawPath;
		this.path = path;
		this.scope = scope;
		this.associatedProject = associatedProject;
	}

	public String getRawPath() {
		return rawPath;
	}

	public String getPath() {
		return path;
	}

	public String getAssociatedProject() {
		return associatedProject;
	}

	public void setAssociatedProject(String associatedProject) {
		this.associatedProject = associatedProject;
	}

	public int getScope() {
		return scope;
	}

	public void setScope(int scope) {
		this.scope = scope;
	}

	public static String getScopeLabel(int scope) {
		switch(scope) {
			case DependenciesConstants.DEPENDENCY_SCOPE_PROVIDED:
				return DependenciesConstants.DEPENDENCY_SCOPE_LABEL_PROVIDED;
			case DependenciesConstants.DEPENDENCY_SCOPE_COMPILE:
				return DependenciesConstants.DEPENDENCY_SCOPE_LABEL_COMPILE;
			case DependenciesConstants.DEPENDENCY_SCOPE_TEST:
				return DependenciesConstants.DEPENDENCY_SCOPE_LABEL_TEST;
			case DependenciesConstants.DEPENDENCY_SCOPE_RUNTIME:
				return DependenciesConstants.DEPENDENCY_SCOPE_LABEL_RUNTIME;
			case DependenciesConstants.DEPENDENCY_SCOPE_SYSTEM:
				return DependenciesConstants.DEPENDENCY_SCOPE_LABEL_SYSTEM;
			default:
				return DependenciesConstants.DEPENDENCY_SCOPE_LABEL_NONE;
		}
	}

	public static int getScope(String scopeLabel) {
		if(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_PROVIDED.equals(scopeLabel)) {
			return DependenciesConstants.DEPENDENCY_SCOPE_PROVIDED;
		}
		else if(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_COMPILE.equals(scopeLabel)) {
			return DependenciesConstants.DEPENDENCY_SCOPE_COMPILE;
		}
		else if(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_TEST.equals(scopeLabel)) {
			return DependenciesConstants.DEPENDENCY_SCOPE_TEST;
		}
		else if(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_RUNTIME.equals(scopeLabel)) {
			return DependenciesConstants.DEPENDENCY_SCOPE_RUNTIME;
		}
		else if(DependenciesConstants.DEPENDENCY_SCOPE_LABEL_SYSTEM.equals(scopeLabel)) {
			return DependenciesConstants.DEPENDENCY_SCOPE_SYSTEM;
		}
		else {
			return DependenciesConstants.DEPENDENCY_SCOPE_NONE;
		}
	}

	@Override
	public Object clone() {
		return new DependencyInfo(rawPath, path, associatedProject, scope);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof DependencyInfo) {
			DependencyInfo that = (DependencyInfo)obj;
			return rawPath == that.rawPath || (rawPath != null && rawPath.equals(that.rawPath));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return rawPath == null ? 0 : rawPath.hashCode();
	}

	public boolean hasAssociatedProject() {
		return associatedProject != null && !DependenciesConstants.ASSOCIATED_PROJECT_NONE.equals(associatedProject);
	}

	public void setPath(String rawPath, String resolvedPath) {
		this.rawPath = rawPath;
		this.path = resolvedPath;
	}
}