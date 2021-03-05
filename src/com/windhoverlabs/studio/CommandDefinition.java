package com.windhoverlabs.studio;

/**
 * 
 * Java object representation of a Command definition.
 * Has getters and setters for fields.
 * 
 * @author vagrant
 *
 */
public class CommandDefinition extends MessageDefinition {
    static final int    COMMAND_CODE_UNDEFINED = -1;
    static final String COMMAND_NAME_UNDEFINED = "";
    static final String COMMAND_PATH_UNDEFINED = "";
    
	private int    commandCode;
	private String commandName;
	private String secondaryPath;
	
	
	public CommandDefinition() {
    	super();
    	
    	commandCode   = COMMAND_CODE_UNDEFINED;
    	commandName   = COMMAND_NAME_UNDEFINED;
    	secondaryPath = COMMAND_PATH_UNDEFINED;
    }
    
	
	
	public CommandDefinition(int msgid) {
    	super(msgid);
    	
    	commandCode   = COMMAND_CODE_UNDEFINED;
    	commandName   = COMMAND_NAME_UNDEFINED;
    	secondaryPath = COMMAND_PATH_UNDEFINED;
    }
    
	
	
	public CommandDefinition(String macro) {
    	super(macro);
    	
    	commandCode   = COMMAND_CODE_UNDEFINED;
    	commandName   = COMMAND_NAME_UNDEFINED;
    	secondaryPath = COMMAND_PATH_UNDEFINED;
    }
    
	
	
	public CommandDefinition(String macro, int msgid) {
    	super(macro, msgid);
    	
    	commandCode   = COMMAND_CODE_UNDEFINED;
    	commandName   = COMMAND_NAME_UNDEFINED;
    	secondaryPath = COMMAND_PATH_UNDEFINED;
    }
    
	
	
	public CommandDefinition(String macro, int msgid, String commandName, int commandCode) {
    	super(macro, msgid);
    	
    	commandCode   = COMMAND_CODE_UNDEFINED;
    	commandName   = COMMAND_NAME_UNDEFINED;
    	secondaryPath = COMMAND_PATH_UNDEFINED;
    }
	
	
	
	public final String getCommandName() {
		return this.commandName;
	}
	
	
	
	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}
	
	
	
	public final int getCommandCode() {
		return this.commandCode;
	}
	
	
	
	public void setCommandCode(int commandCode) {
		this.commandCode = commandCode;
	}
	
	
	
	public final String getSecondaryPath() {
		return this.secondaryPath;
	}
	
	
	
	public void setSecondaryPath(String secondaryPath) {
		this.secondaryPath = secondaryPath;
	}
	
}

