package com.myplugin.rmp;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;

/**
 * 
 * Listener that will check whether a project selection has the Airliner nature.
 * 
 * @author vagrant
 *
 */
public class ProjectExplorerSelectionListener implements ISelectionListener {

	/**
	 * 
	 * Sets the eclipse activity for the AirlinerIDE plugin if successful, else removes it.
	 * 
	 * @param workbenchPart
	 * @param newSelection 
	 * 
	 */
	public void selectionChanged(IWorkbenchPart workbenchPart, ISelection newSelection) {
		if (newSelection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) newSelection).getFirstElement();
			if (element instanceof IAdaptable) {
				if (element instanceof IResource) {
					IResource resource = (IResource) ((IAdaptable) element).getAdapter(IResource.class);
					IProject project = resource.getProject();
					
					if (project.exists() && project.isOpen()) {
						IWorkbenchActivitySupport workbenchAS = PlatformUI.getWorkbench().getActivitySupport();
						IActivityManager activityManager = workbenchAS.getActivityManager();
						
						Set<String> enabledActivities = new HashSet<String>(activityManager.getEnabledActivityIds());
						for (String s : enabledActivities) {
							System.out.println("enabledActivities : " + s);
	
						}
						try {
							if (project.getNature("com.windhoverlabs.ide.airlinerNature") != null) {
								if (!enabledActivities.contains("com.windhoverlabs.ide.airlinerActivity")) {
									if (enabledActivities.add("com.windhoverlabs.ide.airlinerActivity")) {
										workbenchAS.setEnabledActivityIds(enabledActivities);
									}
								}
							} else {
								if (enabledActivities.contains("com.windhoverlabs.ide.airlinerActivity")) {
									if (enabledActivities.remove("com.windhoverlabs.ide.airlinerActivity")) {
										workbenchAS.setEnabledActivityIds(enabledActivities);
									}
								}
							}
							
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
