package com.cwctravel.eclipse.plugins.dependencies.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.cwctravel.eclipse.plugins.dependencies.DependenciesConstants;

public class ToggleNatureHandler extends AbstractHandler {

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
				toggleNature(javaProject.getProject());
			}
		}
		return null;
	}

	private void toggleNature(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			for(int i = 0; i < natures.length; ++i) {
				if(DependenciesConstants.DEPENDENCIES_NATURE_ID.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
					return;
				}
			}

			// Add the nature
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = DependenciesConstants.DEPENDENCIES_NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
		catch(CoreException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
