package dev.sim0n.caesium.util;

import lombok.experimental.UtilityClass;

import java.util.Properties;

// Credits to @GitRowin
@UtilityClass
public class VersionUtil {

    /**
     * Gets the project version defined in pom.xml
     * @return The project version (or) debug if ran in intellij or exception is thrown
     */
    public String getVersion() {
        Properties properties = new Properties();

        try {
            properties.load(VersionUtil.class.getResourceAsStream("caesium.properties"));

            return properties.getProperty("version");
        } catch (Exception e) {
            return "DEBUG";
        }
    }
}
