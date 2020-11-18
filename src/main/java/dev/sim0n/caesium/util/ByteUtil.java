package dev.sim0n.caesium.util;

import dev.sim0n.caesium.util.classwriter.CaesiumClassWriter;
import lombok.experimental.UtilityClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

@UtilityClass
public class ByteUtil {

    /**
     * Converts a {@param bytes} to a {@link ClassNode}
     * @param bytes The byte array to convert into a {@link ClassNode}
     * @return A class node from {@param bytes}
     */
    public ClassNode parseClassBytes(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();

        reader.accept(classNode, 0);

        return classNode;
    }

    /**
     * Converts {@param classNode} to a byte array
     * @param classNode The class node to convert to a byte array
     * @return A byte array from {@param classNode}
     */
    public byte[] getClassBytes(ClassNode classNode) {
        CaesiumClassWriter classWriter = new CaesiumClassWriter();

        classWriter.newUTF8("caesium");
        classNode.accept(classWriter);

        return classWriter.toByteArray();
    }

    /**
     * Converts {@param bytes} to kb
     * @param bytes The bytes to convert
     * @return {@param bytes} in kb
     */
    public double bytesToKB(long bytes) {
        return bytes / 1024D;
    }
}
