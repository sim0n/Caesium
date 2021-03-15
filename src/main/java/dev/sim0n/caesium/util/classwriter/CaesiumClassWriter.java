package dev.sim0n.caesium.util.classwriter;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import dev.sim0n.caesium.PreRuntime;
import dev.sim0n.caesium.exception.CaesiumException;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;

import javax.swing.*;

public class CaesiumClassWriter extends ClassWriter {
    public CaesiumClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if ("java/lang/Object".equals(type1) || "java/lang/Object".equals(type2))
            return "java/lang/Object";

        String first = null;
        try {
            first = deriveCommonSuperName(type1, type2);
        } catch (CaesiumException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String second = null;
        try {
            second = deriveCommonSuperName(type2, type1);
        } catch (CaesiumException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (!"java/lang/Object".equals(first))
            return first;

        if (!"java/lang/Object".equals(second))
            return second;

        try {
            return getCommonSuperClass(returnClazz(type1).superName, returnClazz(type2).superName);
        } catch (CaesiumException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return "java/lang/Object";
    }

    private String deriveCommonSuperName(String type1, String type2) throws CaesiumException {
        ClassNode first = returnClazz(type1);
        ClassNode second = returnClazz(type2);
        if (isAssignableFrom(type1, type2))
            return type1;
        else if (isAssignableFrom(type2, type1))
            return type2;
        else if (Modifier.isInterface(first.access) || Modifier.isInterface(second.access))
            return "java/lang/Object";
        else {
            do {
                type1 = first.superName;
                first = returnClazz(type1);
            } while (!isAssignableFrom(type1, type2));
            return type1;
        }
    }

    private ClassNode returnClazz(String ref) throws CaesiumException {
        ClassWrapper clazz = PreRuntime.getClassPath().get(ref);
        if (clazz == null) {
            JOptionPane.showMessageDialog(null, "Couldn't find " + ref + " in classpath.", "Error", JOptionPane.ERROR_MESSAGE);
            throw new CaesiumException(ref + " does not exist in classpath!", null);
        }

        return clazz.node;
    }

    private boolean isAssignableFrom(String type1, String type2) throws CaesiumException {
        if ("java/lang/Object".equals(type1))
            return true;

        if (type1.equals(type2))
            return true;

        returnClazz(type1);
        returnClazz(type2);
        ClassTree firstTree = getTree(type1);

        if (firstTree == null)
            throw new CaesiumException("Could not find " + type1 + " in the built class hierarchy", null);

        Set<String> allChildren = new HashSet<>();
        Deque<String> toProcess = new ArrayDeque<>(firstTree.subClasses);
        while (!toProcess.isEmpty()) {
            String s = toProcess.poll();
            if (allChildren.add(s)) {
                returnClazz(s);
                ClassTree tempTree = getTree(s);
                toProcess.addAll(tempTree.subClasses);
            }
        }
        return allChildren.contains(type2);
    }

    public ClassTree getTree(String ref) throws CaesiumException {
        if (!PreRuntime.getHierarchy().containsKey(ref)) {
            ClassWrapper wrapper = PreRuntime.getClassPath().get(ref);
            PreRuntime.buildHierarchy(wrapper, null);
        }

        return PreRuntime.getHierarchy().get(ref);
    }
}
