package com.myplugin.rmp;

/**
 * 
 * Java object representation of a Telemetry definition.
 * Has getters and setters for fields.
 * 
 * @author vagrant
 *
 */
public class TelemetryDefinition extends MessageDefinition {
	public TelemetryDefinition() {
    	super("", -1);
    }
    
	public TelemetryDefinition(int msgid) {
    	super("", msgid);
    }
    
	public TelemetryDefinition(String macro) {
    	super(macro, -1);
    }
    
	public TelemetryDefinition(String macro, int msgid) {
    	super(macro, msgid);
    }
}
