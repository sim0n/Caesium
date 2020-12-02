package dev.sim0n.caesium.util;

import dev.sim0n.caesium.Caesium;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@UtilityClass
public class VersionUtil {

    public String getVersion() {
        try (InputStream is = Caesium.class.getResourceAsStream("/META-INF/maven/dev.sim0n/caesium/pom.properties")) {
            if (is != null) {
                Properties properties = new Properties();
                properties.load(is);

                String pomVersion = properties.getProperty("version");

                if (pomVersion != null) {
                    return pomVersion;
                }
            }
        } catch (IOException ignored) {
        }
        return "Unknown";
    }
}
