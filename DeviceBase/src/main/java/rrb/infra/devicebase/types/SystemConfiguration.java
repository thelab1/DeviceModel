package rrb.infra.devicebase.types;

import org.jdom2.Element;
import rrb.infra.devicemodel.DeviceNode;

/**
 *
 * @author pobzeb
 */
public class SystemConfiguration {
    private String deviceName = "";
    private String deviceDescription = "";
    private String deviceServiceHostname = "";
    private int deviceServicePort = 0;
    private String serviceName = "";
    private String rootNodeName = "";

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceDescription() {
        return this.deviceDescription;
    }

    public void setDeviceDescription(String deviceDescription) {
        this.deviceDescription = deviceDescription;
    }

    public String getDeviceServiceHostname() {
        return this.deviceServiceHostname;
    }

    public void setDeviceServiceHostname(String deviceServiceHostname) {
        this.deviceServiceHostname = deviceServiceHostname;
    }

    public int getDeviceServicePort() {
        return this.deviceServicePort;
    }

    public void setDeviceServicePort(int deviceServicePort) {
        this.deviceServicePort = deviceServicePort;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getRootNodeName() {
        return this.rootNodeName;
    }

    public void setRootNodeName(String rootNodeName) {
        this.rootNodeName = rootNodeName;
    }

    public void setSystemConfig(DeviceNode systemConfig) {
        if (systemConfig.getChild("SystemConfig") != null) {
            if (systemConfig.getChildByPath("/SystemConfig/DeviceName") == null) {
                this.setDeviceName(systemConfig.getChildByPath("/SystemConfig/DeviceName").getValue());
            }

            if (systemConfig.getChildByPath("/SystemConfig/DeviceDescription") == null) {
                this.setDeviceDescription(systemConfig.getChildByPath("/SystemConfig/DeviceDescription").getValue());
            }

            if (systemConfig.getChildByPath("/SystemConfig/DeviceServiceHostname") == null) {
                this.setDeviceServiceHostname(systemConfig.getChildByPath("/SystemConfig/DeviceServiceHostname").getValue());
            }

            if (systemConfig.getChildByPath("/SystemConfig/DeviceServicePort") == null) {
                this.setDeviceServicePort(Integer.parseInt(systemConfig.getChildByPath("/SystemConfig/DeviceServicePort").getValue()));
            }
        }
    }

    public DeviceNode getSystemConfig() {
        DeviceNode root = new DeviceNode("SystemConfig");
        root.addChild(new DeviceNode("DeviceName", this.getDeviceName()));
        root.addChild(new DeviceNode("DeviceDescription", this.getDeviceDescription()));
        root.addChild(new DeviceNode("DeviceServiceHostname", this.getDeviceServiceHostname()));
        root.addChild(new DeviceNode("DeviceServicePort", String.valueOf(this.getDeviceServicePort())));
        root.addChild(new DeviceNode("ServiceName", this.getServiceName()));
        root.addChild(new DeviceNode("RootNode", this.getRootNodeName()));
        return root;
    }
}
