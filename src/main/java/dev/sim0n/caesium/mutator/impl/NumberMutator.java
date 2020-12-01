package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.ASMUtil;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class NumberMutator extends ClassMutator {
    private final InsnList deobfInsns = new InsnList();

    private int numbers;

    @Override
    public void handle(ClassWrapper wrapper) {
        numbers = 0;

        if ((wrapper.node.access & ACC_INTERFACE) != 0)
            return;

        List<InsnList> insns = new ArrayList<>();

        wrapper.methods.stream()
                .filter(MethodWrapper::hasInstructions)
                .forEach(method -> {
                    InsnList instructions = method.getInstructions();

                    insns.add(instructions);

                    AbstractInsnNode insn = instructions.getFirst();

                    do {
                        int opcode = insn.getOpcode();

                        if (opcode < ICONST_M1 || opcode > LDC)
                            continue;

                        ++numbers;
                    } while ((insn = insn.getNext()) != null);
                });

        if (numbers > 5000) // this is wayyy too much
            return;

        insns.forEach(instructions -> {
                    AbstractInsnNode insn = instructions.getFirst();

                    do {
                        int opcode = insn.getOpcode();

                        if (opcode < ICONST_M1 || opcode > LDC)
                            continue;

                        AbstractInsnNode newInsn;

                        if (opcode <= ICONST_5) {
                            newInsn = generateInsn(wrapper, opcode - ICONST_0);
                        } else {
                            switch (opcode) {
                                case BIPUSH:
                                case SIPUSH:
                                    newInsn = generateInsn(wrapper, ((IntInsnNode) insn).operand);
                                    break;

                                case LDC:
                                    LdcInsnNode ldc = (LdcInsnNode) insn;

                                    if (ldc.cst instanceof Number) {
                                        newInsn = generateInsn(wrapper, (Number) ldc.cst);
                                    } else {
                                        continue;
                                    }

                                    break;

                                default:
                                    continue;
                            }
                        }

                        instructions.set(insn, newInsn);

                        insn = newInsn;

                        ++counter;
                    } while ((insn = insn.getNext()) != null);
                });

        MethodNode clinit = wrapper.getClinit();

        clinit.instructions.insert(deobfInsns);

        deobfInsns.clear();
    }

    @Override
    public void handleFinish() {
        logger.info("Mutated {} numbers", counter);
    }

    /**
     * Generates a push instruction
     * @param clazz The class that owns the constants
     * @param value The value to generate a push instruction for
     * @return A push instruction
     */
    private AbstractInsnNode generateInsn(ClassWrapper clazz, Number value) {
        ClassNode owningClass = clazz.node;

        String fieldName = getRandomName();
        String className = value.getClass().getSimpleName();

        String desc = String.valueOf(className.charAt(0));

        switch (className) {
            case "Integer":
                deobfInsns.add(getIntPush((Integer) value));
                break;

            case "Long":
                // we need to change desc here because Long has a different desc
                desc = "J";
                deobfInsns.add(getLongPush((Long) value));
                break;

            case "Float":
                deobfInsns.add(getFloatPush((Float) value));
                break;

            case "Double":
                deobfInsns.add(getDoublePush((Double) value));
                break;

            default:
                throw new IllegalArgumentException();

        }

        deobfInsns.add(new FieldInsnNode(PUTSTATIC, owningClass.name, fieldName, desc));

        clazz.addField(new FieldNode(ACC_PRIVATE | ACC_STATIC, fieldName, desc, null, null));

        return new FieldInsnNode(GETSTATIC, owningClass.name, fieldName, desc);
    }

    // rotates left
    private int getRotatedInt(int value, int shiftDist) {
        return (value << shiftDist) | (value >>> -shiftDist);
    }

    private InsnList getIntPush(int value) {
        InsnList instructions = new InsnList();

        boolean reverse = random.nextBoolean();

        if (reverse) {
            instructions.add(ASMUtil.getOptimisedInt(Integer.reverse(value)));
            instructions.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "reverse", "(I)I", false));

            return instructions;
        }

        int shift = random.nextInt() & 255;

        // this will rotate back right
        instructions.add(ASMUtil.getOptimisedInt(getRotatedInt(value, shift)));
        instructions.add(ASMUtil.getOptimisedInt(shift));

        instructions.add(new InsnNode(IUSHR));
        instructions.add(ASMUtil.getOptimisedInt(getRotatedInt(value, shift)));
        instructions.add(ASMUtil.getOptimisedInt(shift));

        if (random.nextBoolean()) {
            // @see https://itzsomebody.xyz/2020/03/29/math-obfuscation-of-java-bytecode.html
            instructions.add(new InsnNode(ICONST_M1));
            instructions.add(new InsnNode(IXOR));
            instructions.add(new InsnNode(ICONST_1));
            instructions.add(new InsnNode(IADD));
        } else {
            instructions.add(new InsnNode(INEG));
        }

        instructions.add(new InsnNode(ISHL));
        instructions.add(new InsnNode(IOR));

        // x & 0xFFFFFFFF will always be the same value
        if (random.nextBoolean()) {
            instructions.add(new LdcInsnNode(0xFFFFFFFF));
            instructions.add(new InsnNode(IAND));
        }

        return instructions;
    }

    private InsnList getLongPush(long value) {
        InsnList instructions = new InsnList();

        instructions.add(new LdcInsnNode(Long.reverse(value)));
        instructions.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "reverse", "(J)J", false));

        return instructions;
    }

    private InsnList getFloatPush(float value) {
        InsnList instructions = new InsnList();

        instructions.add(getIntPush(Float.floatToIntBits(value)));
        instructions.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false));

        return instructions;
    }

    private InsnList getDoublePush(double value) {
        InsnList instructions = new InsnList();

        instructions.add(getLongPush(Double.doubleToLongBits(value)));
        instructions.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false));

        return instructions;
    }

}
