package dev.sim0n.caesium.util;

import java.io.IOException;


public class OSUtil {

    private static String osName = System.getProperty("os.name").toLowerCase();

    public static OS getCurrentOS() throws IOException {
        if (osName.contains("win")) {
            return OS.WINDOWS;
        } else if (osName.contains("mac")) {
            return OS.MAC;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return OS.UNIX;
        } else {
            System.exit(0);
            return null;
        }

    }
}
