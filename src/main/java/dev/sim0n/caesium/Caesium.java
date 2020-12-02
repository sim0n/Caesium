package dev.sim0n.caesium;

import com.google.common.base.Strings;
import dev.sim0n.caesium.manager.ClassManager;
import dev.sim0n.caesium.manager.MutatorManager;
import dev.sim0n.caesium.util.ByteUtil;
import dev.sim0n.caesium.util.Dictionary;
import dev.sim0n.caesium.util.VersionUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.security.SecureRandom;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Getter
public class Caesium {
    public static final String VERSION = VersionUtil.getVersion();

    private static final String SEPARATOR = Strings.repeat("-", 30);

    @Getter
    private static final Logger logger = LogManager.getLogger();

    private final SecureRandom random = new SecureRandom();

    private static Optional<Caesium> instance;

    private final MutatorManager mutatorManager;
    private final ClassManager classManager;

    @Setter
    private Dictionary dictionary = Dictionary.NUMBERS;

    public Caesium() {
        instance = Optional.of(this);

        mutatorManager = new MutatorManager();
        classManager = new ClassManager();
    }

    public int run(File input, File output) throws Exception {
        checkNotNull(input, "Input can't be null");
        checkNotNull(output, "Output can't be null");

        separator();
        logger.info(String.format("Caesium version %s", VERSION));
        separator();

        classManager.parseJar(input);
        classManager.handleMutation();
        classManager.exportJar(output);

        double inputKB = ByteUtil.bytesToKB(input.length());
        double outputKB = ByteUtil.bytesToKB(output.length());

        logger.info(String.format("Successfully obfuscated target jar. %.3fkb -> %.3fkb", inputKB, outputKB));

        return 0;
    }

    public void separator() {
        logger.info(SEPARATOR);
    }

    public static Caesium getInstance() {
        return instance.orElseThrow(() -> new IllegalStateException("Caesium instance is null"));
    }
}
