package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import lombok.Setter;
import org.objectweb.asm.tree.MethodNode;

public class LocalVariableMutator extends ClassMutator {

    @Setter
    private int type = 0;

    @Override
    public void handle(ClassWrapper wrapper) {
        wrapper.methods.stream()
                .filter(MethodWrapper::hasInstructions)
                .map(m -> m.node)
                .forEach(method -> {
                    switch (type) {
                        case 0:
                            if (method.localVariables != null && method.localVariables.size() > 0)
                                method.localVariables.clear();

                            if (method.parameters != null && method.parameters.size() > 0)
                                method.parameters.clear();
                            break;

                        case 1:
                            if (method.localVariables != null && method.localVariables.size() > 0)
                                method.localVariables.forEach(var -> var.name = getRandomName());

                            if (method.parameters != null && method.parameters.size() > 0)
                                method.parameters.forEach(parameter -> parameter.name = getRandomName());
                            break;
                    }

                    ++counter;
                });
    }

    @Override
    public void handleFinish() {
        String output = "";

        switch (type) {
            case 0:
                output = "Removed {} local variables & parameters";
                break;

            case 1:
                output = "Renamed {} local variables & parameters";
                break;
        }

        logger.info(output, counter);
    }
}
