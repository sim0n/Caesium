package dev.sim0n.caesium.util.wrapper.impl;

import dev.sim0n.caesium.util.wrapper.Wrapper;
import lombok.Getter;
import org.objectweb.asm.tree.FieldNode;

@Getter
public class FieldWrapper implements Wrapper {
    private final FieldNode node;

    public FieldWrapper(FieldNode node) {
        this.node = node;
    }
}
