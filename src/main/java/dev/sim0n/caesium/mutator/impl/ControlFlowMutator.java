package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;

import java.util.concurrent.ThreadLocalRandom;

public class ControlFlowMutator extends ClassMutator {
    private String jumpIntCondField;
    private String jumpBoolCondField;

    private int jumpIntCond;
    private boolean jumpBoolCond;

    @Override
    public void handle(ClassWrapper wrapper) {
        wrapper.methods.stream()
                .filter(MethodWrapper::hasInstructions)
                .forEach(method -> {
                    InsnList instructions = method.node.instructions;

                    obfuscateConditions(method, instructions);
                    addFakeJumps(wrapper, instructions);
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

    private void obfuscateConditions(MethodWrapper wrapper, InsnList instructions) {
        AbstractInsnNode insn = instructions.getFirst();

       /* do {
            int opcode = insn.getOpcode();

            if (opcode == IFEQ) {
                int var = ++wrapper.node.maxLocals;

                InsnList insns = new InsnList();

                insns.add(new InsnNode(ICONST_0));
                insns.add(new VarInsnNode(ISTORE, var));

                instructions.insert(insn, insns);
            }
        } while ((insn = insn.getNext()) != null);*/
    }

    /**
     * Replaces a jump instruction with a fake jump
     * @param wrapper The class wrapper
     * @param instructions The instructions of the method to obfuscate
     */
    private void addFakeJumps(ClassWrapper wrapper, InsnList instructions) {
        AbstractInsnNode insn = instructions.getFirst();

        do {
            int opcode = insn.getOpcode();

            if (opcode == GOTO) {
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

                jump.add(new FieldInsnNode(GETSTATIC, wrapper.node.name, useInt ? jumpIntCondField : jumpBoolCondField, useInt ? "I" : "Z"));

                int jumpOpcode = useInt ? jumpIntCond < 0 ? IFLT : IFGE : jumpBoolCond ? IFNE : IFEQ;

                jump.add(new JumpInsnNode(jumpOpcode, ((JumpInsnNode) insn).label));

                jump.add(new InsnNode(ACONST_NULL));
                jump.add(new InsnNode(ATHROW));

                AbstractInsnNode last = jump.getLast();

                instructions.insert(insn, jump);
                instructions.remove(insn);

                insn = last;

                ++counter;
            }
        } while ((insn = insn.getNext()) != null);
    }

    @Override
    public void handleFinish() {
        logger.info("Added {} fake jumps", counter);
    }
}
