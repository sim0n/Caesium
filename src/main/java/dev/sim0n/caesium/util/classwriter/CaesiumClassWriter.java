package dev.sim0n.caesium.util.classwriter;

import org.objectweb.asm.ClassWriter;

// wacky hack so we can compute frames without loading libraries (be careful with this, it doesn't always work)
public class CaesiumClassWriter extends ClassWriter {
    public CaesiumClassWriter() {
        super(COMPUTE_FRAMES);
    }

    @Override
    protected String getCommonSuperClass(String class1, String class2) {
        try {
            return super.getCommonSuperClass(class1, class2);
        } catch (RuntimeException e) {
            return "java/lang/Object";
        }
    }
}
