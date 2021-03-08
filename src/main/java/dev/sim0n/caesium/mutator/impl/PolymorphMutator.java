package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This will generate useless instructions which (may) make it more annoying for the attacker to deal with
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
                                if (insn instanceof LdcInsnNode) {
                                    if (random.nextBoolean()) {
                                        instructions.insertBefore(insn, new IntInsnNode(BIPUSH, ThreadLocalRandom.current().nextInt(-64, 64)));
                                        instructions.insertBefore(insn, new InsnNode(POP));

                                        ++counter;
                                    }
                                } else if (index.getAndIncrement() % 6 == 0) {
                                    if (random.nextFloat() > 0.6) {
                                        instructions.insertBefore(insn, new IntInsnNode(BIPUSH, ThreadLocalRandom.current().nextInt(-27, 37)));
                                        instructions.insertBefore(insn, new InsnNode(POP));
                                    } else {
                                        instructions.insertBefore(insn, new InsnNode(NOP));
                                    }

                                    ++counter;
                                }
                            });
                });
    }

    @Override
    public void handleFinish() {
        logger.info("Inserted {} useless instructions", counter);
    }
}
