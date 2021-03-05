package com.windhoverlabs.studio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * The activator class controls the plug-in life cycle
 */
public class ContextManager extends AbstractUIPlugin {
	
	public static final String PLUGIN_ID = "com.windhoverlabs.ide";
	public static ContextManager PLUGIN;
	private static HashMap<IProject, String> projectPaths;
	private static HashMap<IProject, String[]> projectDefinitionPaths;
	private static ArrayList<String> jarFilePaths;
	private static HashMap<IProject, IPropertyChangeListener> projectPropertyListener;
	
	
	/**
	 * 
	 * The constructor
	 * 
	 */
	public ContextManager() {
		PLUGIN = this;

	}

	/**
	 * 
	 * Starts the AirlinerIDE plugin.
	 * 
	 * @param context
	 * 
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		initialize();
	}

	/**
	 * Stops the AirlinerIDE plugin. 
	 * 
	 * @param context
	 * 
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		PLUGIN = null;
	}

	/**
	 * 
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 * 
	 */
	public static ContextManager getDefault() {
		return PLUGIN;
	}
	
	/**
	 * 
	 * Upon plug-in start, adds listeners which will listen to a project selection in the Project Explorer view. 
	 * Listener checks whether the Airliner nature is configured and changes what features are available.
	 * 
	 */
	public static void initialize() {
		projectPaths = new HashMap<IProject, String>();
		projectDefinitionPaths = new HashMap<IProject, String[]>();
		jarFilePaths = new ArrayList<String>();
		projectPropertyListener = new HashMap<IProject, IPropertyChangeListener>();
		

		final IWorkbench workbench = PlatformUI.getWorkbench();
		
		workbench.getDisplay().asyncExec(new Runnable() {
			public void run() {
				ISelectionService ss = workbench.getActiveWorkbenchWindow().getSelectionService();
				ProjectExplorerSelectionListener listener = new ProjectExplorerSelectionListener();
				ss.addPostSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, listener);

			};
		});
	}
	
	/**
	 * 
	 * Static method that adds an Airliner property listener to an Airliner project.
	 * 
	 * @param project
	 * 
	 */
	public static void addPropertyChangeListener(IProject project) {
		IScopeContext context = new ProjectScope(project);
		IPreferenceStore preferenceStore = new ScopedPreferenceStore(context, "com.windhoverlabs.ide.airlinerNature");
		
		IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty() == PropertiesConstants.DEF_CONFIG_PATHS) {
					if (!event.getOldValue().equals(event.getNewValue())) {
						searchDefinitionConfigurationFiles(project);
					}
				}
			}
		};
		
		preferenceStore.addPropertyChangeListener(propertyChangeListener);
		projectPropertyListener.put(project, propertyChangeListener);
	}
	
	/**
	 * 
	 * Static method that removes an Airliner property listener from an Airliner project.
	 * 
	 * @param project
	 * 
	 */
	public static void removePropertyChangeListener(IProject project) {
		IScopeContext context = new ProjectScope(project);
		IPreferenceStore preferenceStore = new ScopedPreferenceStore(context, "com.windhoverlabs.ide.airlinerNature");
		
		if (projectPropertyListener.containsKey(project)) {
			IPropertyChangeListener propertyChangeListener = projectPropertyListener.get(project);
			preferenceStore.removePropertyChangeListener(propertyChangeListener);	
			projectPropertyListener.remove(project);
		}
	}
	
	/**
	 * Searches for configuration files in paths defined in the project preferences once started.
	 */
	public static void searchDefinitionConfigurationFiles(IProject project) {
		
		IScopeContext context = new ProjectScope(project);
		IPreferenceStore preferenceStore = new ScopedPreferenceStore(context, "com.windhoverlabs.ide.cfsNature");
		String paths = preferenceStore.getString(PropertiesConstants.DEF_CONFIG_PATHS);

        /* To convert '${project_loc}' to an actual path... */
		VariablesPlugin variablesPlugin = VariablesPlugin.getDefault();
		IStringVariableManager manager = variablesPlugin.getStringVariableManager();
				
		try {
			final String updatedPaths = manager.performStringSubstitution(paths);
			
			projectPaths.put(project, updatedPaths);
			ArrayList<String> defPaths = new ArrayList<String>();
			String defConfFileName = "definition.json";

			if (updatedPaths.length() > 0) {
				Job job = new Job("Load Jar Job") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						String[] pathList = updatedPaths.split(":");
						// Search through each path defined for a definition configuration file.
						for (String aPath : pathList) {
							ArrayList<File> definitionFiles = findAllFiles(aPath, defConfFileName, false);
							definitionFiles.forEach( (File aFile) -> {		
								if(isValidDefinitionFile(aFile.getAbsolutePath()) == true) {
								    defPaths.add(aFile.getAbsolutePath());
								    JsonParser jp = new JsonParser();
								    try {
								        JsonElement je = jp.parse(new InputStreamReader(new FileInputStream(aFile.getAbsoluteFile())));
								        JsonObject jo = je.getAsJsonObject();
				                		String shortName = jo.get("short_name").getAsString();
								        if (jo.has("ide")) {
								            jo = jo.get("ide").getAsJsonObject();
								            if (jo.has("plugin")) {
								                String jarName = jo.get("plugin").getAsString();
								                String jarPath = aFile.getParent() + "/" + jarName;
								                File jarFile = new File(jarPath);
								                if (jarFile.exists()) {
								                    jarFilePaths.add(jarPath);
								                }
								            }

								            if (jo.has("view")) {
									            JsonObject jsonPlugin = jo.get("view").getAsJsonObject();       
								                for (Map.Entry<String,JsonElement> viewEntry : jsonPlugin.entrySet()) {
								                	if (viewEntry.getValue().isJsonObject()) {
								                		JsonObject jsonViewEntry = viewEntry.getValue().getAsJsonObject();
								                		
								                		String dbPath = "modules." + shortName + "." + jsonViewEntry.get("dbPath").getAsString();
								                		String viewClass = jsonViewEntry.get("viewClass").getAsString();
								                		
								                		addViewClassToDBPath(project, viewClass, dbPath);
								                	}
								                }
								            }
								        }
								    } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
								        e.printStackTrace();
								    }
								}
							});
						}
	                    DynamicClassLoader.setUpNew(jarFilePaths, ContextManager.class);
						return Status.OK_STATUS;
					}
				};
				
				job.setUser(false);
				job.schedule();
			}
						
			String[] defPathArray = new String[defPaths.size()];
			defPathArray = defPaths.toArray(defPathArray);
			
			projectDefinitionPaths.put(project, defPathArray);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 
	 * Loads the view given a class name for a custom editor.
	 * 
	 * @param project
	 * @param className
	 * @param dbPath
	 * 
	 */
	public static void addViewClassToDBPath(IProject project, String className, String dbPath) {
		try {
			Hashtable viewClasses = (Hashtable) project.getSessionProperty(new QualifiedName("Airliner", "ViewClasses"));
			if(viewClasses == null) {
				viewClasses = new Hashtable(); 
			}
			viewClasses.put(dbPath, className);
			project.setSessionProperty(new QualifiedName("Airliner", "ViewClasses"), viewClasses);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * Checks whether a file is definition file
	 * 
	 * @param file
	 * @return isValidDefinition
	 * 
	 */
	public static boolean isValidDefinitionFile(String file) {
		boolean isValidDefinition = true;
		/* TODO: Add definition validation code. */

		return isValidDefinition;
	}
	
	/**
	 * 
	 * Returns a list of definition files found.
	 * 
	 * @param path
	 * @param fName
	 * @param deepRecursion
	 * 
	 * @return fileList
	 * 
	 */
    public static ArrayList<File> findAllFiles(String path, String fName, boolean deepRecursion) {
        File file = new File(path);
        ArrayList<File> fileList = new ArrayList<File>();
        
        /* Is the path given to us a directory? */
        if (file.isDirectory()) {
        	/* Search files first. */
            for (File aChild : file.listFiles()) {
            	/* Is this a file? */
            	if(aChild.isFile()) {
            		/* Yes it is.  Does this file have the same name as what we're searching for? */
                    if (aChild.getName().equals(fName)) {
                    	/* Yes it does.  Add it to the list. */
                    	fileList.add(aChild);

                    	/* If deep recursion was not selected, don't search the subdirectories.  
                    	 * Just return now.
                    	 */
                    	if(deepRecursion == false) {
                    		return fileList;
                    	}
                    	
                    	/* Deep recursion was selected, so keep searching subdirectories. 
                    	 * However, no need to continue searching through files in this 
                    	 * directory since it can only contain one with that file name.
                    	 */
                    	break;
                    }
                }
            }
            
        	/* Now search all subdirectories directories. */
            for (File aChild : file.listFiles()) {
            	if(aChild.isDirectory()) {
                    ArrayList<File> newFileList = findAllFiles(aChild.getAbsolutePath(), fName, deepRecursion);
                    if (newFileList != null) {
                    	fileList.addAll(newFileList);
                    }
                }
            }
        } else {
    		/* No.  Its a file.  Does this file have the same name as what we're searching for? */
            if (file.getName().equals(fName)) {
            	/* Yes it does.  Add it to the list. */
            	fileList.add(file);
            }
        }
        
        /* If we got this far, its because either nothing was found and we need to return a null,
         * or at least 1 thing was found and deep recursion was selected.  Either way, return null
         * if we didn't find anything.
         */
        if(fileList.isEmpty()) {
        	return null;
        } else {
        	return fileList;
        }
    }
}