package dev.sim0n.caesium.mutator.impl;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.ASMUtil;
import dev.sim0n.caesium.util.StringUtil;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import lombok.Getter;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class StringMutator extends ClassMutator {
    @Getter
    private final Set<String> exclusions = new HashSet<>();

    private int key1;

    private long key2;
    private long key3;

    private String stringField1 = getRandomName();
    private String stringField2 = getRandomName();
    private String keyField = getRandomName();
    private String decryptMethodName = getRandomName();

    private String bsmName = getRandomName();
    private String bsmSig = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/Object;";
    private Handle bsmHandle = null;

    private final List<String> strings = new ArrayList<>();

    @Override
    public void handle(ClassWrapper wrapper) {
        if ((wrapper.node.access & ACC_INTERFACE) != 0)
            return;

        ClassNode target = wrapper.node;

        getStrings(target);

        if (stringCount == 0)
            return;

        key1 = ThreadLocalRandom.current().nextInt(1, 125);

        key2 = random.nextLong();
        key3 = random.nextLong();

        stringField1 = getRandomName();
        stringField2 = getRandomName();

        keyField = getRandomName();

        decryptMethodName = getRandomName();

        bsmName = getRandomName();
        bsmHandle = new Handle(H_INVOKESTATIC, target.name, bsmName, bsmSig);

        {
            FieldVisitor fv = target.visitField(ACC_PRIVATE | ACC_STATIC, stringField1, "[Ljava/lang/String;", null, null);
            fv.visitEnd();
        }

        {
            FieldVisitor fv = target.visitField(ACC_PRIVATE | ACC_STATIC, stringField2, "[Ljava/lang/String;", null, null);
            fv.visitEnd();
        }

        {
            FieldVisitor fv = target.visitField(ACC_PRIVATE | ACC_STATIC, keyField, "J", null, null);
            fv.visitEnd();
        }

        MethodNode clinit = wrapper.getClinit();

        obfuscateStrings(target);

        makeCallSite(target);

        clinit.instructions.insert(getClinitInstructions(target));

        target.methods.add(makeDecryptMethod(target));

        target.methods.add(makeInit(target));
    }

    private int stringCount;

    public void getStrings(ClassNode owner) {
        stringCount = 0;

        owner.methods.stream()
                .filter(methodNode -> methodNode.instructions.size() > 0)
                .forEach(methodNode -> {
                    InsnList instructions = methodNode.instructions;
                    AbstractInsnNode insn = instructions.getFirst();

                    do {
                        if (insn.getOpcode() == LDC && ((LdcInsnNode) insn).cst instanceof String) {
                            ++stringCount;
                        }
                    } while ((insn = insn.getNext()) != null);
                });
    }

    int index;

    public void obfuscateStrings(ClassNode owner) {
        // let's make sure to clear every string before
        strings.clear();

        owner.methods.stream()
                .filter(methodNode -> methodNode.instructions.size() > 0)
                .forEach(methodNode -> {
                    InsnList instructions = methodNode.instructions;
                    AbstractInsnNode insn = instructions.getFirst();

                    do {
                        if (insn.getOpcode() == LDC) {
                            LdcInsnNode ldc = (LdcInsnNode) insn;

                            if (ldc.cst instanceof String) {
                                String string = (String) ldc.cst;

                                if (exclusions.contains(string))
                                    continue;

                                AbstractInsnNode ain = ASMUtil.getOptimisedInt(index);

                                InsnList newInstructions = new InsnList();

                                if (random.nextBoolean()) {
                                    newInstructions.add(new LdcInsnNode(key3));
                                    newInstructions.add(new LdcInsnNode(new Long(key1)));
                                    newInstructions.add(new InsnNode(LXOR));
                                } else {
                                    // x & 0xFFFFFFFF will always be the same value
                                    if (random.nextBoolean()) {
                                        newInstructions.add(new LdcInsnNode(0xFFFFFFFF));
                                        newInstructions.add(new InsnNode(IAND));
                                    }

                                    newInstructions.add(new LdcInsnNode(key3 ^ key1));
                                }

                                // We want to use an invokedynamic that references the decrypt function
                                newInstructions.add(new InvokeDynamicInsnNode(getRandomName(), "(IJ)Ljava/lang/String;", bsmHandle));

                                instructions.set(insn, insn = ain);
                                instructions.insert(insn, newInstructions);

                                strings.add(string);
                                ++index;
                                ++counter;
                            }
                        }
                    } while ((insn = insn.getNext()) != null);
                });

        // reset for next run
        index = 0;
    }

    @Override
    public void handleFinish() {
        logger.info("encrypted {} string literals", counter);
    }


    /**
     * Encrypts a string using DES
     * @param s The string to encrypt
     * @return The encrypted string
     */
    public String encryptString(String s) {
        try {
            long newKey = new Long(key3) ^ key2;

            Cipher instance = Cipher.getInstance("DES/CBC/PKCS5Padding");
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");

            byte[] keys = new byte[8];

            keys[0] = (byte) (newKey >>> 56);

            for (int i = 1; i < 8; ++i) {
                keys[i] = (byte) (newKey << i * 8 >>> 56);
            }

            instance.init(Cipher.ENCRYPT_MODE, keyFactory.generateSecret(new DESKeySpec(keys)), new IvParameterSpec(new byte[8]));

            return new String(Base64.getEncoder().encode(instance.doFinal((s.getBytes()))));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void makeCallSite(ClassNode cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC, bsmName, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();

        String typeName = "L" + cw.name.replace(".", "/") + ";";

        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
        mv.visitLabel(l0);
        mv.visitLineNumber(73, l0);
        mv.visitTypeInsn(NEW, "java/lang/invoke/MutableCallSite");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(Type.getType(typeName));
        mv.visitLdcInsn(decryptMethodName);
        mv.visitLdcInsn("(IJ)Ljava/lang/String;");
        mv.visitLdcInsn(Type.getType(typeName));
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
        mv.visitLabel(l1);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l2);
        mv.visitLineNumber(74, l2);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/lang/Exception"});
        mv.visitVarInsn(ASTORE, 3);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(75, l3);
        mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
        mv.visitInsn(DUP);
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitLdcInsn(cw.name + ":");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn(":");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodType", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
        mv.visitInsn(ATHROW);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitMaxs(7, 4);
        mv.visitEnd();
    }

    /**
     * Generates a decrypt method
     * @param owner The class owner
     * @return A string decryption method
     */
    public MethodNode makeDecryptMethod(ClassNode owner) {
        MethodNode mv = new MethodNode(ACC_PRIVATE + ACC_STATIC, decryptMethodName, "(IJ)Ljava/lang/String;", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(57, l3);
        mv.visitVarInsn(LLOAD, 1);
        mv.visitLdcInsn(new Long(key1));
        mv.visitInsn(LXOR);
        mv.visitVarInsn(LSTORE, 1);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLineNumber(58, l4);
        mv.visitVarInsn(LLOAD, 1);
        mv.visitLdcInsn(new Long(key2));
        mv.visitInsn(LXOR);
        mv.visitVarInsn(LSTORE, 1);
        Label l5 = new Label();
        mv.visitLabel(l5);
        mv.visitLineNumber(60, l5);
        mv.visitFieldInsn(GETSTATIC, owner.name, stringField1, "[Ljava/lang/String;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(AALOAD);
        Label l6 = new Label();
        mv.visitJumpInsn(IFNONNULL, l6);
        mv.visitLabel(l0);
        mv.visitLineNumber(65, l0);
        mv.visitLdcInsn("DES/CBC/PKCS5Padding");
        mv.visitMethodInsn(INVOKESTATIC, "javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;", false);
        mv.visitVarInsn(ASTORE, 3);
        Label l7 = new Label();
        mv.visitLabel(l7);
        mv.visitLineNumber(66, l7);
        mv.visitLdcInsn("DES");
        mv.visitMethodInsn(INVOKESTATIC, "javax/crypto/SecretKeyFactory", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;", false);
        mv.visitVarInsn(ASTORE, 4);
        mv.visitLabel(l1);
        mv.visitLineNumber(69, l1);
        Label l8 = new Label();
        mv.visitJumpInsn(GOTO, l8);
        mv.visitLabel(l2);
        mv.visitLineNumber(67, l2);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/lang/Exception"});
        mv.visitVarInsn(ASTORE, 5);
        Label l9 = new Label();
        mv.visitLabel(l9);
        mv.visitLineNumber(68, l9);
        mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(owner.name);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l8);
        mv.visitLineNumber(71, l8);
        mv.visitFrame(F_APPEND, 2, new Object[]{"javax/crypto/Cipher", "javax/crypto/SecretKeyFactory"}, 0, null);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitIntInsn(NEWARRAY, T_BYTE);
        mv.visitVarInsn(ASTORE, 5);
        Label l10 = new Label();
        mv.visitLabel(l10);
        mv.visitLineNumber(73, l10);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(LLOAD, 1);
        mv.visitIntInsn(BIPUSH, 56);
        mv.visitInsn(LUSHR);
        mv.visitInsn(L2I);
        mv.visitInsn(I2B);
        mv.visitInsn(BASTORE);
        Label l11 = new Label();
        mv.visitLabel(l11);
        mv.visitLineNumber(75, l11);
        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ISTORE, 6);
        Label l12 = new Label();
        mv.visitLabel(l12);
        mv.visitFrame(F_APPEND, 2, new Object[]{"[B", INTEGER}, 0, null);
        mv.visitVarInsn(ILOAD, 6);
        mv.visitIntInsn(BIPUSH, 8);
        Label l13 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l13);
        Label l14 = new Label();
        mv.visitLabel(l14);
        mv.visitLineNumber(76, l14);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitVarInsn(ILOAD, 6);
        mv.visitVarInsn(LLOAD, 1);
        mv.visitVarInsn(ILOAD, 6);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(IMUL);
        mv.visitInsn(LSHL);
        mv.visitIntInsn(BIPUSH, 56);
        mv.visitInsn(LUSHR);
        mv.visitInsn(L2I);
        mv.visitInsn(I2B);
        mv.visitInsn(BASTORE);
        Label l15 = new Label();
        mv.visitLabel(l15);
        mv.visitLineNumber(75, l15);
        mv.visitIincInsn(6, 1);
        mv.visitJumpInsn(GOTO, l12);
        mv.visitLabel(l13);
        mv.visitLineNumber(79, l13);
        mv.visitFrame(F_CHOP, 1, null, 0, null);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitInsn(ICONST_2);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitTypeInsn(NEW, "javax/crypto/spec/DESKeySpec");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 5);
        mv.visitMethodInsn(INVOKESPECIAL, "javax/crypto/spec/DESKeySpec", "<init>", "([B)V", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javax/crypto/SecretKeyFactory", "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;", false);
        mv.visitTypeInsn(NEW, "javax/crypto/spec/IvParameterSpec");
        mv.visitInsn(DUP);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitIntInsn(NEWARRAY, T_BYTE);
        mv.visitMethodInsn(INVOKESPECIAL, "javax/crypto/spec/IvParameterSpec", "<init>", "([B)V", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", false);
        Label l16 = new Label();
        mv.visitLabel(l16);
        mv.visitLineNumber(81, l16);
        mv.visitFieldInsn(GETSTATIC, owner.name, stringField1, "[Ljava/lang/String;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitTypeInsn(NEW, "java/lang/String");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;", false);
        mv.visitFieldInsn(GETSTATIC, owner.name, stringField2, "[Ljava/lang/String;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(AALOAD);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javax/crypto/Cipher", "doFinal", "([B)[B", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        mv.visitInsn(AASTORE);
        mv.visitLabel(l6);
        mv.visitLineNumber(84, l6);
        mv.visitFrame(F_CHOP, 3, null, 0, null);
        mv.visitFieldInsn(GETSTATIC, owner.name, stringField1, "[Ljava/lang/String;");
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(AALOAD);
        mv.visitInsn(ARETURN);
        Label l17 = new Label();
        mv.visitLabel(l17);

        mv.visitMaxs(8, 7);
        mv.visitEnd();

        return mv;
    }

    public InsnList getClinitInstructions(ClassNode owner) {
        InsnList instructions = new InsnList();

        LabelNode l0 = new LabelNode();
        instructions.add(l0);
        instructions.add(ASMUtil.getOptimisedInt(stringCount));
        instructions.add(new TypeInsnNode(ANEWARRAY, "java/lang/String"));
        instructions.add(new FieldInsnNode(PUTSTATIC, owner.name, stringField1, "[Ljava/lang/String;"));

        LabelNode l1 = new LabelNode();
        instructions.add(l1);
        instructions.add(ASMUtil.getOptimisedInt(stringCount));
        instructions.add(new TypeInsnNode(ANEWARRAY, "java/lang/String"));
        instructions.add(new FieldInsnNode(PUTSTATIC, owner.name, stringField2, "[Ljava/lang/String;"));

        LabelNode l2 = new LabelNode();
        instructions.add(l2);
        instructions.add(new MethodInsnNode(INVOKESTATIC, owner.name, "_init", "()V", false));

        return instructions;
    }

    public MethodNode makeInit(ClassNode owner) {
        MethodNode mv = new MethodNode(ACC_PRIVATE + ACC_STATIC, "_init","()V", null,null);

        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        //mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(95, l3);
        mv.visitLdcInsn(new Long(key3));
        mv.visitFieldInsn(PUTSTATIC, owner.name, keyField, "J");
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLineNumber(97, l4);
        mv.visitFieldInsn(GETSTATIC, owner.name, keyField, "J");
        mv.visitLdcInsn(new Long(key2));
        mv.visitInsn(LXOR);
        mv.visitVarInsn(LSTORE, 0);
        mv.visitLabel(l0);
        mv.visitLineNumber(100, l0);
        mv.visitLdcInsn("DES/CBC/PKCS5Padding");
        mv.visitMethodInsn(INVOKESTATIC, "javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;", false);
        mv.visitVarInsn(ASTORE, 2);
        Label l5 = new Label();
        mv.visitLabel(l5);
        mv.visitLineNumber(101, l5);
        mv.visitLdcInsn("DES");
        mv.visitMethodInsn(INVOKESTATIC, "javax/crypto/SecretKeyFactory", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;", false);
        mv.visitVarInsn(ASTORE, 3);
        Label l6 = new Label();
        mv.visitLabel(l6);
        mv.visitLineNumber(103, l6);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitIntInsn(NEWARRAY, T_BYTE);
        mv.visitVarInsn(ASTORE, 4);
        Label l7 = new Label();
        mv.visitLabel(l7);
        mv.visitLineNumber(105, l7);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(LLOAD, 0);
        mv.visitIntInsn(BIPUSH, 56);
        mv.visitInsn(LUSHR);
        mv.visitInsn(L2I);
        mv.visitInsn(I2B);
        mv.visitInsn(BASTORE);
        Label l8 = new Label();
        mv.visitLabel(l8);
        mv.visitLineNumber(107, l8);
        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ISTORE, 5);
        Label l9 = new Label();
        mv.visitLabel(l9);
        mv.visitFrame(F_FULL, 5, new Object[]{LONG, "javax/crypto/Cipher", "javax/crypto/SecretKeyFactory", "[B", INTEGER}, 0, new Object[]{});
        mv.visitVarInsn(ILOAD, 5);
        mv.visitIntInsn(BIPUSH, 8);
        Label l10 = new Label();
        mv.visitJumpInsn(IF_ICMPGE, l10);
        Label l11 = new Label();
        mv.visitLabel(l11);
        mv.visitLineNumber(108, l11);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitVarInsn(LLOAD, 0);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitInsn(IMUL);
        mv.visitInsn(LSHL);
        mv.visitIntInsn(BIPUSH, 56);
        mv.visitInsn(LUSHR);
        mv.visitInsn(L2I);
        mv.visitInsn(I2B);
        mv.visitInsn(BASTORE);
        Label l12 = new Label();
        mv.visitLabel(l12);
        mv.visitLineNumber(107, l12);
        mv.visitIincInsn(5, 1);
        mv.visitJumpInsn(GOTO, l9);
        mv.visitLabel(l10);
        mv.visitLineNumber(111, l10);
        mv.visitFrame(F_CHOP, 1, null, 0, null);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ICONST_2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitTypeInsn(NEW, "javax/crypto/spec/DESKeySpec");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitMethodInsn(INVOKESPECIAL, "javax/crypto/spec/DESKeySpec", "<init>", "([B)V", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javax/crypto/SecretKeyFactory", "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;", false);
        mv.visitTypeInsn(NEW, "javax/crypto/spec/IvParameterSpec");
        mv.visitInsn(DUP);
        mv.visitIntInsn(BIPUSH, 8);
        mv.visitIntInsn(NEWARRAY, T_BYTE);
        mv.visitMethodInsn(INVOKESPECIAL, "javax/crypto/spec/IvParameterSpec", "<init>", "([B)V", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", false);
        Label l13 = new Label();
        mv.visitLabel(l13);
        mv.visitLineNumber(113, l13);
        mv.visitInsn(ICONST_1);
        mv.visitVarInsn(ISTORE, 5);
        Label l14 = new Label();
        mv.visitLabel(l14);
        mv.visitLineNumber(115, l14);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 6);
        Label l15 = new Label();
        mv.visitLabel(l15);
        mv.visitFrame(F_APPEND, 2, new Object[]{INTEGER, INTEGER}, 0, null);
        mv.visitVarInsn(ILOAD, 6);
        mv.visitVarInsn(ILOAD, 5);
        mv.visitJumpInsn(IF_ICMPGE, l1);
        Label l16 = new Label();
        mv.visitLabel(l16);
        mv.visitLineNumber(116, l16);
        mv.visitVarInsn(ILOAD, 6);

        Label l17 = new Label();
        Label l18 = new Label();
        Label l19 = new Label();
        Label l20 = new Label();
        Label l21 = new Label();
        mv.visitTableSwitchInsn(0, 4, l20, new Label[]{l17, l18, l19, l20, l21});
        mv.visitLabel(l17);
        mv.visitLineNumber(118, l17);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        for (int i = 0; i < stringCount; i++) {
            mv.visitFieldInsn(GETSTATIC, owner.name, stringField2, "[Ljava/lang/String;");

            ASMUtil.visitOptimisedInt(mv, i);

            mv.visitLdcInsn(encryptString(strings.get(i)));
            mv.visitInsn(AASTORE);
        }

        Label l22 = new Label();
        mv.visitLabel(l22);
        mv.visitLineNumber(119, l22);
        mv.visitJumpInsn(GOTO, l20);
        mv.visitLabel(l18);
        mv.visitLineNumber(122, l18);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        // generate fake strings
        for (int i = 0; i < stringCount; i++) {
            mv.visitFieldInsn(GETSTATIC, owner.name, stringField2, "[Ljava/lang/String;");

            ASMUtil.visitOptimisedInt(mv, i);

            mv.visitLdcInsn(encryptString(strings.get(i) + StringUtil.getRandomString(2, 5, true)));
            mv.visitInsn(AASTORE);
        }

        Label l23 = new Label();
        mv.visitLabel(l23);
        mv.visitLineNumber(123, l23);
        mv.visitJumpInsn(GOTO, l20);
        mv.visitLabel(l19);
        mv.visitLineNumber(126, l19);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        // generate completely random fake strings
        for (int i = 0; i < stringCount * ThreadLocalRandom.current().nextInt(5, 20); i++) {
            mv.visitFieldInsn(GETSTATIC, owner.name, stringField2, "[Ljava/lang/String;");

            ASMUtil.visitOptimisedInt(mv, i);

            mv.visitLdcInsn(encryptString(StringUtil.getRandomString(5, 20, true)));
            mv.visitInsn(AASTORE);
        }

        Label l24 = new Label();
        mv.visitLabel(l24);
        mv.visitLineNumber(127, l24);
        mv.visitJumpInsn(GOTO, l20);
        mv.visitLabel(l21);
        mv.visitLineNumber(130, l21);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        // generate completely random fake strings
        for (int i = 0; i < Math.min(5, stringCount) * ThreadLocalRandom.current().nextInt(5, 20); i++) {
            mv.visitFieldInsn(GETSTATIC, owner.name, stringField2, "[Ljava/lang/String;");

            ASMUtil.visitOptimisedInt(mv, i);

            mv.visitLdcInsn(encryptString(StringUtil.getRandomString(5, 20, true)));
            mv.visitInsn(AASTORE);
        }

        mv.visitLabel(l20);
        mv.visitLineNumber(115, l20);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitIincInsn(6, 1);
        mv.visitJumpInsn(GOTO, l15);
        mv.visitLabel(l1);
        mv.visitLineNumber(137, l1);
        mv.visitFrame(F_FULL, 1, new Object[]{LONG}, 0, new Object[]{});
        Label l25 = new Label();
        mv.visitJumpInsn(GOTO, l25);
        mv.visitLabel(l2);
        mv.visitLineNumber(135, l2);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/lang/Exception"});
        mv.visitVarInsn(ASTORE, 2);
        Label l26 = new Label();
        mv.visitLabel(l26);
        mv.visitLineNumber(136, l26);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);
        mv.visitLabel(l25);
        mv.visitLineNumber(138, l25);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitInsn(RETURN);
        Label l27 = new Label();
        mv.visitLabel(l27);

        mv.visitMaxs(6, 7);
        mv.visitEnd();

        return mv;
    }

}
