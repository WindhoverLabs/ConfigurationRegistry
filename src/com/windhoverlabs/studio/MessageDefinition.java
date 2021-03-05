package com.windhoverlabs.studio;

/**
 * 
 * Java object representation of a Message definition.
 * Has getters and setters for fields.
 * 
 * @author vagrant
 *
 */
public class MessageDefinition {
	
    private int    msgid;
    private String macro;
    private String path;
    
    static final int MSGID_UNDEFINED = -1;
    static final String MACRO_UNDEFINED = "";
    static final String PATH_UNDEFINED = "";
    
    public MessageDefinition() {
    	this(MACRO_UNDEFINED, MSGID_UNDEFINED);
    }
    
    public MessageDefinition(int msgid) {
    	this(MACRO_UNDEFINED, msgid);
    }
    
    public MessageDefinition(String macro) {
    	this(macro, MSGID_UNDEFINED);
    }
    
    public MessageDefinition(String macro, int msgid) {
    	this.msgid = msgid;
    	this.macro = macro;
    	this.path = PATH_UNDEFINED;
    }
    
    public int getMessageID() {
    	return msgid;
    }
    
    public void setMessageID(int msgid) {
    	this.msgid = msgid;
    }
    
    public String getMacro() {
    	return macro;
    }
    
    public void setMacro(String macro) {
    	this.macro = macro;
    }
    
    public String getPath() {
    	return path;
    }
    
    public void setPath(String path) {
    	this.path = path;
    }
}
