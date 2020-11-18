package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import org.objectweb.asm.tree.ClassNode;

/**
 * This inserts a class that will crash almost every gui based RE tool
 */
public class ImageCrashMutator extends ClassMutator {

    @Override
    public void handle(ClassWrapper wrapper) { }

    public ClassWrapper getCrashClass() {
        ClassNode classNode = new ClassNode();

        classNode.name = String.format("<html><img src=\"https:%s", getRandomName());

        classNode.access = ACC_PUBLIC;
        classNode.version = V1_5;

        return new ClassWrapper(classNode);
    }

    @Override
    public void handleFinish() {
        logger.info("inserted crash class");
    }
}
