package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import org.objectweb.asm.tree.ClassNode;

public class AttributeMutator extends ClassMutator {
    public AttributeMutator() {
        setEnabled(true);
    }

    @Override
    public void handle(ClassWrapper wrapper) {
        ClassNode node = wrapper.node;

    }

    @Override
    public void handleFinish() {

    }
}
