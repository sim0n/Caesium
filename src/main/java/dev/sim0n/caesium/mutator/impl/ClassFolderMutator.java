package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;

/**
 * This will turn all classes into directories by append a / to .class
 */
public class ClassFolderMutator extends ClassMutator {
    @Override
    public void handle(ClassWrapper wrapper) {
        ++counter;
    }

    @Override
    public void handleFinish() {
        logger.info("turned {} classes into folders", counter);
    }
}
