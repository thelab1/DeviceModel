package devicebase;

import devicemodel.DeviceNode;
import java.util.Map;

/**
 *
 * @author Pobzeb
 */
public abstract class DeviceBase {

    public DeviceBase(String[] args) {
        // Parse the options.
        Map<String, String> options = OptionsParser.parseOptions(args);

        // Start the application.
        this.startApplication(options, null);
    }

    public abstract void startApplication(Map<String, String> args, DeviceNode systemConfig);
}
