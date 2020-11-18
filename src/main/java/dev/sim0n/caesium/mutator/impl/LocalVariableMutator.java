package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import org.objectweb.asm.tree.MethodNode;

public class LocalVariableMutator extends ClassMutator {
    @Override
    public void handle(ClassWrapper wrapper) {
        wrapper.getMethods().stream()
                .filter(MethodWrapper::hasInstructions)
                .forEach(method -> {
                    MethodNode node = method.getNode();

                    if (node.localVariables != null && node.localVariables.size() > 0)
                        node.localVariables.forEach(var -> var.name = getRandomName());

                    if (node.parameters != null && node.parameters.size() > 0)
                        node.parameters.forEach(parameter -> parameter.name = getRandomName());

                    ++counter;
                });
    }

    @Override
    public void handleFinish() {
        logger.info("renamed {} local variables & parameters", counter);
    }
}
