package dev.sim0n.caesium;

import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import dev.sim0n.caesium.exception.CaesiumException;
import dev.sim0n.caesium.gui.LibraryTab;
import dev.sim0n.caesium.util.OSUtil;
import dev.sim0n.caesium.util.classwriter.ClassTree;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;

public class PreRuntime {
    private static Map<String, ClassWrapper> classPath = new HashMap<>();;
    private static Map<String, ClassWrapper> classes = new HashMap<>();
    private static Map<String, ClassTree> hierarchy = new HashMap<>();
    public static DefaultListModel<String> libraries = new DefaultListModel<>();

    public static void loadJavaRuntime() throws HeadlessException, IOException {
        String path;
        switch (OSUtil.getCurrentOS()) {
        case WINDOWS:
            path = System.getProperty("sun.boot.class.path");
            if (path != null) {
                String[] pathFiles = path.split(";");
                for (String lib : pathFiles) {
                    if (lib.endsWith(".jar")) {
                        libraries.addElement(lib);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "rt.jar was not found, you need to add it manually.",
                        "Runtime Error", JOptionPane.ERROR_MESSAGE);
            }
            break;

        case UNIX:
        case MAC:
            path = System.getProperty("sun.boot.class.path");
            if (path != null) {
                String[] pathFiles = path.split(":");
                for (String lib : pathFiles) {
                    if (lib.endsWith(".jar")) {
                        libraries.addElement(lib);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "rt.jar was not found, you need to add it manually.",
                        "Runtime Error", JOptionPane.ERROR_MESSAGE);
            }
            break;
        default:
            break;
        }
    }

    public static void loadInput(String inputFile) throws CaesiumException {
        File input = new File(inputFile);
        if (input.exists()) {
            // Logger.info(String.format("Loading input \"%s\".", input.getAbsolutePath()));
            try {
                ZipFile zipFile = new ZipFile(input);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        if (entry.getName().endsWith(".class")) {
                            try {
                                ClassReader cr = new ClassReader(zipFile.getInputStream(entry));
                                ClassNode classNode = new ClassNode();
                                cr.accept(classNode, ClassReader.SKIP_FRAMES);
                                ClassWrapper classWrapper = new ClassWrapper(classNode, false);
                                classPath.put(classWrapper.originalName, classWrapper);
                                classes.put(classWrapper.originalName, classWrapper);
                            } catch (Throwable ignored) {

                            }
                        }
                    }
                }
                zipFile.close();
            } catch (ZipException e) {

                throw new CaesiumException(
                        String.format("Input file \"%s\" could not be opened as a zip file.", input.getAbsolutePath()),
                        e);
            } catch (IOException e) {
                throw new CaesiumException(String.format(
                        "IOException happened while trying to load classes from \"%s\".", input.getAbsolutePath()), e);
            }
        } else {
            throw new CaesiumException(String.format("Unable to find \"%s\".", input.getAbsolutePath()), null);
        }
    }

    public static void loadClassPath() {
        ArrayList<String> libs;

        libs = Collections.list(libraries.elements());
        for (String s : libs) {
            File file = new File(s);
            if (file.exists()) {
                System.out.print(String.format("Loading library \"%s\".", file.getAbsolutePath()));
                try {
                    ZipFile zipFile = new ZipFile(file);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                            try {
                                ClassReader cr = new ClassReader(zipFile.getInputStream(entry));
                                ClassNode classNode = new ClassNode();
                                cr.accept(classNode,
                                        ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                                ClassWrapper classWrapper = new ClassWrapper(classNode, true);

                                classPath.put(classWrapper.originalName, classWrapper);
                            } catch (Throwable t) {
                                // Don't care.
                            }
                        }
                    }
                    zipFile.close();
                } catch (ZipException e) {
                    // Logger.info(
                    // String.format("Library \"%s\" could not be opened as a zip file.",
                    // file.getAbsolutePath()));
                    e.printStackTrace();
                } catch (IOException e) {
                    // Logger.info(String.format("IOException happened while trying to load classes
                    // from \"%s\".",
                    // file.getAbsolutePath()));
                    e.printStackTrace();
                }
            } else {
                // Logger.info(String.format("Library \"%s\" could not be found and will be
                // ignored.",
                // file.getAbsolutePath()));
            }

        }
    }

    public static void buildHierarchy(ClassWrapper classWrapper, ClassWrapper sub) throws CaesiumException {
        if (hierarchy.get(classWrapper.node.name) == null) {
            ClassTree tree = new ClassTree(classWrapper);
            if (classWrapper.node.superName != null) {
                tree.parentClasses.add(classWrapper.node.superName);
                ClassWrapper superClass = classPath.get(classWrapper.node.superName);
                if (superClass == null)
                    throw new CaesiumException(classWrapper.node.superName + " is missing in the classpath.", null);
                buildHierarchy(superClass, classWrapper);
            }
            if (classWrapper.node.interfaces != null && !classWrapper.node.interfaces.isEmpty()) {
                for (String s : classWrapper.node.interfaces) {
                    tree.parentClasses.add(s);
                    ClassWrapper interfaceClass = classPath.get(s);
                    if (interfaceClass == null)
                        throw new CaesiumException(s + " is missing in the classpath.", null);

                    buildHierarchy(interfaceClass, classWrapper);
                }
            }
            hierarchy.put(classWrapper.node.name, tree);
        }
        if (sub != null) {
            hierarchy.get(classWrapper.node.name).subClasses.add(sub.node.name);
        }
    }

    public static void buildInheritance() {
        classes.values().forEach(classWrapper -> {
            try {
                buildHierarchy(classWrapper, null);
            } catch (CaesiumException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }

    public static Map<String, ClassWrapper> getClassPath() {
        return classPath;
    }

    public static Map<String, ClassWrapper> getClasses() {
        return classes;
    }

    public static Map<String, ClassTree> getHierarchy() {
        return hierarchy;
    }

}
