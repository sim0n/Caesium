package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;

import java.util.Collections;

// This will shuffle class members, however this can break reflection
public class ShuffleMutator extends ClassMutator {
    @Override
    public void handle(ClassWrapper wrapper) {
        Collections.shuffle(wrapper.node.fields, random);
        Collections.shuffle(wrapper.node.methods, random);

        counter += wrapper.fields.size() + wrapper.node.methods.size();
    }

    @Override
    public void handleFinish() {
        logger.info("Shuffled {} members", counter);
    }
}
