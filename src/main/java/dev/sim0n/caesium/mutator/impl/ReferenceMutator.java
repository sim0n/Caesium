package dev.sim0n.caesium.mutator.impl;

import com.google.common.base.Strings;
import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This hides method invocations with invokedynamics
 */
public class ReferenceMutator extends ClassMutator {
    // 0 = light
    // 1 = normal
    private int type = 1;

    private int opcodeRandomKey;
    private int opcodeComparisonRandomKey;

    private String opcodeRandomKeyName;
    private String opcodeComparisonRandomKeyName;

    private String bsmSig;
    private String bsmName;

    private String base64RandomTable;
    private String decodeMethodName;
    private String decodeMethodSig;
    private int extraParamsCount;

    private Handle bootstrapMethodHandle = null;

    @Override
    public void handle(ClassWrapper wrapper) {
        if (wrapper.node.version < V1_8)
            return;

        if ((wrapper.node.access & ACC_INTERFACE) != 0)
            return;

        AtomicBoolean appliedInvoke = new AtomicBoolean(false);

        opcodeRandomKeyName = getRandomName();
        opcodeComparisonRandomKeyName = getRandomName();
        bsmName = getRandomName();

        opcodeRandomKey = random.nextInt();
        opcodeComparisonRandomKey = 184;

        base64RandomTable = getRandomTable();
        decodeMethodName = getRandomName();
        byte encryptionKey = (byte) (opcodeRandomKey & 0xFF);

        // To make deobfuscation more difficult we'll generate fake parameters
        extraParamsCount = random.nextInt(6);
        String extraDesc = Strings.repeat("Ljava/lang/Object;", extraParamsCount);

        bsmSig = String.format("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;%s)Ljava/lang/Object;", extraDesc);
        decodeMethodSig = String.format("(Ljava/lang/String;B%s)[B", extraDesc);
        String targetName = wrapper.node.name;

        bootstrapMethodHandle = new Handle(H_INVOKESTATIC, targetName, bsmName, bsmSig);

        // don't want to deal with sub classes
        if (targetName.contains("$"))
            return;

        wrapper.methods.stream()
                .filter(MethodWrapper::hasInstructions)
                .forEach(method -> {
                    InsnList instructions = method.getInstructions();

                    Stream.of(method.getInstructions().toArray())
                            .filter(MethodInsnNode.class::isInstance)
                            .map(MethodInsnNode.class::cast)
                            // ignore these for now
                            .filter(insn -> !insn.owner.startsWith("[L"))
                            .filter(insn -> !insn.name.startsWith("<"))
                            .forEach(insn -> {
                                int opcode = insn.getOpcode();

                                String newSig = opcode == INVOKESTATIC ? insn.desc : insn.desc.replace("(", "(Ljava/lang/Object;");

                                Type origReturnType = Type.getReturnType(newSig);
                                Type[] args = Type.getArgumentTypes(newSig);

                                for (int i = 0; i < args.length; i++) {
                                    Type type = args[i];

                                    args[i] = type.getSort() == Type.OBJECT ? Type.getType(Object.class) : type;
                                }

                                newSig = Type.getMethodDescriptor(origReturnType, args);

                                switch (opcode) {
                                    case INVOKEVIRTUAL:
                                    case INVOKESTATIC:
                                    case INVOKEINTERFACE:
                                        Object[] params = new Object[4 + extraParamsCount];


                                        params[0] = extraParamsCount != 0 ? (opcode ^ opcodeRandomKey) : opcode;
                                        params[1] = base64Encode(insn.owner.replaceAll("/", ".").getBytes(), base64RandomTable, encryptionKey);
                                        params[2] = base64Encode(insn.name.getBytes(), base64RandomTable, encryptionKey);
                                        params[3] = insn.desc;

                                        // generate random parameter objects
                                        for (int i = 0; i < extraParamsCount; i++)
                                            params[4 + i] = getRandomObject();

                                        InvokeDynamicInsnNode invokeInsn = new InvokeDynamicInsnNode(getRandomName(), newSig, bootstrapMethodHandle, params);

                                        instructions.insert(insn, invokeInsn);

                                        if (origReturnType.getSort() == Type.ARRAY)
                                            instructions.insert(invokeInsn, new TypeInsnNode(CHECKCAST, origReturnType.getInternalName()));

                                        instructions.remove(insn);
                                        appliedInvoke.set(true);

                                        ++counter;
                                        break;
                                }
                            });
                });

        // we didn't replace any instructions with an invoke dynamic so don't add all this stuff
        if (!appliedInvoke.get())
            return;

        wrapper.addField(new FieldNode(ACC_PRIVATE + ACC_STATIC, opcodeRandomKeyName, "I", null, null));
        wrapper.addField(new FieldNode(ACC_PRIVATE + ACC_STATIC, opcodeComparisonRandomKeyName, "I", null, null));

        MethodNode clinit = wrapper.getClinit();

        InsnList insns = new InsnList();

        insns.add(new LdcInsnNode(opcodeRandomKey));
        insns.add(new FieldInsnNode(PUTSTATIC, targetName, opcodeRandomKeyName, "I"));
        insns.add(new LabelNode());
        insns.add(new LdcInsnNode(opcodeComparisonRandomKey));
        insns.add(new FieldInsnNode(PUTSTATIC, targetName, opcodeComparisonRandomKeyName, "I"));

        clinit.instructions.insert(insns);
        insertDecodeMethod(wrapper.node);
        if (extraParamsCount == 0) { // no extra parameters so insert default bootstrap function
            insertMethod(wrapper.node);
        } else {
            insertExtraParamMethod(wrapper.node);
        }
    }

    public String base64Encode(byte[] bytes, String table, byte opcodeRandomKey) {
        StringBuilder result = new StringBuilder();
        int length = bytes.length;
        int mod = 0;
        byte prev = 0;
        for (int i = 0; i < length; i++) {
            bytes[i] ^= opcodeRandomKey;
        }
        for (int i = 0; i < length; i++) {
            mod = i % 3;
            if (mod == 0) {
                result.append(table.charAt((bytes[i] >> 2) & 0x3F));
            } else if (mod == 1) {
                result.append(table.charAt((prev << 4 | bytes[i] >> 4 & 0x0F) & 0x3F));
            } else {
                result.append(table.charAt((bytes[i] >> 6 & 0x03 | prev << 2) & 0x3F));
                result.append(table.charAt(bytes[i] & 0x3F));
            }
            prev = bytes[i];
        }
        if (mod == 0) {
            result.append(table.charAt(prev << 4 & 0x3C));
            result.append("==");
        } else if (mod == 1) {
            result.append(table.charAt(prev << 2 & 0x3F));
            result.append("=");
        }
        return result.toString();
    }

    public String getRandomTable() {
        String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-/";
        List<Character> list = new ArrayList<Character>();
        for (int i = 0; i < base.length(); i++) {
            list.add(base.charAt(i));
        }
        Collections.shuffle(list);
        base = "";
        for (Character ch : list) {
            base += ch;
        }
        return base;
    }

    @Override
    public void handleFinish() {
        logger.info("Hid {} method references", counter);
    }

    /**
     * Generates a random object to append to our random parameters
     *
     * @return A random object
     */
    private Object getRandomObject() {
        switch (random.nextInt(10)) {
            case 0:
                return random.nextLong();

            case 1:
                return random.nextInt();

            case 2:
                return getRandomName();

            case 3:
                return random.nextFloat();

            case 4:
                return random.nextGaussian();

            case 5:
                return Float.floatToIntBits(random.nextFloat());

            case 6: {
                AtomicReference<String> s = new AtomicReference<>("");

                IntStream.range(0, 5 + random.nextInt(20))
                        .forEach(i -> s.updateAndGet(v -> v + (char) random.nextInt(60)));

                return s.get();
            }

            case 7: {
                return (byte) random.nextInt(Byte.MAX_VALUE);
            }

            case 8:
                return random.nextDouble() * 20F;

            case 9:
                return base64Encode(getRandomName().getBytes(), base64RandomTable, (byte) 0);

            case 10:
                return -Math.abs(random.nextInt());
        }

        return 0;
    }

    public void insertDecodeMethod(ClassNode target) {
        MethodVisitor mv = target.visitMethod(ACC_PUBLIC | ACC_STATIC, decodeMethodName, decodeMethodSig, null, null);
        mv.visitCode();
        Label label0 = new Label();
        mv.visitLabel(label0);
        mv.visitLineNumber(12, label0);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 2);
        Label label1 = new Label();
        mv.visitLabel(label1);
        mv.visitLineNumber(13, label1);
        mv.visitLdcInsn("");
        mv.visitVarInsn(ASTORE, 3);
        Label label2 = new Label();
        mv.visitLabel(label2);
        mv.visitLineNumber(14, label2);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 4);
        Label label3 = new Label();
        mv.visitLabel(label3);
        mv.visitFrame(Opcodes.F_APPEND, 3, new Object[]{Opcodes.INTEGER, "java/lang/String", Opcodes.INTEGER}, 0, null);
        mv.visitVarInsn(ILOAD, 4);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        Label label4 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, label4);
        Label label5 = new Label();
        mv.visitLabel(label5);
        mv.visitLineNumber(15, label5);
        mv.visitLdcInsn(base64RandomTable);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 4);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "indexOf", "(I)I", false);
        mv.visitInsn(I2B);
        mv.visitVarInsn(ISTORE, 2);
        Label label6 = new Label();
        mv.visitLabel(label6);
        mv.visitLineNumber(16, label6);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(ICONST_M1);
        Label label7 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, label7);
        Label label8 = new Label();
        mv.visitLabel(label8);
        mv.visitLineNumber(17, label8);
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn("000000");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 3);
        Label label9 = new Label();
        mv.visitJumpInsn(GOTO, label9);
        mv.visitLabel(label7);
        mv.visitLineNumber(19, label7);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toBinaryString", "(I)Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 5);
        Label label10 = new Label();
        mv.visitLabel(label10);
        mv.visitLineNumber(20, label10);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        mv.visitIntInsn(BIPUSH, 7);
        Label label11 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, label11);
        Label label12 = new Label();
        mv.visitLabel(label12);
        mv.visitLineNumber(21, label12);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 5);
        Label label13 = new Label();
        mv.visitJumpInsn(GOTO, label13);
        mv.visitLabel(label11);
        mv.visitLineNumber(22, label11);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/String"}, 0, null);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitJumpInsn(IF_ICMPNE, label13);
        Label label14 = new Label();
        mv.visitLabel(label14);
        mv.visitLineNumber(23, label14);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitInsn(ICONST_2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 5);
        mv.visitLabel(label13);
        mv.visitLineNumber(25, label13);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        mv.visitIntInsn(BIPUSH, 6);
        Label label15 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, label15);
        Label label16 = new Label();
        mv.visitLabel(label16);
        mv.visitLineNumber(26, label16);
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitLdcInsn("0");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 5);
        mv.visitJumpInsn(GOTO, label13);
        mv.visitLabel(label15);
        mv.visitLineNumber(28, label15);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitLabel(label9);
        mv.visitLineNumber(14, label9);
        mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        mv.visitIincInsn(4, 1);
        mv.visitJumpInsn(GOTO, label3);
        mv.visitLabel(label4);
        mv.visitLineNumber(31, label4);
        mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitLdcInsn("00000000");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
        Label label17 = new Label();
        mv.visitJumpInsn(IFEQ, label17);
        Label label18 = new Label();
        mv.visitLabel(label18);
        mv.visitLineNumber(32, label18);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(ISUB);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 3);
        mv.visitJumpInsn(GOTO, label4);
        mv.visitLabel(label17);
        mv.visitLineNumber(34, label17);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(IDIV);
        mv.visitIntInsn(NEWARRAY, T_BYTE);
        mv.visitVarInsn(ASTORE, 4);
        Label label19 = new Label();
        mv.visitLabel(label19);
        mv.visitLineNumber(35, label19);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 5);
        Label label20 = new Label();
        mv.visitLabel(label20);
        mv.visitFrame(Opcodes.F_APPEND, 2, new Object[]{"[B", Opcodes.INTEGER}, 0, null);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitInsn(ARRAYLENGTH);
        Label label21 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, label21);
        Label label22 = new Label();
        mv.visitLabel(label22);
        mv.visitLineNumber(36, label22);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(IMUL);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(IMUL);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
        mv.visitInsn(ICONST_2);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(Ljava/lang/String;I)Ljava/lang/Integer;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "byteValue", "()B", false);
        mv.visitInsn(BASTORE);
        Label label23 = new Label();
        mv.visitLabel(label23);
        mv.visitLineNumber(37, label23);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitInsn(DUP2);
        mv.visitInsn(BALOAD);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IXOR);
        mv.visitInsn(I2B);
        mv.visitInsn(BASTORE);
        Label label24 = new Label();
        mv.visitLabel(label24);
        mv.visitLineNumber(35, label24);
        mv.visitIincInsn(5, 1);
        mv.visitJumpInsn(GOTO, label20);
        mv.visitLabel(label21);
        mv.visitLineNumber(39, label21);
        mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitInsn(ARETURN);
        Label label25 = new Label();
        mv.visitLabel(label25);
        mv.visitMaxs(6, 6);
        mv.visitEnd();
    }

    public void insertExtraParamMethod(ClassNode target) {
        MethodVisitor mv = target.visitMethod(ACC_PUBLIC + ACC_STATIC, bsmName, bsmSig, null, new String[]{"java/lang/Exception"});
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(298, l0);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        mv.visitVarInsn(ISTORE, 8);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(300, l1);
        mv.visitVarInsn(ILOAD, 8);
        mv.visitFieldInsn(GETSTATIC, target.name, opcodeRandomKeyName, "I");
        mv.visitInsn(IXOR);
        mv.visitIntInsn(SIPUSH, 255);
        mv.visitInsn(IAND);
        mv.visitVarInsn(ISTORE, 8);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(302, l2);
        mv.visitFieldInsn(GETSTATIC, target.name, opcodeComparisonRandomKeyName, "I");
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitVarInsn(ASTORE, 7);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(304, l3);

        mv.visitFieldInsn(GETSTATIC, target.name, opcodeRandomKeyName, "I");
        mv.visitIntInsn(SIPUSH, 255);
        mv.visitInsn(IAND);
        mv.visitInsn(I2B);
        mv.visitVarInsn(ISTORE, 11);

        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);

        mv.visitVarInsn(ALOAD, 4);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitVarInsn(ILOAD, 11);
        for (int i = 0; i < extraParamsCount; i++) {
            int varOffest = i + 6;
            if (varOffest == 8) {
                mv.visitVarInsn(ILOAD, varOffest);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else mv.visitVarInsn(ALOAD, varOffest);
        }
        mv.visitMethodInsn(INVOKESTATIC, target.name, decodeMethodName, decodeMethodSig, false);

        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 9);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLineNumber(305, l4);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false);
        mv.visitVarInsn(ASTORE, 10);
        Label l5 = new Label();
        mv.visitLabel(l5);
        mv.visitLineNumber(307, l5);
        mv.visitVarInsn(ILOAD, 8);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        Label l6 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, l6);
        Label l7 = new Label();
        mv.visitLabel(l7);
        mv.visitLineNumber(308, l7);
        mv.visitTypeInsn(NEW, "java/lang/invoke/MutableCallSite");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);

        mv.visitVarInsn(ALOAD, 5);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitVarInsn(ILOAD, 11);
        for (int i = 0; i < extraParamsCount; i++) {
            int varOffest = i + 6;
            if (varOffest == 8) {
                mv.visitVarInsn(ILOAD, varOffest);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else mv.visitVarInsn(ALOAD, varOffest);
        }
        mv.visitMethodInsn(INVOKESTATIC, target.name, decodeMethodName, decodeMethodSig, false);

        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l6);
        mv.visitLineNumber(310, l6);
        mv.visitFrame(F_APPEND, 3, new Object[]{INTEGER, "java/lang/Class", "java/lang/invoke/MethodType"}, 0, null);
        mv.visitTypeInsn(NEW, "java/lang/invoke/MutableCallSite");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);

        mv.visitVarInsn(ALOAD, 5);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitVarInsn(ILOAD, 11);
        for (int i = 0; i < extraParamsCount; i++) {
            int varOffest = i + 6;
            if (varOffest == 8) {
                mv.visitVarInsn(ILOAD, varOffest);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else mv.visitVarInsn(ALOAD, varOffest);
        }
        mv.visitMethodInsn(INVOKESTATIC, target.name, decodeMethodName, decodeMethodSig, false);

        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        mv.visitInsn(ARETURN);
        Label l8 = new Label();
        mv.visitLabel(l8);
        mv.visitMaxs(7, 12);
        mv.visitEnd();
    }

    public void insertMethod(ClassNode target) {
        final MethodVisitor mv = target.visitMethod(9, bsmName, bsmSig, null, new String[]{"java/lang/Exception"});
        mv.visitCode();
        final Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(35, l0);

        mv.visitFieldInsn(GETSTATIC, target.name, opcodeRandomKeyName, "I");
        mv.visitIntInsn(SIPUSH, 255);
        mv.visitInsn(IAND);
        mv.visitInsn(I2B);
        mv.visitVarInsn(ISTORE, 10);

        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);

        mv.visitVarInsn(ALOAD, 4);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitVarInsn(ILOAD, 10);
        mv.visitMethodInsn(INVOKESTATIC, target.name, decodeMethodName, decodeMethodSig, false);

        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitVarInsn(ASTORE, 7);
        final Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(36, l2);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitVarInsn(ASTORE, 8);
        final Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(37, l3);
        mv.visitVarInsn(ALOAD, 6);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitVarInsn(ALOAD, 8);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false);
        mv.visitVarInsn(ASTORE, 9);
        final Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLineNumber(39, l4);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        mv.visitIntInsn(SIPUSH, 184);
        final Label l5 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, l5);
        final Label l6 = new Label();
        mv.visitLabel(l6);
        mv.visitLineNumber(40, l6);
        mv.visitTypeInsn(NEW, "java/lang/invoke/MutableCallSite");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);

        mv.visitVarInsn(ALOAD, 5);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitVarInsn(ILOAD, 10);
        mv.visitMethodInsn(INVOKESTATIC, target.name, decodeMethodName, decodeMethodSig, false);

        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l5);
        mv.visitLineNumber(42, l5);
        mv.visitFrame(1, 3, new Object[]{"java/lang/Class", "java/lang/ClassLoader", "java/lang/invoke/MethodType"}, 0, null);
        mv.visitTypeInsn(NEW, "java/lang/invoke/MutableCallSite");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);

        mv.visitVarInsn(ALOAD, 5);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitVarInsn(ILOAD, 10);
        mv.visitMethodInsn(INVOKESTATIC, target.name, decodeMethodName, decodeMethodSig, false);

        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        mv.visitInsn(ARETURN);
        final Label l7 = new Label();
        mv.visitLabel(l7);
        mv.visitMaxs(7, 11);
        mv.visitEnd();
    }

}
