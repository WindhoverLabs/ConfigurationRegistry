package com.windhoverlabs.studio;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;


/**
 * 
 * ConfigDB Registry. Abstraction to handle the modification of the configuration.
 * This is a Subject entity notifies observers listening to change.
 * 
 * @author vagrant
 *
 */
public class ConfigDB extends Observable {
	
	private JsonElement base;
	private JsonElement local;
	private JsonElement combined;
	private String path;
	private boolean dirty;
	private boolean updateInProgress;
	private int updates = 0;
	
	public ConfigDB() {
		dirty = false;
		updateInProgress = false;
	}
	
	public void addChangeListener(Observer observer) {
		if (observer != null) {
			addObserver(observer);
		}
	}
	
	public void removeChangeListener(Observer observer) {
		if (observer != null) {
			deleteObserver(observer);
		}
	}
	
	public void removeAllChangeListener() {
		deleteObservers();
	}
	
    private void notifyListeners() {
    	notifyObservers();
    }
    
	public final boolean isDirty() {
		return dirty;
	}

	private void makeDirty() {
		// Dirty will never be set since update in progress will return true for functions who call makedirty(). When it ends, isDirty() will return false and so it will never make dirty
		// and notify the listeners of the changes.
		dirty = true;
		setChanged();
		
		if (updates == 0) {
			createCombined();
			notifyListeners();
		}
	}
	
	
	private void makeClean() {
		dirty = false;
    	setChanged();
		notifyListeners();
	}
	
	public void saveFile() {
		saveToFile(this.local);
		makeClean();
	}
	
	public void saveToFile(JsonElement json) {
		String toBeSaved = JsonObjectsUtil.beautifyJson(json.getAsJsonObject().toString());
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(path));
			writer.write(toBeSaved);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			
			if (ResourcesPlugin.getPlugin() != null) {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				workspace.getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public void createCombined() {
		this.combined = new JsonObject();
		JsonObjectsUtil.merge(this.base, this.combined);
		JsonObjectsUtil.merge(this.local, this.combined);
	}
	
	private JsonElement getBase() {
		return this.base;
	}
	
	public void setBase(JsonElement config) {
		this.base = config;
	}
	
	public JsonElement getLocal() {
		return this.local;
	}
	
	public void setLocal(JsonElement config) {
		this.local = config;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public final JsonElement getJsonElementByPath(String path) {
		JsonElement jsonElement = getJsonElementByPath(path, JSONForm.COMBINED);
		
		return jsonElement;
	}
    
    
	public final JsonElement getJsonElementByPath(String path, boolean withCreate) {
		JsonElement jsonElement = getJsonElementByPath(path, JSONForm.COMBINED, withCreate);
		
		return jsonElement;
	}	
	
	private JsonElement getJsonElementByPath(String path, JSONForm which) {
		return getJsonElementByPath(path, which, false);
	}
    
	
	private JsonElement getJsonElementByPath(String path, JSONForm which, boolean withCreate) {
		JsonElement tgtElement = null;
		boolean addedNewElement = false;
		
        switch(which) {
            case BASE:
                tgtElement = this.base;
                break;

            case LOCAL:
                tgtElement = this.local;
                break;

            default:
                tgtElement = this.combined;
                break;
        }
		
		String[] parts = path.split("\\.|\\[|\\]");
	    JsonElement result = tgtElement;

	    for (String key : parts) {

	        key = key.trim();
	        if (key.isEmpty())
	            continue;

	        if (result == null){
	            result = JsonNull.INSTANCE;
	            break;
	        }

	        if (result.isJsonObject()){
	            if(((JsonObject)result).has(key)) {
		            result = ((JsonObject)result).get(key);
	            } else {
	            	if(withCreate) {
	            		JsonObject newObject = new JsonObject();
	            		((JsonObject)result).add(key, newObject);
	            		addedNewElement = true;
	            		result = newObject;
	            	} else {
	            		result = null;
	            	}
	            }
	        }
	        else if (result.isJsonArray()){
	            int ix = Integer.valueOf(key);
	            
	            if(ix < ((JsonArray)result).size()) {
	                result = ((JsonArray)result).get(ix);
	            } else {
	            	if(withCreate) {
            			JsonObject newObject = null;
	            		while(ix >= ((JsonArray)result).size()) {
	            			newObject = new JsonObject();
	            			((JsonArray)result).add(newObject);
	            		}
	            		addedNewElement = true;
	            		result = newObject;
	            	} else {
                        result = null;
	            	}
	            }
	        }
	        else break;
	    }
	    
        if(addedNewElement == true) {
        	getJsonElementByPath(path, JSONForm.LOCAL, true);
    		makeDirty();
        }

	    return result;
	}
	
	
	public final JsonObject getJsonObjectByPath(String path) {
		return getJsonObjectByPath(path, false);
	}
	
	public final JsonObject getJsonObjectByPath(String path, boolean withCreate) {
		JsonElement jsonElement = getJsonElementByPath(path, withCreate);
		
		if(jsonElement == null) {
			return null;
		}
		
		if(jsonElement.isJsonObject() == false) {
			return null;
		}
		
		return jsonElement.getAsJsonObject();
	}
	
	private final JsonObject getJsonObjectByPath(String path, JSONForm which) {
		JsonElement tgtElement = null;
		
        switch(which) {
            case BASE:
                tgtElement = this.base;
                break;

            case LOCAL:
                tgtElement = this.local;
                break;

            default:
                tgtElement = this.combined;
                break;
        }
        
		String[] parts = path.split("\\.|\\[|\\]");
		JsonObject toSearchObject = tgtElement.getAsJsonObject();
		
		for (int i = 0; i < parts.length; i++) {
			if (i + 1 == parts.length) {
				if (toSearchObject.has(parts[i])) {
					toSearchObject = toSearchObject.get(parts[i]).getAsJsonObject();

					return toSearchObject;
				} else {
					return null;
				}
			}
			if (toSearchObject.has(parts[i])) {
				toSearchObject = toSearchObject.get(parts[i]).getAsJsonObject();
			} else {
				return null;
			}
		}
		return null;
	}
	
	public void setObjectByPathKey(String objectPath, JsonElement jsonElement) {
		String parentObject = getPathOfParentElement(objectPath);
		String key = getElementNameByPath(objectPath);
		
		JsonObject currentObject = getJsonObjectByPath(parentObject, JSONForm.LOCAL);
		JsonObject combinedObject = getJsonObjectByPath(parentObject, JSONForm.BASE);

		if (currentObject.has(key)) {
			currentObject.remove(key);
		}
		if (combinedObject.has(key)) {
			combinedObject.remove(key);
		}
		
		currentObject.add(key, jsonElement);
		combinedObject.add(key, jsonElement);
		
		makeDirty();
		
	}
	
	public void addObjectByPathKey(String parentObjectPath, String key, JsonObject object) {
		addObjectByPath(parentObjectPath, JSONForm.LOCAL);
		JsonObject currentObject = getJsonObjectByPath(parentObjectPath, JSONForm.LOCAL);
		
		currentObject.add(key, object);
		
		makeDirty();
	}
	
	private void addObjectByPath(String objPath, JSONForm which) {
		String[] parts = objPath.split("\\.|\\[|\\]");
		JsonElement tgtElement = which.equals(JSONForm.LOCAL) ? this.local : this.base;
		JsonObject parentObject = tgtElement.getAsJsonObject();

        for (String elemName : parts) {
        	JsonElement curElement = parentObject.get(elemName);
        	
        	if(curElement == null) {
        		JsonObject newObject = new JsonObject();
        		parentObject.add(elemName, newObject);
        		parentObject = newObject;
  
        	} else if(curElement.isJsonObject() == false) {
        		/* TODO:  Add error handling */
        	} else {
            	parentObject = curElement.getAsJsonObject();
        	}
        }
	}
	
	public final NamedObject getNamedObjectByPath(String path) {	
		NamedObject namedObj = getNamedObjectByPath(path, JSONForm.COMBINED);

		return namedObj;
	}
	
	private final NamedObject getNamedObjectByPath(String path, JSONForm which) {
		String[] parts = path.split("\\.|\\[|\\]");
		
		JsonObject jsonObj = getJsonObjectByPath(path, which);
		NamedObject namedObj = null; 
		
		if(jsonObj != null) {
			namedObj = new NamedObject();
			
			namedObj.setName(parts[parts.length - 1]);
			namedObj.setPath(path);
			namedObj.setObject(jsonObj);
			
			if(isOverridden(path)) {
				namedObj.setOverridden(true);
	    	} else {
	    		namedObj.setOverridden(false);
	    	}
		}

		return namedObj;
	}
	
	private void setNamedObject(NamedObject namedObj, JSONForm which) {
		String[] parts = namedObj.getPath().split("\\.|\\[|\\]");
		int depth = parts.length;
		JsonElement localPointer;

		switch(which) {
        case BASE:
       	 localPointer = this.base;
            break;

        case LOCAL:
       	 localPointer = this.local;
            break;

        default:
       	 localPointer = this.combined;
            break;
		 }
		JsonObject localObject = localPointer.getAsJsonObject();
		
		for (int i = 0; i < depth; i++) {
			// You are currently at the selected element
			if (i + 1 == depth) {
				JsonElement toUpdate = (JsonElement) namedObj.getObject();
				localObject.add(namedObj.getName(), toUpdate);
				break;
			} 			
			// Let's check if the path exists and if it does update the crawl to use the json object.
			// If the path doesn't exist then create an empty object inside the current crawled object.
			if (localObject.has(parts[i])) {
				// Update the element to be crawled.
				localObject = localObject.get(parts[i]).getAsJsonObject(); 
			} else {
				localObject.add(parts[i], new JsonObject());
				localObject = localObject.get(parts[i]).getAsJsonObject();
			}			
		}
		
		switch(which) {
        case BASE:
       	 	this.base = localPointer;
            break;

        case LOCAL:
       	 	this.local = localPointer;
            break;

        default:
        	this.combined = localPointer;
            break;
		}
	}
	
	public boolean isOverridden(String path) {
		JsonElement result = getJsonElementByPath(path, JSONForm.LOCAL);
		
		if((result == null) || (result == JsonNull.INSTANCE)) {
			return false;
		} else {
			return true;
		}
	}
	
	public void unoverrideByPathKey(String path, String key) {
		
		JsonElement jsonLocalParentElement = getJsonElementByPath(path, JSONForm.LOCAL);
	    JsonObject jsonLocalParentObject = (JsonObject) jsonLocalParentElement.getAsJsonObject();
	    
		jsonLocalParentObject.remove(key);
		
		int objSize = jsonLocalParentObject.size();
		if(objSize <= 0) {
			String parentPath = getPathOfParentElement(path);
			String parentKey = getElementNameByPath(path);
			unoverrideByPathKey(parentPath, parentKey);
		}

		makeDirty();
	}	
	
	public String getPathOfParentElement(String path) {
		String[] parts = path.split("\\.|\\[|\\]");
		String parentPath = parts[0];
		
		
		for (int i = 1; i < parts.length-1; i++) {
			parentPath = parentPath + "." + parts[i];
		}
		
		return parentPath;
	}
	
	public String getElementNameByPath(String path) {
		return getElementNameByPath(path, 1);
	}
	
	public String getElementNameByPath(String path, int backOffset) {
		String[] parts = path.split("\\.|\\[|\\]");
		String elementName = "";
		
		elementName = parts[parts.length - backOffset];
		
		return elementName;
	}
	
	
	
	public String getStringByPath(String path, boolean withCreate, String defaultValue) {
		boolean addKey = false;
		String parentPath = getPathOfParentElement(path);
		JsonObject parentObject = getJsonObjectByPath(parentPath, withCreate);		
		String keyName = getElementNameByPath(path);
		
		if(parentObject == null) {
			return null;
		} else if(parentObject.has(keyName)) {
			return parentObject.get(keyName).getAsString();
		} else {
			if(withCreate == true) {
				parentObject.addProperty(keyName, defaultValue);
				addKey = true;
			} else {
				return null;
			}
		}
		
		if(addKey == true) {
			JsonPrimitive intPrimitive = new JsonPrimitive("");
			setKeyValue(parentPath, keyName, intPrimitive);
		}

		return defaultValue;
	}
	
	
	
	public String getStringByPath(String path, boolean withCreate) {
        return getStringByPath(path, withCreate, null);
	}
	
	
	
	public void deleteKeyByPath(String path) {
		String parentPath = getPathOfParentElement(path);
		String key = getElementNameByPath(path);
		JsonObject localObject = getJsonObjectByPath(parentPath, JSONForm.LOCAL);
		
		localObject.remove(key);
		createCombined();
		
		makeDirty();
	}
	
	public void setKeyByPath(String newPath, String oldPath) {
		String parentPath = getPathOfParentElement(oldPath);
		String oldKey = getElementNameByPath(oldPath);
		String newKey = getElementNameByPath(newPath);
		
		JsonObject parentObject = getJsonObjectByPath(parentPath, JSONForm.LOCAL);
		JsonObject localObject = getJsonObjectByPath(oldPath, JSONForm.LOCAL);

		parentObject.remove(oldKey);
		parentObject.add(newKey, localObject);

		makeDirty();
	}
	
	
	
	public String getStringByPath(String path) {
		return getStringByPath(path, false);
	}
	
	
	
	public void setStringByPath(String path, String value) {
		String parentPath = getPathOfParentElement(path);
		String key = getElementNameByPath(path);
		
		JsonPrimitive stringPrimitive = new JsonPrimitive(value);
		setKeyValue(parentPath, key, stringPrimitive);
	}
	
	
	
	public boolean isPathValid(String path) {
		JsonElement jsonElement = getJsonElementByPath(path);
		
		if(jsonElement != null) {
			return true;
		} else {
			return false;
		}	
	}
	
	
	public int getIntegerByPath(String path, boolean withCreate, int defaultValue) {
		boolean addKey = false;
		String parentPath = getPathOfParentElement(path);
		JsonObject parentObject = getJsonObjectByPath(parentPath, withCreate);		
		String keyName = getElementNameByPath(path);
		
		if(parentObject == null) {
			return 0;
		} else if(parentObject.has(keyName)) {
			JsonElement jsonElement = parentObject.get(keyName);
			
			if(jsonElement.isJsonPrimitive()) {
				JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
				
				if(jsonPrimitive.isNumber()) {
					return jsonPrimitive.getAsInt();
				} else if(jsonPrimitive.isString()) {
					String strValue = jsonPrimitive.getAsString();
					
					try {
						return Integer.parseInt(strValue, 10);
					} catch (NumberFormatException ex) {
						return Integer.decode(strValue);
					}
				}
			} else {
				return 0;
			}
			return parentObject.get(keyName).getAsInt();
		} else {
			if(withCreate == true) {
				parentObject.addProperty(keyName, defaultValue);
				addKey = true;
			} else {
				return 0;
			}
		}
		
		if(addKey == true) {
			JsonPrimitive intPrimitive = new JsonPrimitive(defaultValue);
			setKeyValue(parentPath, keyName, intPrimitive);
		}

		return defaultValue;
	}
	
	
	
	public int getIntegerByPath(String path, boolean withCreate) {
        return getIntegerByPath(path, withCreate, 0);
	}
	
	
	
	public int getIntegerByPath(String path) {
		return getIntegerByPath(path, false);
	}
	
	public boolean getBooleanByPathKey(String path, String key) {
		JsonPrimitive jsonPrimitive = getKeyValue(path, key);
		Boolean result = null;
		if (jsonPrimitive != null) {
			result = jsonPrimitive.getAsBoolean();
		}
		return result;
	}
	
	public void setBooleanByPathKey(String path, String key, boolean value) {
		JsonPrimitive booleanPrimitive = new JsonPrimitive(value);
		setKeyValue(path, key, booleanPrimitive);
	}
	
	public int getIntegerByPathKey(String path, String key) {
		JsonPrimitive jsonPrimitive = getKeyValue(path, key);
		Integer result = null;
		if (jsonPrimitive != null) {
			result = jsonPrimitive.getAsInt();
		}
		return result;
	}
	
	public void setIntegerByPathKey(String path, String key, int value) {
		JsonPrimitive intPrimitive = new JsonPrimitive(value);
		setKeyValue(path, key, intPrimitive);
	}
	
	public float getFloatByPathKey(String path, String key) {
		JsonPrimitive jsonPrimitive = getKeyValue(path, key);
		Float result = null;
		if (jsonPrimitive != null) {
			result = jsonPrimitive.getAsFloat();
		}
		return result;
	}
	
	public void setFloatByPathKey(String path, String key, float value) {
		JsonPrimitive floatPrimitive = new JsonPrimitive(value);
		setKeyValue(path, key, floatPrimitive);
	}
	
	
	
	private JsonPrimitive getKeyValue(String parentObjectPath, String key) {
		JsonObject parentObject = getJsonObjectByPath(parentObjectPath, JSONForm.COMBINED);
		JsonPrimitive returnedJsonPrimitive = null;
		if (parentObject != null && parentObject.has(key)) {
			JsonElement jsonElement = parentObject.get(key);
			if (jsonElement.isJsonPrimitive()) {
				returnedJsonPrimitive = jsonElement.getAsJsonPrimitive();
			}
		}
		
		return returnedJsonPrimitive;
	}
	
	private void setKeyValue(String parentObjectPath, String keyName, JsonPrimitive value) {
		addObjectByPath(parentObjectPath, JSONForm.LOCAL);
		JsonObject localObject = getJsonObjectByPath(parentObjectPath, JSONForm.LOCAL);
		localObject.add(keyName, value);
		
		makeDirty();
	}
	

	public void addKey(String path) {
		String parentPath = getPathOfParentElement(path);
		String key = getElementNameByPath(path);
		
		JsonPrimitive empty = new JsonPrimitive("");
		setKeyValue(parentPath, key, empty);
	}
	
	public void update(NamedObject js) {
		// Update our in-memory object/representation
		setNamedObject(js, JSONForm.LOCAL);
		// Now let's update the persistent representation. Can be file, database etc.
		
		makeDirty();
	}
	
	public static boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    } catch(NullPointerException e) {
	        return false;
	    }
	    // only got here if we didn't return false
	    return true;
	}
	
	public void startUpdate() {
		updates++;
	}
	
	public void endUpdate() {
		updates--;
				
		if (isDirty() && updates == 0) {
			makeDirty();
		}

	}
	
	private boolean isUpdateInProgress() {
		return updateInProgress;
	}
	
	public ArrayList<String> getModulePaths() {
		ArrayList<String> outArray = new ArrayList<String>();
		
        JsonObject modules = getJsonObjectByPath("modules");        
        for (Map.Entry<String,JsonElement> entry : modules.entrySet()) {
        	if (entry.getValue().isJsonObject()) {
	        	String moduleKey = entry.getKey();
	        	
	        	outArray.add("modules." + moduleKey);
        	}
        } 

		return outArray;
	}
	
	public ArrayList<String> getCoreComponentPaths() {
		ArrayList<String> outArray = new ArrayList<String>();
		
        JsonObject components = getJsonObjectByPath("core");        
        for (Map.Entry<String,JsonElement> entry : components.entrySet()) {
        	if (entry.getValue().isJsonObject()) {
	        	String moduleKey = entry.getKey();
	        	
	        	outArray.add("core." + moduleKey);
        	}
        } 

		return outArray;
	}
	
	public ArrayList<MessageDefinition> getMessageDefinitions() {
		ArrayList<MessageDefinition> outArray = new ArrayList<MessageDefinition>();
		
		ArrayList<TelemetryDefinition> telemDefs = getTelemetryDefinitions();
		ArrayList<CommandDefinition> cmdDefs = getCommandDefinitions();
		
		outArray.addAll(telemDefs);
		outArray.addAll(cmdDefs);
		
		return outArray;
	}
	
	
	
	public ArrayList<String> getTelemetryDefinitionPaths(String path) {
		ArrayList<String> outArray = new ArrayList<String>();

		String telemDefsPath = path + ".telemetry"; 
		JsonObject jsonObjTelemetry = getJsonObjectByPath(telemDefsPath);
		
        for (Map.Entry<String,JsonElement> jsonElemTelemDef : jsonObjTelemetry.entrySet()) {
        	if (jsonElemTelemDef.getValue().isJsonObject()) {
	        	String telemMsgName = jsonElemTelemDef.getKey();
	        	
	        	String telemMsgDefPath = telemDefsPath + "." + telemMsgName;
	        	outArray.add(telemMsgDefPath);
        	}
        } 

		return outArray;
	}
	
	
	
	public ArrayList<String> getTelemetryDefinitionPaths() {
		ArrayList<String> outArray = new ArrayList<String>();
		ArrayList<String> modulePaths = getModulePaths();
		ArrayList<String> coreComponentPaths = getCoreComponentPaths();
		
		for (String path : modulePaths) {
			ArrayList<String> newDefPaths = getTelemetryDefinitionPaths(path);
			
			outArray.addAll(newDefPaths);
		}
		
		for (String path : coreComponentPaths) {
			ArrayList<String> newDefPaths = getTelemetryDefinitionPaths(path);
			
			outArray.addAll(newDefPaths);
		}
		
		return outArray;
	}
	
	
	
	public ArrayList<String> getCommandPrimaryDefinitionPaths(String path) {
		ArrayList<String> outArray = new ArrayList<String>();

		String cmdDefsPath = path + ".commands"; 
		JsonObject jsonObjCommand = getJsonObjectByPath(cmdDefsPath);
		
        for (Map.Entry<String,JsonElement> jsonElemCmdDef : jsonObjCommand.entrySet()) {
        	if (jsonElemCmdDef.getValue().isJsonObject()) {
	        	String cmdMsgName = jsonElemCmdDef.getKey();
	        	
	        	String cmdMsgDefPath = cmdDefsPath + "." + cmdMsgName;
	        	outArray.add(cmdMsgDefPath);
        	}
        } 
		
		return outArray;
	}
	
	
	
	private ArrayList<String> getCommandPrimaryDefinitionPaths() {
		ArrayList<String> outArray = new ArrayList<String>();
		ArrayList<String> modulePaths = getModulePaths();
		ArrayList<String> coreComponentPaths = getCoreComponentPaths();
		
		for (String path : modulePaths) {
			ArrayList<String> newDefPaths = getCommandPrimaryDefinitionPaths(path);
			
			outArray.addAll(newDefPaths);
		}
		
		for (String path : coreComponentPaths) {
			ArrayList<String> newDefPaths = getCommandPrimaryDefinitionPaths(path);
			
			outArray.addAll(newDefPaths);
		}
		
		return outArray;
	}
	
	
	
	public ArrayList<String> getCommandSecondaryDefinitionPaths(String primaryPath) {
		ArrayList<String> outArray = new ArrayList<String>();
		
		String secondaryPath = primaryPath + ".commands";
		
        JsonObject commands = getJsonObjectByPath(secondaryPath);        
        for (Map.Entry<String,JsonElement> entry : commands.entrySet()) {
        	if (entry.getValue().isJsonObject()) {
	        	String cmdSecondaryKey = entry.getKey();
	        	
	        	outArray.add(secondaryPath + "." + cmdSecondaryKey);
        	}
        } 
		
		return outArray;
	}
	
	
	
	public ArrayList<TelemetryDefinition> getTelemetryDefinitions(String name) {
		ArrayList<TelemetryDefinition> outArray = new ArrayList<TelemetryDefinition>();
		
		ArrayList<String> telemDefPaths = getTelemetryDefinitionPaths(name);
		
		for (String path : telemDefPaths) {
			TelemetryDefinition telemDef = new TelemetryDefinition();
			
			int msgid = getIntegerByPath(path + ".msgid");
			String macro = getElementNameByPath(path);
			
			telemDef.setMessageID(msgid);
			telemDef.setMacro(macro);
			telemDef.setPath(path);
			
			outArray.add(telemDef);
		}
		
		return outArray;
	}
	
	
	
	public ArrayList<TelemetryDefinition> getTelemetryDefinitions() {
		ArrayList<TelemetryDefinition> outArray = new ArrayList<TelemetryDefinition>();
		
		ArrayList<String> telemDefPaths = getTelemetryDefinitionPaths();
		
		for (String path : telemDefPaths) {
			TelemetryDefinition telemDef = new TelemetryDefinition();
			
			int msgid = getIntegerByPath(path + ".msgid");
			String macro = getElementNameByPath(path);
			
			telemDef.setMessageID(msgid);
			telemDef.setMacro(macro);
			telemDef.setPath(path);
			
			outArray.add(telemDef);
		}
		
		return outArray;
	}
	
	
	
	public ArrayList<CommandDefinition> getCommandDefinitions() {
		ArrayList<CommandDefinition> outArray = new ArrayList<CommandDefinition>();
		
		ArrayList<String> cmdDefPrimaryPaths = getCommandPrimaryDefinitionPaths();
		
		for (String primaryPath : cmdDefPrimaryPaths) {
			int msgid = getIntegerByPath(primaryPath + ".msgid");
			String macro = getElementNameByPath(primaryPath);
			
			ArrayList<String> cmdDefSecondaryPaths = getCommandSecondaryDefinitionPaths(primaryPath);
			
			for (String secondaryPath : cmdDefSecondaryPaths) {
				CommandDefinition cmdDef = new CommandDefinition();
				
				String commandName = getElementNameByPath(secondaryPath);
				
				int commandCode = getIntegerByPath(secondaryPath + ".cc");

				cmdDef.setMessageID(msgid);
				cmdDef.setMacro(macro);
			    cmdDef.setCommandName(commandName);
				cmdDef.setCommandCode(commandCode);
				cmdDef.setPath(primaryPath);
				cmdDef.setSecondaryPath(secondaryPath);
				
				outArray.add(cmdDef);
			}
		}
		
		return outArray;
	}
		
	public static boolean isHexadecimal(String value) {
		value = value.toLowerCase();

	    if (value.length() != 4)
	    {
	        return false;
	    }

	    for (int i = 0; i < value.length(); i++)
	    {
	        char c = value.charAt(i);

	        if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f'))
	        {
	            return false;
	        }
	    }

	    return true;
	}
	
	public String getElementNameOfParentByPath(String path) {
		String parentPath = getPathOfParentElement(path);
		// Find the element name of the parent.
		String parentKey = getElementNameByPath(parentPath);
		
		return parentKey;
	}
	
	public boolean isPathTelemetry(String path) {
		String parentName = getElementNameOfParentByPath(path);

		// Is the string representation of parentName the same as "telemetry"
		if(parentName.equals("telemetry")) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isPathCommand(String path) {
		String parentName = getElementNameOfParentByPath(path);
		
		// Is the string representation of parentName the same as "commands"
		if(parentName.equals("commands")) {
			return true;
		} else {
			return false;
		}
	}
	
	public ConfigDB getCopy() {
		ConfigDB copy = new ConfigDB();
		copy.setPath(path);
		copy.setBase(this.base);
		copy.setLocal(this.local);
		copy.createCombined();
		
		return copy;
	}
}