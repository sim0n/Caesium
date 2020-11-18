package dev.sim0n.caesium;

import com.google.common.base.Strings;
import dev.sim0n.caesium.manager.ClassManager;
import dev.sim0n.caesium.manager.MutatorManager;
import dev.sim0n.caesium.util.ByteUtil;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.security.SecureRandom;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Getter
public class Caesium {
    public static final String VERSION = "1.0.0";

    private static final String SEPARATOR = Strings.repeat("-", 30);

    @Getter
    private static final Logger logger = LogManager.getLogger();

    private final SecureRandom random = new SecureRandom();

    private static Optional<Caesium> instance;

    private final MutatorManager mutatorManager;
    private final ClassManager classManager;

    public Caesium() {
        instance = Optional.of(this);

        mutatorManager = new MutatorManager();
        classManager = new ClassManager();
    }

    public int run(File input, File output) throws Exception {
        checkNotNull(input, "input can't be null");
        checkNotNull(output, "output can't be null");

        separator();
        logger.info(String.format("caesium version %s", VERSION));
        separator();

        classManager.parseJar(input);
        classManager.handleMutation();
        classManager.exportJar(output);

        logger.info(String.format("successfully obfuscated target jar. %.3fkb -> %.3fkb", ByteUtil.bytesToKB(input.length()), ByteUtil.bytesToKB(output.length())));

        return 0;
    }

    public void separator() {
        logger.info(SEPARATOR);
    }

    public static Caesium getInstance() {
        return instance.orElseThrow(() -> new IllegalStateException("Caesium instance is null"));
    }
}
