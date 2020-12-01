package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This will generate NOP instructions which (may) make it more annoying for the attacker to deal with
 */
public class PolymorphMutator extends ClassMutator {

    @Override
    public void handle(ClassWrapper wrapper) {
        wrapper.methods.stream()
                .filter(MethodWrapper::hasInstructions)
                .forEach(method -> {
                    InsnList instructions = method.getInstructions();

                    AtomicInteger index = new AtomicInteger();

                    Stream.of(instructions.toArray())
                            .forEach(insn -> {
                                if (index.getAndIncrement() % 6 == 0) {
                                    instructions.insertBefore(insn, new InsnNode(NOP));
                                    ++counter;
                                }
                            });
                });
    }

    @Override
    public void handleFinish() {
        logger.info("Inserted {} nop instructions", counter);
    }
}
