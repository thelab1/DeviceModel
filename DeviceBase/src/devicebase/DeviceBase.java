package devicebase;

import devicebase.OptionsParser.BaseOptionType;
import devicebase.socket.RapidSocket;
import devicemodel.DeviceNode;
import devicemodel.NodeManager;
import devicemodel.conversions.XmlConversions;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.jdom2.JDOMException;

/**
 *
 * @author Pobzeb
 */
public abstract class DeviceBase {
    public final RapidSocket rapidSocket;

    public DeviceBase(String[] args) {
        // Parse the options.
        Map<String, String> options = OptionsParser.parseOptions(args);

        // Make sure we got our System
        // and Device config files.
        if (!options.containsKey(BaseOptionType.SYSTEM_CONFIG_OPTION.getShortName()) ||
            !options.containsKey(BaseOptionType.DEVICE_CONFIG_OPTION.getShortName())) {
            if (!options.containsKey(BaseOptionType.SYSTEM_CONFIG_OPTION.getShortName())) System.err.println("No System Config argument found");
            if (!options.containsKey(BaseOptionType.DEVICE_CONFIG_OPTION.getShortName())) System.err.println("No Device Config argument found");
            System.exit(1);
        }

        // Get the config files.
        File systemConfigFile = new File(options.get(BaseOptionType.SYSTEM_CONFIG_OPTION.getShortName()));
        if (!systemConfigFile.exists()) {
            System.err.println("System Config file not found.");
            System.exit(1);
        }
        File deviceConfigFile = new File(options.get(BaseOptionType.DEVICE_CONFIG_OPTION.getShortName()));
        if (!deviceConfigFile.exists()) {
            System.err.println("Device Config file not found.");
            System.exit(1);
        }

        // Parse the config files.
        DeviceNode systemConfig = null;
        DeviceNode deviceConfig = null;
        try { systemConfig = XmlConversions.xmlToNode(systemConfigFile); }
        catch (IOException | JDOMException ex) {
            System.err.println("Error parsing System Config file.");
            System.exit(1);
        }
        try { deviceConfig = XmlConversions.xmlToNode(deviceConfigFile); }
        catch (IOException | JDOMException ex) {
            System.err.println("Error parsing Device Config file.");
            System.exit(1);
        }

        // Initialize the root node.
        NodeManager.getInstance(deviceConfig);

        // Update the SystemConfig.
        systemConfig.getChildByPath("/SystemConfig").addChildren(new DeviceNode[] {
            new DeviceNode("ServiceName", NodeManager.getRootNode().getName().toLowerCase()),
            new DeviceNode("RootNode", NodeManager.getRootNode().getName())
        });

        // Update the model with the system config.
        NodeManager.getRootNode().addChild(
            new DeviceNode("DeviceStatus").addChildren(new DeviceNode[] {
                new DeviceNode("DeviceConnection", "Disconnected"),
                new DeviceNode("DeviceState", ""),
                new DeviceNode("DeviceAlarms", "")
            })
        );
        NodeManager.getRootNode().addChild(
            new DeviceNode("Information")).addChild(
                new DeviceNode("SystemConfig"));
        NodeManager.update("/Information/SystemConfig", systemConfig.getChild("SystemConfig"));

        // Create the socket.
        this.rapidSocket = new RapidSocket("localhost", 8081) {
            @Override
            public void onOpen() {
            }

            @Override
            public void onClose() {
            }

            @Override
            public void onError(Exception ex) {
            }
        };
        this.rapidSocket.start();

        // Start the application.
        this.startApplication(options, systemConfig);
    }

    public abstract void startApplication(Map<String, String> args, DeviceNode systemConfig);
}
