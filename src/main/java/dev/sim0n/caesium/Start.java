package dev.sim0n.caesium;

import com.google.common.collect.ImmutableList;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;

public class Start {
    public static void main(String[] args) {
        OptionParser optionParser = new OptionParser();
        OptionSpec<Void> help = optionParser
                .acceptsAll(ImmutableList.of("H", "help"), "show help menu")
                .forHelp();

        OptionSpec<File> input = optionParser
                .acceptsAll(ImmutableList.of("I", "input"), "the path of the input JAR file")
                .withRequiredArg()
                .required()
                .ofType(File.class);

        try {
            OptionSet options = optionParser.parse(args);

            if (options.has(help)) {
                optionParser.printHelpOn(System.out);
                return;
            }

            File inputFile = input.value(options);
            File outputFile = new File(inputFile.getName().replace(".jar", "-mutated.jar"));

            Caesium caesium = new Caesium();

            if (caesium.run(inputFile, outputFile) != 0) {
                Caesium.getLogger().warn("Exited with non default exit code.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
