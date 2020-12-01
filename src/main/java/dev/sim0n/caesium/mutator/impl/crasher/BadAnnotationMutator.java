package dev.sim0n.caesium.mutator.impl.crasher;

import dev.sim0n.caesium.mutator.ClassMutator;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;
import dev.sim0n.caesium.util.wrapper.impl.FieldWrapper;
import dev.sim0n.caesium.util.wrapper.impl.MethodWrapper;
import joptsimple.internal.Strings;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;

/**
 * This generates a bunch of invisible annotations which will cause procyon to be very slow
 */
public class BadAnnotationMutator extends ClassMutator {
    private final String STRING = Strings.repeat('\n', 40);

    @Override
    public void handle(ClassWrapper wrapper) {
        ClassNode node = wrapper.node;

        if (node.invisibleAnnotations == null)
            node.invisibleAnnotations = new ArrayList<>();

        node.invisibleAnnotations.add(getAnnotationNode());

        wrapper.fields.stream()
                .map(f -> f.node)
                .forEach(f -> {
                    if (f.invisibleAnnotations == null)
                        f.invisibleAnnotations = new ArrayList<>();

                    f.invisibleAnnotations.add(getAnnotationNode());
                });

        wrapper.methods.stream()
                .map(m -> m.node)
                .forEach(m -> {
                    if (m.invisibleAnnotations == null)
                        m.invisibleAnnotations = new ArrayList<>();

                    m.invisibleAnnotations.add(getAnnotationNode());
                });
    }

    private AnnotationNode getAnnotationNode() {
        ++counter;
        return new AnnotationNode(STRING);
    }

    @Override
    public void handleFinish() {
        logger.info("Added {} annotations", counter);
    }
}
