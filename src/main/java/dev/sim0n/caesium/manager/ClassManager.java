package dev.sim0n.caesium.manager;

import com.google.common.io.ByteStreams;
import dev.sim0n.caesium.Caesium;
import dev.sim0n.caesium.exception.CaesiumException;
import dev.sim0n.caesium.mutator.impl.ClassFolderMutator;
import dev.sim0n.caesium.mutator.impl.crasher.ImageCrashMutator;
import dev.sim0n.caesium.util.ByteUtil;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import lombok.Getter;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
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
        logger.info("Loading classes...");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(input))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                byte[] data = ByteStreams.toByteArray(zis);

                String name = entry.getName();

                if (name.endsWith(".class")) {
                    ClassNode classNode = ByteUtil.parseClassBytes(data);

                    classes.put(new ClassWrapper(classNode, false), name);
                } else {
                    if (name.equals("META-INF/MANIFEST.MF")) {
                        String manifest = new String(data);

                        // delete this line
                        manifest = manifest.substring(0, manifest.length() - 2);

                        // watermark the manifest
                        manifest += String.format("Obfuscated-By: Caesium %s\r\n", Caesium.VERSION);

                        data = manifest.getBytes();
                    }

                    resources.put(name, data);
                }
            }
        }

        logger.info("Loaded {} classes for mutation", classes.size());
        caesium.separator();
    }

    public void handleMutation() throws Exception {
        try (ZipOutputStream out = new ZipOutputStream(outputBuffer)) {
            Optional<ImageCrashMutator> imageCrashMutator = Optional.ofNullable(mutatorManager.getMutator(ImageCrashMutator.class));
            Optional<ClassFolderMutator> classFolderMutator = Optional.ofNullable(mutatorManager.getMutator(ClassFolderMutator.class));

            AtomicBoolean hideClasses = new AtomicBoolean(classFolderMutator.isPresent() && classFolderMutator.get().isEnabled());

            imageCrashMutator.ifPresent(crasher -> {
                if (!crasher.isEnabled())
                    return;

                ClassWrapper wrapper = crasher.getCrashClass();

                classes.put(wrapper, String.format("%s.class", wrapper.node.name));
            });

            classes.forEach((node, name) -> {
                mutatorManager.handleMutation(node);

                try {
                    if (hideClasses.get()) // turn them into folders
                        name += "/";

                    out.putNextEntry(new ZipEntry(name));
                    out.write(ByteUtil.getClassBytes(node.node));

                    if (hideClasses.get()) { // generate a bunch of fake classes
                        String finalName = name;

                        IntStream.range(0, 1 + caesium.getRandom().nextInt(10))
                                .forEach(i -> {
                                    try {
                                        out.putNextEntry(new ZipEntry(String.format("%scaesium_%d.class", finalName, i ^ 27)));
                                        out.write(new byte[] { 0 });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                    }
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
