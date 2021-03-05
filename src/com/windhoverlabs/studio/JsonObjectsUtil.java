package com.myplugin.rmp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * 
 * Static class which contains methods to assist in handling and merging json config files.
 * 
 * @author vagrant
 *
 */
public class JsonObjectsUtil {
	
	public static enum ConflictStrategy {
		THROW_EXCEPTION, PREFER_FIRST_OBJECT, PREFER_SECOND_OBJECT, PREFER_NON_NULL;
	}
	
	
	@SuppressWarnings("serial")
	public static class JsonObjectExtensionConflictExeception extends Exception {
		public JsonObjectExtensionConflictExeception(String message) {
			super(message);
		}
	}
	
	static Gson gson = new Gson();
	static JsonParser jp = new JsonParser();
	
	/**
	 * 
	 * Runner function that creates the ConfigDB registry for the specified config.json file.
	 * 
	 * @param pathToConfig
	 * @returncfsConfig
	 * 
	 */
	public static ConfigDB goMerge(File pathToConfig) {
		//Load file into reader.
		Reader rd = null;
		try {
			rd = new FileReader(pathToConfig);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		ConfigDB cfsConfig = new ConfigDB();
		
		//Parse into jsonelement
		JsonElement localConfigElm = jp.parse(rd);
		JsonObject localConfigObj = localConfigElm.getAsJsonObject();
		
		cfsConfig.setLocal(deepClone(localConfigElm));
		
		//Obtain jsonObject for 'core'
		JsonElement coreConfigElm = localConfigObj.get("core");
		JsonObject coreConfigObj = coreConfigElm.getAsJsonObject();
		
		// Iterate through the core components
		for (Map.Entry<String, JsonElement> entry : coreConfigObj.entrySet()) {
			// Retrieve current module as json object
			JsonElement currentModuleElm = coreConfigObj.get(entry.getKey());
			JsonObject currentModuleObj = currentModuleElm.getAsJsonObject();
			
			// Get the definition base path
			String spathtoconfig = pathToConfig.getParent().toString().concat("/");
			String basePath = currentModuleObj.get("definition").getAsString();
			String concat = spathtoconfig.concat(basePath);
			// Open the definition file for this module
			Path path = Paths.get(concat);
			File defFile = new File(path.toString());
			// Load into JsonElement
			try {
				JsonElement defEle = jp.parse(new FileReader(defFile));
				// Deep Merge this definition object into the 'module/<name>/' object of the local config.
				merge(defEle, currentModuleElm);
			} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		//Obtain jsonObject for 'modules'
		JsonElement moduleConfigElm = localConfigObj.get("modules");
		JsonObject moduleConfigObj = moduleConfigElm.getAsJsonObject();
		
		// Iterate through the modules
		for (Map.Entry<String, JsonElement> entry : moduleConfigObj.entrySet()) {
			// Retrieve current module as json object
			JsonElement currentModuleElm = moduleConfigObj.get(entry.getKey());
			JsonObject currentModuleObj = currentModuleElm.getAsJsonObject();
			
			// Get the definition base path
			String spathtoconfig = pathToConfig.getParent().toString().concat("/");
			String basePath = currentModuleObj.get("definition").getAsString();
			String concat = spathtoconfig.concat(basePath);
			// Open the definition file for this module
			Path path = Paths.get(concat);
			File defFile = new File(path.toString());
			// Load into JsonElement
			try {
				JsonElement defEle = jp.parse(new FileReader(defFile));
				// Deep Merge this definition object into the 'module/<name>/' object of the local config.
				merge(defEle, currentModuleElm);
			} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		// Get the base directory, i.e. "../../../.."
		String p1 = pathToConfig.toPath().getParent().toString().concat("/");
		String configBase = localConfigObj.get("config_base").getAsString();
		String p2 = p1.concat(configBase);
		String basePath = Paths.get(p2).normalize().toString();
		String pathLocalConfig = pathToConfig.toPath().getParent().toString();
		 
		//JsonElement baseElement = getInherittedConfig(basePath, configBase, pathLocalConfig);
		//localConfigElm = getInherittedConfig(localConfigElm, basePath, pathLocalConfig);
		JsonElement baseElement = getBaseConfig(localConfigElm, basePath, pathLocalConfig);
		
		cfsConfig.setBase(baseElement);
		cfsConfig.createCombined();
		
		return cfsConfig;
	}
	
	/**
	 * 
	 * Creates a replicate of a Json Element.
	 * 
	 * @param el
	 * @return jsonEntity
	 */
	public static JsonElement deepClone(JsonElement el){
	    if (el.isJsonPrimitive() || el.isJsonNull())
	        return el;
	    if (el.isJsonArray()) {
	        JsonArray array = new JsonArray();
	        for(JsonElement arrayEl: el.getAsJsonArray())
	            array.add(deepClone(arrayEl));
	        return array;
	    }
	    if(el.isJsonObject()) {
	        JsonObject obj = new JsonObject();
	        for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
	            obj.add(entry.getKey(), deepClone(entry.getValue()));
	        }
	        return obj;
	    }
	    throw new IllegalArgumentException("JsonElement type " + el.getClass().getName());
	}
	
	/**
	 * 
	 * Merges the parent config with the current config recursively.
	 * 
	 * @param mergedConfig
	 * @param absConfigBase
	 * @param absCurrentDir
	 * @return mergedConfig
	 * 
	 */
	public static JsonElement getInherittedConfig(JsonElement mergedConfig, String absConfigBase, String absCurrentDir) {
		// Can we go further up?
		String configfile = "config.json";
		if (!absCurrentDir.equals(absConfigBase)) {
			// Yes, get parent directory of the current directory
			String absParentPath = Paths.get(absCurrentDir).getParent().toString();
			// Merge the parent object
			mergedConfig = getInherittedConfig(mergedConfig, absConfigBase, absParentPath);
		}
		// Now merge the current config file. First check for configuration file
		File cfgFileNameCurrent = new File(absCurrentDir.concat("/".concat(configfile)));
		
		if (cfgFileNameCurrent.exists()) {
			// Create Json Element
			try {
				JsonElement cfgCurrentEle = jp.parse(new FileReader(cfgFileNameCurrent));
				JsonObject cfgCurrentObj = cfgCurrentEle.getAsJsonObject();
				// Iterate through module objects and only merge what is defined in local config
				JsonElement currentObjEle = cfgCurrentObj.get("modules");
				JsonObject currentObjObj = currentObjEle.getAsJsonObject();
				for (Map.Entry<String, JsonElement> entry : mergedConfig.getAsJsonObject().get("modules").getAsJsonObject().entrySet()) {
					if (currentObjObj.has(entry.getKey())) {
						merge(currentObjObj.get(entry.getKey()), mergedConfig.getAsJsonObject().get("modules").getAsJsonObject().get(entry.getKey()));
					}
				}
			} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return mergedConfig;	
	}
	
	/**
	 * 
	 * Merges and retrieves the base config json representation.
	 * 
	 * @param localConfig
	 * @param absConfigBase
	 * @param absCurrentDir
	 * @return baseObject
	 * 
	 */
	public static JsonElement getBaseConfig(JsonElement localConfig, String absConfigBase, String absCurrentDir) {
		JsonObject baseObject = new JsonObject();
		JsonElement baseElement = (JsonElement) baseObject;
		
		/* Merge the local config first so the base config will overwrite it. */
		merge(localConfig, baseObject);
		
		// Can we go up one?
		if (!absCurrentDir.equals(absConfigBase)) {
			// Yes.  Get parent directory of the current directory
			String absParentPath = Paths.get(absCurrentDir).getParent().toString();
			baseElement = getInherittedConfig(baseElement, absConfigBase, absParentPath);
		} 

		return baseObject;
	}
	
	/**
	 * 
	 * Merges two json elements.
	 * 
	 * @param sourceJE
	 * @param destinationJE
	 * @return destinationJE
	 * 
	 */
	public static JsonElement merge(JsonElement sourceJE, JsonElement destinationJE) {
		JsonObject sourceJO = sourceJE.getAsJsonObject();
		JsonObject destinationJO = destinationJE.getAsJsonObject();
		
		for(Map.Entry<String, JsonElement> entry : sourceJO.entrySet()) {
			if (entry.getValue().isJsonObject()) {
				JsonElement tempElement = null;
				if (destinationJO.has(entry.getKey())) {
					tempElement = destinationJO.get(entry.getKey());
				} else {
					JsonElement emptyElement = jp.parse("{}");
					destinationJO.add(entry.getKey(), emptyElement);
					tempElement = destinationJO.get(entry.getKey());
				}
				merge(entry.getValue().getAsJsonObject(), tempElement);		
			} else if (entry.getValue().isJsonArray()) {
				String newKey = entry.getKey();
				JsonElement newElement = deepClone(entry.getValue());
				destinationJO.add(newKey, newElement);
			} else if (!entry.getValue().isJsonNull()){
				destinationJO.addProperty(entry.getKey(), entry.getValue().getAsString());
			}
		}
		
		destinationJE = (JsonElement) destinationJO;
		return destinationJE;
	}
		
	/**
	 * 
	 * Deep merges two json files.
	 * 
	 * @param pathA
	 * @param pathb
	 * @param pathSaved
	 * @return success
	 *  
	 */
	public static boolean deepMerge(String pathA, String pathb, String pathSaved) {
		//Let's assume there is no choice of which module and so we choose sch
		String module = "sch";
		boolean success = true;
		JsonObject base = JsonObjectsUtil.createSkeletonConfig(module);
		JsonObject objA = JsonObjectsUtil.createJsonObjectFromFile(pathA);
		JsonObject objB = JsonObjectsUtil.createJsonObjectFromFile(pathb);
		
		JsonObject module1 = objA.get("modules").getAsJsonObject();
		JsonObject moduleObj = module1.get(module).getAsJsonObject();
		
		try {
			extendJsonObject(objB, ConflictStrategy.PREFER_SECOND_OBJECT, moduleObj);
		} catch (JsonObjectExtensionConflictExeception e) {
			success = false;
			return success;
		}
		base.add(module, objB);
		JsonObject ret = new JsonObject();
		ret.add("modules", base);
		if (!JsonObjectsUtil.writeToFile(pathSaved, ret)) return false;
		
		return success;
	}

	/**
	 * 
	 * Deep merges two json files.
	 * 
	 * @param pathA
	 * @param pathb
	 * @param pathSaved
	 * @return mergedObjectAsString
	 * 
	 */
	public static String deepMergeString(String pathA, String pathb, String pathSaved) {
		//Let's assume there is no choice of which module and so we choose sch
		String module = "sch";
		JsonObject base = JsonObjectsUtil.createSkeletonConfig(module);
		JsonObject objA = JsonObjectsUtil.createJsonObjectFromFile(pathA);
		JsonObject objB = JsonObjectsUtil.createJsonObjectFromFile(pathb);
		
		JsonObject module1 = objA.get("modules").getAsJsonObject();
		JsonObject moduleObj = module1.get(module).getAsJsonObject();
		
		try {
			extendJsonObject(objB, ConflictStrategy.PREFER_SECOND_OBJECT, moduleObj);
		} catch (JsonObjectExtensionConflictExeception e) {
		}
		base.add(module, objB);
		JsonObject mergedObject = new JsonObject();
		mergedObject.add("modules", base);
		JsonObjectsUtil.writeToFile(pathSaved, mergedObject);
		String mergedObjectAsString = mergedObject.getAsString();
		
		return mergedObjectAsString;
	}

	/**
	 * 
	 * Deep merges two json files.
	 * 
	 * @param pathA
	 * @param pathb
	 * @param pathSaved
	 * @return mergedObject
	 * 
	 */
	public static JsonObject deepMergeObject(String pathA, String pathb, String pathSaved) {
		//Let's assume there is no choice of which module and so we choose sch
		String module = "sch";
		JsonObject base = JsonObjectsUtil.createSkeletonConfig(module);
		JsonObject objA = JsonObjectsUtil.createJsonObjectFromFile(pathA);
		JsonObject objB = JsonObjectsUtil.createJsonObjectFromFile(pathb);
		
		JsonObject module1 = objA.get("modules").getAsJsonObject();
		JsonObject moduleObj = module1.get(module).getAsJsonObject();
		
		try {
			extendJsonObject(objB, ConflictStrategy.PREFER_SECOND_OBJECT, moduleObj);
		} catch (JsonObjectExtensionConflictExeception e) {
		}
		base.add(module, objB);
		JsonObject mergedObject = new JsonObject();
		mergedObject.add("modules", base);
	
		return mergedObject;
	}

	/**
	 * 
	 * Converts one line json data representation into pretty form with tabs.
	 * 
	 * @param uglyString
	 * @return prettyJson
	 * 
	 */
	public static String beautifyJson(String uglyString) {
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(uglyString).getAsJsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(json);
		return prettyJson;
	}
	
	/**
	 * 
	 * Writes a json object to a file.
	 * 
	 * @param path
	 * @param obj
	 * @return success
	 * 
	 */
	private static boolean writeToFile(String path, JsonObject obj) {
		File file = new File(path);
		boolean success = true;
		if (!file.exists()) {
			try {
				file.createNewFile();
				FileOutputStream fos = new FileOutputStream(file, false);
				String uglyString = obj.toString();
				String prettyString = beautifyJson(uglyString);
				byte[] strToBytes = prettyString.getBytes();
				fos.write(strToBytes);
				fos.close();
				return success;
			} catch (IOException e) {
				success = false;
				return success;
			}
		} else {
			success = false;
			return success;
		}
	}

	/**
	 * 
	 * Creates an empty json object.
	 * 
	 * @param module
	 * @return skeleton
	 * 
	 */
	private static JsonObject createSkeletonConfig(String module) {
		JsonObject skeleton = new JsonObject();
		skeleton.add(module, new JsonObject());
		return skeleton;
	}

	/**
	 * 
	 * Creates a json object from a json file.
	 * 
	 * @param filePath
	 * @return jsonObj
	 * 
	 */
	private static JsonObject createJsonObjectFromFile(String filePath) {
		//file parameter is not being used, using hard coded static paths for testing.
		JsonObject jsonObj = null;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			JsonParser jp = new JsonParser();
			JsonElement je = jp.parse(br);
			JsonObject jo = je.getAsJsonObject();
			
			return jo;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return jsonObj;
	}
	
	/**
	 * 
	 * Recursive function to merge json objects.
	 * 
	 * @param destinationObject
	 * @param conflictResolutionStrategy
	 * @param objs
	 * @throws JsonObjectExtensionConflictExeception
	 * 
	 */
	public static void extendJsonObject(JsonObject destinationObject, ConflictStrategy conflictResolutionStrategy, JsonObject ... objs) throws JsonObjectExtensionConflictExeception {
		for (JsonObject obj : objs) {
			extendJsonObject(destinationObject, obj, conflictResolutionStrategy);
		}
	}
	
	/**
	 * 
	 * Recursive function to merge json objects.
	 * 
	 * @param leftObj
	 * @param rightObj
	 * @param conflictStrategy
	 * @throws JsonObjectExtensionConflictExeception
	 * 
	 */
	private static void extendJsonObject(JsonObject leftObj, JsonObject rightObj, ConflictStrategy conflictStrategy) throws JsonObjectExtensionConflictExeception {
		for (Map.Entry<String, JsonElement> rightEntry : rightObj.entrySet()) {
			String rightKey = rightEntry.getKey();
			JsonElement rightVal = rightEntry.getValue();
			if (leftObj.has(rightKey)) {
				JsonElement leftVal = leftObj.get(rightKey);
				if (leftVal.isJsonArray() && rightVal.isJsonArray()) {
					JsonArray leftArr = leftVal.getAsJsonArray();
					JsonArray rightArr = rightVal.getAsJsonArray();
					for (int i = 0; i < rightArr.size(); i++) {
						leftArr.add(rightArr.get(i));
					}
				} else if (leftVal.isJsonObject() && rightVal.isJsonObject()) {
					extendJsonObject(leftVal.getAsJsonObject(), rightVal.getAsJsonObject(), conflictStrategy);
				} else {
					handleMergeConflict(rightKey, leftObj, leftVal, rightVal, conflictStrategy);
				}
			} else {
				leftObj.add(rightKey,  rightVal);
			}
		}
	}
	
	/**
	 * 
	 * Helper function to provide additional options for merging.
	 * 
	 * @param key
	 * @param leftObj
	 * @param leftVal
	 * @param rightVal
	 * @param conflictStrategy
	 * @throws JsonObjectExtensionConflictExeception
	 * 
	 */
	private static void handleMergeConflict(String key, JsonObject leftObj, JsonElement leftVal, JsonElement rightVal, ConflictStrategy conflictStrategy) throws JsonObjectExtensionConflictExeception {
		switch (conflictStrategy) {
			case PREFER_FIRST_OBJECT:
				break;
			case PREFER_SECOND_OBJECT:
				leftObj.add(key, rightVal);
				break;
			case PREFER_NON_NULL:
				if (leftVal.isJsonNull() && !rightVal.isJsonNull()) {
					leftObj.add(key, rightVal);
				}
				break;
			case THROW_EXCEPTION:
				throw new JsonObjectExtensionConflictExeception("Key " + key + " exists in both objects and the conflict resolution strategy is " + conflictStrategy);
			default:
				throw new JsonObjectExtensionConflictExeception("The conflict resolution strategy " + conflictStrategy + " is unknown and cannot be processed");
		}
	}
}
