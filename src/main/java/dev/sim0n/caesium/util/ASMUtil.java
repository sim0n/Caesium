package dev.sim0n.caesium.util;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

@UtilityClass
public class ASMUtil implements Opcodes {

    public AbstractInsnNode getOptimisedInt(int value) {
        if (value >= -1 && value <= 5)
            return new InsnNode(value + 3);
        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE)
            return new IntInsnNode(BIPUSH, value);
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE)
            return new IntInsnNode(SIPUSH, value);

        return new LdcInsnNode(value);
    }

    public void visitOptimisedInt(MethodVisitor mv, int i) {
        if (i >= -1 && i <= 5)
            mv.visitInsn(i + 3);
        else if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE)
            mv.visitIntInsn(BIPUSH, i);
        else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE)
            mv.visitIntInsn(SIPUSH, i);
        else
            mv.visitLdcInsn(i);
    }

}
