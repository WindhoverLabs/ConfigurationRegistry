package com.windhoverlabs.studio;

/**
 * 
 * Java object representation of a json object and it's key or name.
 * Has getters and setters for fields.
 * 
 * @author vagrant
 *
 */
public class NamedObject {
	
	private String name;
	private Object object;
	private String path;
	private boolean overridden;
	
	public NamedObject() {
		this.overridden = false;
		this.path = "";
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public final String getName() {
		return this.name;
	}
	
	public void setObject(Object object) {
		this.object = object;
	}
	
	public final Object getObject() {
		return this.object;
	}
	
	public void setOverridden(boolean overridden) {
		this.overridden = overridden;
	}
	
	public final boolean getOverridden() {
		return this.overridden;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public final String getPath() {
		return this.path;
	}

	public void save() {
		System.out.println(getPath());
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NamedObject other = (NamedObject) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
}
