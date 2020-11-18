package dev.sim0n.caesium.util.wrapper.impl;

import dev.sim0n.caesium.util.wrapper.Wrapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

@Getter
@RequiredArgsConstructor
public class MethodWrapper implements Wrapper {
    private final MethodNode node;

    public int getMaxSize() {
        CodeSizeEvaluator evaluator = new CodeSizeEvaluator(null);

        node.accept(evaluator);

        return evaluator.getMaxSize();
    }

    public boolean hasInstructions() {
        return node.instructions != null && node.instructions.size() > 0;
    }

    public InsnList getInstructions() {
        return node.instructions;
    }
}
