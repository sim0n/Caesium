package dev.sim0n.caesium.manager;

import com.google.common.io.ByteStreams;
import dev.sim0n.caesium.Caesium;
import dev.sim0n.caesium.exception.CaesiumException;
import dev.sim0n.caesium.mutator.impl.ImageCrashMutator;
import dev.sim0n.caesium.util.ByteUtil;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Getter
public class ClassManager {
    private final Caesium caesium = Caesium.getInstance();

    private final MutatorManager mutatorManager = caesium.getMutatorManager();

    private final Logger logger = Caesium.getLogger();

    private final Map<ClassWrapper, String> classes = new HashMap<>();
    private final Map<String, byte[]> resources = new HashMap<>();

    private final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

    public void parseJar(File input) throws Exception {
        logger.info("loading classes...");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(input))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                byte[] data = ByteStreams.toByteArray(zis);

                String name = entry.getName();

                if (name.endsWith(".class")) {
                    ClassNode classNode = ByteUtil.parseClassBytes(data);

                    classes.put(new ClassWrapper(classNode), name);
                } else {
                    resources.put(name, data);
                }
            }
        }

        logger.info("loaded {} classes for mutation", classes.size());
        caesium.separator();
    }

    public void handleMutation() throws Exception {
        try (ZipOutputStream out = new ZipOutputStream(outputBuffer)) {
            Optional<ImageCrashMutator> imageCrashMutator = Optional.ofNullable(mutatorManager.getMutator(ImageCrashMutator.class));

            imageCrashMutator.ifPresent(crasher -> {
                ClassWrapper wrapper = crasher.getCrashClass();

                classes.put(wrapper, String.format("%s.class", wrapper.getNode().name));
            });

            classes.forEach((node, name) -> {
                mutatorManager.handleMutation(node);

                try {
                    out.putNextEntry(new ZipEntry(name));
                    out.write(ByteUtil.getClassBytes(node.getNode()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            resources.forEach((name, data) -> {
                try {
                    out.putNextEntry(new ZipEntry(name));
                    out.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        mutatorManager.handleMutationFinish();
    }

    /**
     * Exports {@param output}
     * @param output The obfuscated file to export
     * @throws CaesiumException If unable to write output data
     */
    public void exportJar(File output) throws CaesiumException {
        try {
            FileOutputStream fos = new FileOutputStream(output);

            fos.write(outputBuffer.toByteArray());
            fos.close();
        } catch (IOException e) {
            throw new CaesiumException("Failed to write output data", e);
        }
    }
}
