package dev.sim0n.caesium.util.wrapper.impl;

import dev.sim0n.caesium.util.wrapper.Wrapper;
import lombok.Getter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.stream.Collectors;

public class ClassWrapper implements Wrapper {
    public final ClassNode node;

    public final List<MethodWrapper> methods;
    public final List<FieldWrapper> fields;

    public ClassWrapper(ClassNode node) {
        this.node = node;

        methods = node.methods.stream()
                .map(MethodWrapper::new)
                .collect(Collectors.toList());

        fields = node.fields.stream()
                .map(FieldWrapper::new)
                .collect(Collectors.toList());
    }

    /**
     * Gets the clinit method or creates one if it isn't present
     * @return The clinit method
     */
    public MethodNode getClinit() {
        return node.methods.stream()
                .filter(method -> method.name.equals("<clinit>"))
                .findFirst()
                .orElseGet(() -> {
                    MethodNode newClinit = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);

                    newClinit.instructions.add(new InsnNode(RETURN));

                    methods.add(new MethodWrapper(newClinit));
                    node.methods.add(newClinit);

                    return newClinit;
                });
    }

    public void addField(FieldNode fieldNode) {
        node.fields.add(fieldNode);

        fields.add(new FieldWrapper(fieldNode));
    }
}
