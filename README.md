Eclipse plugin to load CFS configuration to YAMCS Studio.

## How to load the plugin with Studio at Startup

Assumptions:  
- YAMCS Studio is installed in `/opt/yamcs-studio`.
- Eclipse for RCP developers 2020-12 (4.18.0) is installed

1. Package the plugin as a `jar` file from Eclipse:
    - Right click on the `ConfigurationRegistry` Project and export as _Deployable plugins and fragments_
    - Click next
      ![eclipse](images/export-as-plugin.png  "export")
    - Set a Directory destination
    - Click Finish
    
    
  This will export a jar file to a `plugins` directory called(assuming no configuration has been changed) `com.windhoverlabs.studio_1.0.0.jar`  

2. Copy the plugin to studio installation:
    ```
     cp [DIR Specified on Eclipse on Step#1]/plugins/com.windhoverlabs.studio_1.0.0.jar /opt/yamcs-studio/plugins/
   ```

3. Open the Studio's configuration file at `/opt/yamcs-studio/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info` and add the following line _exactly as you see it here_ to the end of the:
    ```
   com.windhoverlabs.studio,1.0.0,plugins/com.windhoverlabs.studio_1.0.0.jar,4,false
   ```

The next time you open up Studio, you should have the ConfigurationRegistry plugin available for use.

Documentation updated  on March 8, 2021