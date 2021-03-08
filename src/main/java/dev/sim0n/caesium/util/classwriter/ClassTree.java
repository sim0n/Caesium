package dev.sim0n.caesium.util.classwriter;

import java.util.HashSet;
import java.util.Set;

import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;

public class ClassTree {
    /**
     * Attached ClassWrapper.
     */
    public final ClassWrapper classWrapper;

    /**
     * Names of classes this represented class inherits from.
     */
    public final Set<String> parentClasses = new HashSet<>();

    /**
     * Names of classes this represented class is inherited by.
     */
    public final Set<String> subClasses = new HashSet<>();

    /**
     * Creates a ClassTree object.
     *
     * @param classWrapper the ClassWraper attached to this ClassTree.
     */
    public ClassTree(ClassWrapper classWrapper) {
        this.classWrapper = classWrapper;
    }
}
