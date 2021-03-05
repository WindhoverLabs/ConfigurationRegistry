 package com.myplugin.rmp;
  import org.eclipse.core.resources.IResource;
  import org.eclipse.core.runtime.CoreException;
  import org.eclipse.core.runtime.QualifiedName;
  import org.eclipse.swt.SWT;
  import org.eclipse.swt.layout.GridData;
  import org.eclipse.swt.layout.GridLayout;
  import org.eclipse.swt.widgets.Composite;
  import org.eclipse.swt.widgets.Control;
  import org.eclipse.swt.widgets.Label;
  import org.eclipse.swt.widgets.Text;
  import org.eclipse.ui.IWorkbenchPropertyPage;
  import org.eclipse.ui.dialogs.PropertyPage;
  import com.myplugin.rmp.views.PropertyManagerView.TreeObject;
 
  import java.io.File;
  import java.util.ArrayList;
  import java.util.StringTokenizer;

  import org.eclipse.core.resources.IProject;
  import org.eclipse.core.resources.ProjectScope;
  import org.eclipse.core.runtime.IAdaptable;
  import org.eclipse.core.runtime.preferences.IScopeContext;
  import org.eclipse.jface.preference.IPreferenceStore;
  import org.eclipse.jface.preference.PathEditor;
  import org.eclipse.swt.SWT;
  import org.eclipse.swt.layout.GridData;
  import org.eclipse.swt.widgets.Composite;
  import org.eclipse.swt.widgets.Control;
  import org.eclipse.ui.IWorkbenchPropertyPage;
  import org.eclipse.ui.dialogs.PropertyPage;
  import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.swt.widgets.Button;

  /**
   * 
   * GUI Interface for Airliner section in project properties.
   * 
   * @author vagrant
   *
   */
  public class CFSPropertiesPage extends PropertyPage implements IWorkbenchPropertyPage {
  		
  	private PathEditor pathChooser;
  	private IProject currentProject;
  	private IPreferenceStore preferenceStore;
  	private IScopeContext context;
  	private Composite pathEditorHolder;
  	
  	public CFSPropertiesPage() {
  		
  	}
  	/**
      * 
      * Main function executes to create and return the GUI interface of the Airliner property page.
      * 
      * @param parent
      * @return Control
      * 
      */
  	@Override
  	protected Control createContents(Composite parent) {
  		prepareProperties();
  		prepareHolder(parent);
  		createPathChooser();
  	
  		return getControl();
  	}
  	
  	
  	/**
      * 
      * Finds the project properties in the preference store and links it to the GUI interfaces on the Airliner property page.
      * 
      */
  	private void prepareProperties() {
  		// Retrieve the currently selected project.
  		IAdaptable adaptable = getElement();
  		currentProject = (IProject) adaptable.getAdapter(IProject.class);
          context = new ProjectScope(currentProject);
          // Retrieve the current project's preference store, and associate the property page with it.
          preferenceStore = new ScopedPreferenceStore(context, "com.windhoverlabs.ide.cfsNature");
          setPreferenceStore(preferenceStore);
  	}
  	
  	/**
      * 
      * Creates the parent composite to hold the path editor composite.
      * 
      */
  	private void prepareHolder(Composite parent) {
  		// Create the composite which will hold the path chooser.
  		pathEditorHolder = new Composite(parent, SWT.NONE);
  		GridData gridData = new GridData();
  		gridData.horizontalAlignment = GridData.FILL;
  		gridData.grabExcessHorizontalSpace = true;
  		pathEditorHolder.setLayoutData(gridData);
  		pathEditorHolder.setLayout(new GridLayout(1, false));
  	}
  	
  	/**
      * 
      * Creates the path editor composite and sets the contents.
      * 
      */
  	private void createPathChooser() {
  		// Retrieve the current properties from the associated preference store.
  		// Retrieve the preferenceStore string representation of path, and convert it into an array of paths.
  		String defaultPaths = getDefaultPaths();
  		preferenceStore.setDefault(PropertiesConstants.DEF_CONFIG_PATHS, defaultPaths);
  		String paths = preferenceStore.getString(PropertiesConstants.DEF_CONFIG_PATHS);
  		String[] pathList = parseString(paths);
  		// Create the path chooser, and assign it with the preference variable 'path', set it to the associated preference store.
  		pathChooser = new PathEditor(PropertiesConstants.DEF_CONFIG_PATHS, "Path to YAML file", "Choose", pathEditorHolder);
  		pathChooser.setPreferenceStore(preferenceStore);
  		// Populate the list with current project's path property. If it hasn't been set yet, then set it to the default.
  		pathChooser.getListControl(pathEditorHolder).setItems(pathList);
  	}
  	
  	/**
      * 
      * Loads the default of the Airliner property page. For the paths editor it is ${project_loc}/apps
      * 
      */
  	protected void performDefaults() {
  		pathChooser.loadDefault();
  		super.performDefaults();
  	}
  	
  	/**
      * 
      * Saves the input data.
      * 
      * @return performedStatus
      * 
      */
  	public boolean performOk() {
  		String[] items = pathChooser.getListControl(pathEditorHolder).getItems();
  		String oneList = createList(items);
  		preferenceStore.setValue(PropertiesConstants.DEF_CONFIG_PATHS, oneList);
  		ContextManager.searchDefinitionConfigurationFiles(currentProject);
  		return true;
  	}
  	
  	/**
      * 
      * Helper function to convert an array of paths to a single string to store in the preference store.
      * 
      * @param items
      * @return paths
      * 
      */
  	private String createList(String[] items) {
  		StringBuilder path = new StringBuilder("");

  		for (String item : items) {
  			path.append(item);
  			path.append(File.pathSeparator);
  		}
  		return path.toString();
  	}
  	
  	/**
      * 
      * Helper function to convert a single string of paths to an array of paths to be used as input for the path editor chooser.
      * 
      * @param stringList
      * @return pathsAsList
      * 
      */
  	private String[] parseString(String stringList) {
  		StringTokenizer st = new StringTokenizer(stringList, File.pathSeparator + "\n\r");
  		ArrayList<Object> v = new ArrayList<>();
  		while (st.hasMoreElements()) {
  			v.add(st.nextElement());
  		}
  		return v.toArray(new String[v.size()]);
  	}
  	
  	/**
      * 
      * Returns the default path of the configuration file. This could be YAML, XML, SQLite, etc.
      * 
      * @return defaultPath
      * 
      */
  	private String getDefaultPaths() {
  		String defaultPath = "${project_loc}/Resources/definitions.yaml";
  		
  		return defaultPath;
  	}
  }
