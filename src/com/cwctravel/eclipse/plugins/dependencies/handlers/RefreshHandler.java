package com.cwctravel.eclipse.plugins.dependencies.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.cwctravel.eclipse.plugins.dependencies.DependenciesUtil;
import com.cwctravel.eclipse.plugins.dependencies.resolvers.DependenciesResolverJob;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class RefreshHandler extends AbstractHandler {
	public RefreshHandler() {}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = HandlerUtil.getActiveMenuSelection(event);
		IStructuredSelection selection = (IStructuredSelection)sel;

		Iterator<?> it = selection.iterator();
		if(it.hasNext()) {
			Object element = it.next();
			IJavaProject javaProject = null;
			if(element instanceof IJavaProject) {
				javaProject = (IJavaProject)element;
			}

			if(javaProject != null) {
				refreshDependencies(javaProject);
			}
		}

		return null;
	}

	private void refreshDependencies(IJavaProject javaProject) {
		DependenciesResolverJob dependenciesResolverJob = new DependenciesResolverJob(javaProject);
		dependenciesResolverJob.setUser(true);
		dependenciesResolverJob.schedule();
	}

	@Override
	public boolean isEnabled() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if(workbench != null) {
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			ISelection selection = window.getSelectionService().getSelection();
			return DependenciesUtil.hasDependenciesNature(DependenciesUtil.getSelectedProject(selection));
		}
		return false;
	}

}
