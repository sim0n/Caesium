package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import org.objectweb.asm.tree.*;

import java.util.concurrent.ThreadLocalRandom;

public class ControlFlowMutator extends ClassMutator {
    private String jumpIntCondField;
    private String jumpBoolCondField;

    private int jumpIntCond;
    private boolean jumpBoolCond;

    @Override
    public void handle(ClassWrapper wrapper) {
        wrapper.getMethods().stream()
                .filter(MethodWrapper::hasInstructions)
                .forEach(method -> {
                    InsnList instructions = method.getNode().instructions;

                    AbstractInsnNode insn = instructions.getFirst();

                    do {
                        if (insn.getOpcode() == GOTO) {

                            // we want to switch between comparing ints and booleans
                            boolean useInt = random.nextBoolean();

                            if (useInt ? jumpIntCondField == null : jumpBoolCondField == null) {
                                if (useInt) {
                                    jumpIntCondField = getRandomName();

                                    jumpIntCond = ThreadLocalRandom.current().nextInt();
                                } else {
                                    jumpBoolCondField = getRandomName();

                                    jumpBoolCond = random.nextBoolean();
                                }
                            }

                            InsnList jump = new InsnList();

                            jump.add(new FieldInsnNode(GETSTATIC, wrapper.getNode().name, useInt ? jumpIntCondField : jumpBoolCondField, useInt ? "I" : "Z"));

                            int opcode = useInt ? jumpIntCond < 0 ? IFLT : IFGE : jumpBoolCond ? IFNE : IFEQ;

                            jump.add(new JumpInsnNode(opcode, ((JumpInsnNode) insn).label));

                            jump.add(new InsnNode(ACONST_NULL));
                            jump.add(new InsnNode(ATHROW));

                            AbstractInsnNode last = jump.getLast();

                            instructions.insert(insn, jump);
                            instructions.remove(insn);

                            insn = last;

                            ++counter;
                        }
                    } while ((insn = insn.getNext()) != null);
                });

        // make the field conditions
        if (jumpIntCondField != null) {
            wrapper.addField(new FieldNode(ACC_PRIVATE | ACC_STATIC, jumpIntCondField, "I",
                    null, jumpIntCond));

            jumpIntCondField = null;
        }

        if (jumpBoolCondField != null) {
            wrapper.addField(new FieldNode(ACC_PRIVATE | ACC_STATIC, jumpBoolCondField, "Z",
                    null, jumpBoolCond));

            jumpBoolCondField = null;
        }
    }

    @Override
    public void handleFinish() {
        logger.info("added {} fake jumps", counter);
    }
}
