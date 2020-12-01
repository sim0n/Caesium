package dev.sim0n.caesium.util.wrapper.impl;

import dev.sim0n.caesium.util.wrapper.Wrapper;
import lombok.Getter;
import org.objectweb.asm.tree.FieldNode;

public class FieldWrapper implements Wrapper {
    public final FieldNode node;

    public FieldWrapper(FieldNode node) {
        this.node = node;
    }
}
