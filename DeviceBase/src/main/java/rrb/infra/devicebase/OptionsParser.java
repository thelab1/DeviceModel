package rrb.infra.devicebase;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pobzeb
 */
public class OptionsParser {
    public static enum BaseOptionType {
        SYSTEM_CONFIG_OPTION("c", "config"),
        DEVICE_CONFIG_OPTION("dd", "descript");

        private String shortName;
        private String longName;
        private BaseOptionType(String shortName, String longName) {
            this.shortName = shortName;
            this.longName = longName;
        }

        public String getShortName() {
            return this.shortName;
        }

        public String getLongName() {
            return this.longName;
        }

        public static BaseOptionType getBaseOptionType(String opt) {
            for (BaseOptionType optType : values()) {
                if (optType.getShortName().equals(opt) || optType.getLongName().equals(opt)) return optType;
            }

            return null;
        }
    }

    public static Map<String, String> parseOptions(String[] args) {
        // Build an options map to return.
        HashMap<String, String> optionsMap = new HashMap<>();

        // Check to see if there are any arguments.
        if (args.length > 0) {
            String key, val;
            for (int argIdx = 0; argIdx < args.length; argIdx++) {
                // First, look for a key.
                if (args[argIdx].startsWith("-")) {
                    // This parameter is a key. Get the key name.
                    key = args[argIdx].substring(1);
                    if (key.startsWith("-")) key = key.substring(1);

                    // Default the base option types to short name.
                    key = BaseOptionType.getBaseOptionType(key) != null ? BaseOptionType.getBaseOptionType(key).getShortName() : key;
                    val = "";

                    // Check to see if the next parameter exists,
                    // and is a value.
                    if (argIdx + 1 < args.length && !args[argIdx + 1].startsWith("-")) {
                        // The next parameter is a value.
                        for (argIdx = argIdx + 1; argIdx < args.length; argIdx++) {
                            if (val.length() > 0) val += " ";
                            val += args[argIdx];

                            // Check the next arg.
                            if (argIdx + 1 >= args.length || args[argIdx + 1].startsWith("-")) {
                                // Next arg is not there or is another key.
                                break;
                            }
                        }
                    }

                    // Add this key and value.
                    optionsMap.put(key, val);
                }
            }
        }

        // Return the options map.
        return optionsMap;
    }
}
