package com.windhoverlabs.studio;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * 
 * Loads classes given a string at runtime.
 * 
 * @author vagrant
 *
 */
public class DynamicClassLoader {
	private static ClassLoader classLoader;
	
	/**
	 * Initiates the class loader and loads the jar classes.
	 * @param classPaths
	 * @return
	 */
	public static boolean setUpNew(ArrayList<String> classPaths, Class<?> c) {
		if (classPaths.size() > 0) {
			try {
				// Iterate through our list of jar names to create an array of URL representation of our jars.
				URL[] jarFiles = new URL[classPaths.size()];
				for (int i = 0; i < classPaths.size(); i++) {
					String jarName = String.valueOf(classPaths.get(i));
					URL jarAsURL = new URL("file://" + jarName);
					jarFiles[i] = jarAsURL;
				} 
				// Using the base default Composite class loader, create a new instance with our array of URLs added.
				classLoader = URLClassLoader.newInstance(jarFiles, c.getClassLoader());
				
				return true;
			}catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			} 
		} else {
			return false;
		}
	}
	
	/**
	 * Will update the classes. Two implementations. Delete the old monolithic classloader and recreate a new one.
	 * Retrieve the necessary ones, close them and remove them.
	 * @param updatedClassPaths
	 */
//	public static void updateClasses(ArrayList<String> updatedClassPaths, Class<?> c) {
//		// Delete and recreate.
//		try {
//			classLoader.close();
//			setUpNew(updatedClassPaths, c);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	/**
	 * Function will check if the class is in the jars that have been loaded.
	 * @param className
	 * @return
	 */
	public static boolean verifyClassExistence(String className) {	
		try {
			Class.forName(className, false, classLoader);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	/**
	 * Retrieves the Class from the loaded jars. The function that calls this should first check if it contains the class.
	 * @param currentNamedObject
	 * @param className
	 * @param parent
	 * @return
	 */
	public static Composite createCustomClass(NamedObject currentNamedObject, String className, Composite parent, ConfigDB cfsConfig){
		Composite returned = null;
		try {
			Class<?> theComposite  = Class.forName(className, true, classLoader);
			Class<? extends Composite> compositeClass = theComposite.asSubclass(Composite.class);
			Constructor<? extends Composite> constructor = compositeClass.getConstructor(Composite.class, int.class, ConfigDB.class);
			returned = constructor.newInstance(parent, SWT.FILL, cfsConfig);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} 
		return returned;
	}
}