package dev.sim0n.caesium.mutator.impl;

import com.google.common.base.Strings;
import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.xml.bind.DatatypeConverter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ReferenceMutator extends ClassMutator {
    private int opcodeRandomKey;
    private int opcodeComparisonRandomKey;

    private String opcodeRandomKeyName;
    private String opcodeComparisonRandomKeyName;

    private String bsmSig;
    private String bsmName;

    private Handle bootstrapMethodHandle = null;

    @Override
    public void handle(ClassWrapper wrapper) {
        if (wrapper.getNode().version < V1_7) {
            return;
        }

        AtomicBoolean appliedInvoke = new AtomicBoolean(false);

        opcodeRandomKeyName = getRandomName();
        opcodeComparisonRandomKeyName = getRandomName();
        bsmName = getRandomName();

        opcodeRandomKey = random.nextInt();
        opcodeComparisonRandomKey = 184;

        // To make deobfuscation more difficult we'll generate fake parameters
        int extraParamsCount = random.nextInt(6);
        String extraDesc = Strings.repeat("Ljava/lang/Object;", extraParamsCount);

        bsmSig = String.format("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;%s)Ljava/lang/Object;", extraDesc);
        String targetName = wrapper.getNode().name;

        bootstrapMethodHandle = new Handle(H_INVOKESTATIC, targetName, bsmName, bsmSig);

        // don't want to deal with sub classes
        if (targetName.contains("$"))
            return;

        wrapper.getMethods().stream()
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
                                        params[1] = DatatypeConverter.printBase64Binary(insn.owner.replaceAll("/", ".").getBytes());
                                        params[2] = DatatypeConverter.printBase64Binary(insn.name.getBytes());
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

        if (extraParamsCount == 0) { // no extra parameters so insert default bootstrap function
            insertMethod(wrapper.getNode());
        } else {
            insertExtraParamMethod(wrapper.getNode());
        }
    }

    @Override
    public void handleFinish() {
        logger.info("hid {} method references", counter);
    }

    /**
     * Generates a random object to append to our random parameters
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
                return DatatypeConverter.printBase64Binary(getRandomName().getBytes());

            case 10:
                return -Math.abs(random.nextInt());
        }

        return 0;
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
        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitMethodInsn(INVOKESTATIC, "javax/xml/bind/DatatypeConverter", "parseBase64Binary", "(Ljava/lang/String;)[B", false);
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
        mv.visitMethodInsn(INVOKESTATIC, "javax/xml/bind/DatatypeConverter", "parseBase64Binary", "(Ljava/lang/String;)[B", false);
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
        mv.visitMethodInsn(INVOKESTATIC, "javax/xml/bind/DatatypeConverter", "parseBase64Binary", "(Ljava/lang/String;)[B", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitVarInsn(ALOAD, 10);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        mv.visitInsn(ARETURN);
        Label l8 = new Label();
        mv.visitLabel(l8);
        mv.visitMaxs(7, 11);
        mv.visitEnd();
    }

    public void insertMethod(ClassNode target) {
        final MethodVisitor mv = target.visitMethod(9, bsmName, bsmSig, null, new String[] { "java/lang/Exception" });
        mv.visitCode();
        final Label l0 = new Label();

        mv.visitLabel(l0);
        mv.visitLineNumber(35, l0);
        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitMethodInsn(INVOKESTATIC, "javax/xml/bind/DatatypeConverter", "parseBase64Binary", "(Ljava/lang/String;)[B", false);
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
        mv.visitMethodInsn(INVOKESTATIC, "javax/xml/bind/DatatypeConverter", "parseBase64Binary", "(Ljava/lang/String;)[B", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l5);
        mv.visitLineNumber(42, l5);
        mv.visitFrame(1, 3, new Object[] { "java/lang/Class", "java/lang/ClassLoader", "java/lang/invoke/MethodType" }, 0, null);
        mv.visitTypeInsn(NEW, "java/lang/invoke/MutableCallSite");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 7);
        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitMethodInsn(INVOKESTATIC, "javax/xml/bind/DatatypeConverter", "parseBase64Binary", "(Ljava/lang/String;)[B", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitVarInsn(ALOAD, 9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        mv.visitInsn(ARETURN);
        final Label l7 = new Label();
        mv.visitLabel(l7);
        mv.visitMaxs(7, 10);
        mv.visitEnd();
    }

}
