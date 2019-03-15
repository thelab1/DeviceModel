package rrb.infra.devicebase;

import rrb.infra.devicebase.OptionsParser.BaseOptionType;
import rrb.infra.devicemodel.DeviceNode;
import rrb.infra.devicemodel.NodeManager;
import rrb.infra.devicemodel.conversions.XmlConversions;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import org.jdom2.JDOMException;
import rrb.infra.devicebase.types.SystemConfiguration;
import rrb.infra.devicemodel.DeviceModelProto.MessageBlock;
import rrb.infra.rapidsocket.RapidClientHandler;
import rrb.infra.rapidsocket.RapidClientSocket;

/**
 *
 * @author Pobzeb
 */
public abstract class DeviceBase implements RapidClientHandler {
    public final SystemConfiguration systemConfiguration = new SystemConfiguration();
    public final RapidClientSocket rapidSocket;

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

        // Get the local hostname.
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        }
        catch(UnknownHostException ex) {
            System.err.println("Error getting Local Hostname");
            ex.printStackTrace(System.err);
        }

        // Build the service name.
        String serviceName = hostname+"_"+NodeManager.getRootNode().getName().toLowerCase();

        // Update the SystemConfig.
        this.systemConfiguration.setSystemConfig(systemConfig);
        this.systemConfiguration.setServiceName(serviceName);
        this.systemConfiguration.setRootNodeName(NodeManager.getRootNode().getName());

        // Update the model with the device
        // status and the system config.
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
        NodeManager.update("/Information/SystemConfig", this.systemConfiguration.getSystemConfig());

        // Create the socket.
        this.rapidSocket = new RapidClientSocket(this.systemConfiguration.getServiceName(), this.systemConfiguration.getDeviceServiceHostname(), this.systemConfiguration.getDeviceServicePort(), this);

        // Start the application.
        this.startApplication(options, systemConfig);

        // Start the socket.
        this.rapidSocket.start();
    }

    @Override
    public void onOpen() {
        System.out.println("Rapid Client Socket Open");
    }

    @Override
    public void onClose() {
        System.out.println("Rapid Client Socket Closed");
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace(System.err);
    }

    @Override
    public MessageBlock onMessage(MessageBlock msg) {
        // Skip processing this message if it was a reply.
        if (msg.getMessageType() == MessageBlock.MessageTypeType.REPLY) return null;

        // Build the reply.
        MessageBlock.Builder reply = MessageBlock.newBuilder();
        reply.setMessageType(MessageBlock.MessageTypeType.REPLY);
        reply.setToServiceName(msg.getFromServiceName());
        reply.setFromServiceName(this.systemConfiguration.getServiceName());

        // Get the device node for this message.
        DeviceNode node = NodeManager.getChildByPath(msg.getPath());
        if (node == null) {
            // Invalid node path.
            DeviceNode errorNode = new DeviceNode("ERROR").addChildren(new DeviceNode[]{
                new DeviceNode("Code", "500"),
                new DeviceNode("Message", "Invalid Node Path: "+msg.getPath())
            });
            reply.setBody(errorNode.getDeviceNodeProtoBuf());
            this.rapidSocket.sendMessage(reply.build());
        }

        // Send this message node to the setter.
        node.set(NodeManager.toDeviceNode(msg.getBody()));
        reply.setBody(node.getDeviceNodeProtoBuf());

        // Return the reply.
        return reply.build();
    }

    public abstract void startApplication(Map<String, String> args, DeviceNode systemConfig);
}
