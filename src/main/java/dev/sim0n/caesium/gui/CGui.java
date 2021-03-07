/*
 * Created by JFormDesigner on Thu Nov 19 22:55:38 CET 2020
 */

package dev.sim0n.caesium.gui;

import com.formdev.flatlaf.FlatLightLaf;
import dev.sim0n.caesium.Caesium;
import dev.sim0n.caesium.PreRuntime;
import dev.sim0n.caesium.exception.CaesiumException;
import dev.sim0n.caesium.manager.MutatorManager;
import dev.sim0n.caesium.mutator.impl.*;
import dev.sim0n.caesium.mutator.impl.crasher.BadAnnotationMutator;
import dev.sim0n.caesium.mutator.impl.crasher.ImageCrashMutator;
import dev.sim0n.caesium.util.Dictionary;
import dev.sim0n.caesium.util.OSUtil;
import dev.sim0n.caesium.util.wrapper.impl.ClassWrapper;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This entire thing is a mess because it was automatically generated with
 * JFormDesigner
 */

public class CGui extends JFrame {

    public CGui() {
        FlatLightLaf.install();

        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        initComponents();
    }

    public static void main(String[] args) throws HeadlessException, IOException {

        PreRuntime.loadJavaRuntime();

        new CGui().setVisible(true);
    }

    private DefaultListModel<String> listModel = new DefaultListModel<>();

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY
        // //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - unknown
        tabbedPane1 = new JTabbedPane();
        panel1 = new JPanel();
        label1 = new JLabel();
        label2 = new JLabel();
        textField1 = new JTextField();
        textField2 = new JTextField();
        button1 = new JButton();
        button2 = new JButton();
        label3 = new JLabel();
        comboBox1 = new JComboBox();
        panel2 = new JPanel();
        checkBox1 = new JCheckBox();
        checkBox2 = new JCheckBox();
        label4 = new JLabel();
        comboBox2 = new JComboBox();
        checkBox3 = new JCheckBox();
        label5 = new JLabel();
        comboBox3 = new JComboBox();
        checkBox4 = new JCheckBox();
        checkBox5 = new JCheckBox();
        checkBox6 = new JCheckBox();
        label6 = new JLabel();
        comboBox4 = new JComboBox();
        panel3 = new JPanel();
        tabbedPane2 = new JTabbedPane();
        panel5 = new JPanel();
        scrollPane1 = new JScrollPane();
        list1 = new JList();
        button4 = new JButton();
        textField3 = new JTextField();
        button5 = new JButton();
        panel6 = new JPanel();
        panel4 = new JPanel();
        panel7 = new JPanel();
        label7 = new JLabel();
        comboBox5 = new JComboBox();
        button3 = new JButton();

        list1.setModel(listModel);

        // ======== this ========
        setTitle("Caesium Obfuscator");
        Container contentPane = getContentPane();
        contentPane.setLayout(null);

        // ======== tabbedPane1 ========
        {
            tabbedPane1.setFocusable(false);

            // ======== panel1 ========
            {
                panel1.setLayout(null);

                // ---- label1 ----
                label1.setText("Output");
                panel1.add(label1);
                label1.setBounds(new Rectangle(new Point(5, 45), label1.getPreferredSize()));

                // ---- label2 ----
                label2.setText("Input");
                panel1.add(label2);
                label2.setBounds(new Rectangle(new Point(5, 15), label2.getPreferredSize()));
                panel1.add(textField1);
                textField1.setBounds(55, 15, 220, textField1.getPreferredSize().height);
                panel1.add(textField2);
                textField2.setBounds(55, 45, 220, textField2.getPreferredSize().height);

                // ---- button1 ----
                button1.setText("...");
                button1.setFocusable(false);
                panel1.add(button1);
                button1.addActionListener(l -> {
                    JFileChooser chooser = new JFileChooser(".");

                    FileFilter jarFileFilter = new FileNameExtensionFilter("Jar Files", "jar");

                    chooser.setFileFilter(jarFileFilter);
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int response = chooser.showOpenDialog(button1);

                    if (response == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();

                        textField1.setText(file.getAbsolutePath());
                    }
                });
                button1.setBounds(285, 15, 50, 22);

                // ---- button2 ----
                button2.setText("...");
                button2.setFocusable(false);
                button2.addActionListener(l -> {
                    JFileChooser chooser = new JFileChooser(".");

                    int response = chooser.showOpenDialog(button2);

                    if (response == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();

                        textField2.setText(file.getAbsolutePath());
                    }
                });
                panel1.add(button2);
                button2.setBounds(285, 45, 50, 22);

                // ---- label3 ----
                label3.setText("Application type");
                panel1.add(label3);
                label3.setBounds(new Rectangle(new Point(10, 88), label3.getPreferredSize()));
                panel1.add(comboBox1);
                comboBox1.addItem("Self contained");
                comboBox1.addItem("Spigot plugin");

                comboBox1.setFocusable(false);
                comboBox1.setBounds(140, 85, 170, comboBox1.getPreferredSize().height);
            }
            tabbedPane1.addTab("Main", panel1);

            // ======== panel2 ========
            {
                panel2.setLayout(null);

                // ---- checkBox1 ----
                checkBox1.setText("String literal mutation");
                checkBox1.setFocusable(false);
                panel2.add(checkBox1);
                checkBox1.setBounds(new Rectangle(new Point(10, 10), checkBox1.getPreferredSize()));

                // ---- checkBox2 ----
                checkBox2.setText("Control flow mutation");
                checkBox2.setFocusable(false);
                panel2.add(checkBox2);
                checkBox2.setBounds(new Rectangle(new Point(10, 40), checkBox2.getPreferredSize()));

                // ---- label4 ----
                label4.setText("Reference Mutation");
                panel2.add(label4);
                label4.setBounds(new Rectangle(new Point(15, 113), label4.getPreferredSize()));
                comboBox2.addItem("Off");
                comboBox2.addItem("Light");
                comboBox2.addItem("Normal");
                comboBox2.setFocusable(false);
                panel2.add(comboBox2);
                comboBox2.setBounds(150, 110, 170, comboBox2.getPreferredSize().height);

                // ---- checkBox3 ----
                checkBox3.setText("Number mutation");
                checkBox3.setFocusable(false);
                panel2.add(checkBox3);
                checkBox3.setBounds(new Rectangle(new Point(10, 70), checkBox3.getPreferredSize()));

                // ---- label5 ----
                label5.setText("Local Variable tables");
                panel2.add(label5);
                label5.setBounds(new Rectangle(new Point(15, 158), label5.getPreferredSize()));
                comboBox3.addItem("Off");
                comboBox3.addItem("Remove");
                comboBox3.addItem("Rename");
                comboBox3.setFocusable(false);
                panel2.add(comboBox3);
                comboBox3.setBounds(150, 155, 170, comboBox3.getPreferredSize().height);

                checkBox6.setText("Polymorph");
                checkBox6.setFocusable(false);
                panel2.add(checkBox6);
                checkBox6.setBounds(new Rectangle(new Point(10, 240), checkBox6.getPreferredSize()));
                // ---- checkBox4 ----
                checkBox4.setText("Crasher");
                checkBox4.setFocusable(false);
                panel2.add(checkBox4);
                checkBox4.setBounds(new Rectangle(new Point(10, 270), checkBox4.getPreferredSize()));

                // ---- checkBox5 ----
                checkBox5.setText("Class Folder");
                checkBox5.setFocusable(false);
                panel2.add(checkBox5);
                checkBox5.setBounds(new Rectangle(new Point(90, 270), checkBox5.getPreferredSize()));

                // ---- label6 ----
                label6.setText("Line Number tables");
                panel2.add(label6);
                label6.setBounds(new Rectangle(new Point(15, 203), label6.getPreferredSize()));
                comboBox4.setFocusable(false);
                comboBox4.addItem("Off");
                comboBox4.addItem("Remove");
                comboBox4.addItem("Rename");
                panel2.add(comboBox4);
                comboBox4.setBounds(150, 200, 170, comboBox4.getPreferredSize().height);
            }
            tabbedPane1.addTab("Mutators", panel2);

            // ======== panel3 ========
            {
                panel3.setLayout(null);

                // ======== tabbedPane2 ========
                {
                    tabbedPane2.setFocusable(false);

                    // ======== panel5 ========
                    {
                        panel5.setLayout(null);

                        // ======== scrollPane1 ========
                        {
                            scrollPane1.setViewportView(list1);
                        }
                        panel5.add(scrollPane1);
                        scrollPane1.setBounds(1, 5, 324, 275);

                        // ---- button4 ----
                        button4.setText("Add");
                        button4.setFocusable(false);
                        panel5.add(button4);
                        button4.setBounds(178, 285, 58, button4.getPreferredSize().height);

                        // ---- textField3 ----
                        textField3.setText("");
                        panel5.add(textField3);
                        textField3.setBounds(2, 285, 173, 22);

                        button4.addActionListener(l -> {
                            if (textField3.getText().length() > 0) {
                                listModel.addElement(textField3.getText());
                                textField3.setText("");
                            }
                        });

                        // ---- button5 ----
                        button5.setText("Remove");
                        button5.setFocusable(false);
                        panel5.add(button5);

                        button5.addActionListener(l -> {
                            if (listModel.size() > 0 && list1.getSelectedIndex() != -1) {
                                listModel.removeElementAt(list1.getSelectedIndex());
                            }
                        });
                        button5.setBounds(238, 285, 85, button5.getPreferredSize().height);
                    }
                    tabbedPane2.addTab("Strings", panel5);

                    // ======== panel6 ========
                    {
                        panel6.setLayout(null);
                    }
                    tabbedPane2.addTab("References", panel6);
                }
                panel3.add(tabbedPane2);
                tabbedPane2.setBounds(5, 0, 330, 350);
            }
            tabbedPane1.addTab("Exclusions", panel3);

            // ======== panel4 ========
            {
                panel4.setLayout(null);

                // ---- label7 ----
                label7.setText("Dictionary");
                panel4.add(label7);
                label7.setBounds(new Rectangle(new Point(10, 18), label7.getPreferredSize()));
                comboBox5.addItem("abc");
                comboBox5.addItem("ABC");
                comboBox5.addItem("III");
                comboBox5.addItem("123");
                comboBox5.addItem("Wack");
                comboBox5.setSelectedIndex(3);
                comboBox5.setFocusable(false);

                panel4.add(comboBox5);
                comboBox5.setBounds(75, 15, 150, comboBox5.getPreferredSize().height);
            }
            tabbedPane1.addTab("Settings", panel4);
            // ======== panel5 ========
            {
                LibraryTab libraryTab = new LibraryTab();
                tabbedPane1.addTab("Dependencies", null, libraryTab, null);
                comboBox5.setBounds(75, 15, 150, comboBox5.getPreferredSize().height);
            }
        }
        contentPane.add(tabbedPane1);
        tabbedPane1.setBounds(5, 5, 345, 390);

        // ---- button3 ----
        button3.setText("Mutate");
        button3.addActionListener(l -> {
            Caesium caesium = new Caesium();

            File input = new File(textField1.getText());

            try {
                PreRuntime.loadInput(textField1.getText());
            } catch (CaesiumException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            PreRuntime.loadClassPath();
            PreRuntime.buildInheritance();
            if (!input.exists()) {
                JOptionPane.showMessageDialog(null, "Unable to find input file", "", JOptionPane.WARNING_MESSAGE);
                return;
            }

            File parent = new File(input.getParent());

            File output = new File(textField2.getText());

            if (output.exists()) {
                // we do it this way so we don't have to loop through a specified x amount of
                // times
                for (int i = 0; i < parent.listFiles().length; i++) {
                    String filePath = String.format("%s.BACKUP-%d", output.getAbsoluteFile(), i);
                    File file = new File(filePath);

                    if (!file.exists() && output.renameTo(new File(filePath))) {
                        output = new File(textField2.getText());
                        break;
                    }
                }
            }

            try {
                caesium.setDictionary(Dictionary.values()[comboBox5.getSelectedIndex()]);

                MutatorManager mutatorManager = caesium.getMutatorManager();

                // string
                StringMutator stringMutator = mutatorManager.getMutator(StringMutator.class);

                stringMutator.setEnabled(checkBox1.isSelected());

                Enumeration<String> elements = listModel.elements();

                while (elements.hasMoreElements()) {
                    stringMutator.getExclusions().add(elements.nextElement());
                }

                mutatorManager.getMutator(BadAnnotationMutator.class).setEnabled(checkBox4.isSelected());

                mutatorManager.getMutator(ControlFlowMutator.class).setEnabled(checkBox2.isSelected());
                mutatorManager.getMutator(NumberMutator.class).setEnabled(checkBox3.isSelected());

                mutatorManager.getMutator(PolymorphMutator.class).setEnabled(checkBox6.isSelected());

                mutatorManager.getMutator(ImageCrashMutator.class).setEnabled(checkBox4.isSelected());
                mutatorManager.getMutator(ClassFolderMutator.class).setEnabled(checkBox5.isSelected());

                {
                    int value = comboBox2.getSelectedIndex();

                    if (value > 0) {
                        ReferenceMutator mutator = mutatorManager.getMutator(ReferenceMutator.class);

                        mutator.setEnabled(true);
                    }
                }

                {
                    int value = comboBox4.getSelectedIndex();

                    if (value > 0) {
                        LineNumberMutator mutator = mutatorManager.getMutator(LineNumberMutator.class);

                        mutator.setType(value - 1);

                        mutator.setEnabled(true);
                    }
                }

                {
                    int value = comboBox3.getSelectedIndex();

                    if (value > 0) {
                        LocalVariableMutator mutator = mutatorManager.getMutator(LocalVariableMutator.class);

                        mutator.setType(value - 1);

                        mutator.setEnabled(true);
                    }
                }

                if (caesium.run(input, output) != 0) {
                    Caesium.getLogger().warn("Exited with non default exit code.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        contentPane.add(button3);
        button3.setBounds(new Rectangle(new Point(277, 400), button3.getPreferredSize()));

        contentPane.setPreferredSize(new Dimension(355, 430));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - unknown
    private JTabbedPane tabbedPane1;
    private JPanel panel1;
    private JLabel label1;
    private JLabel label2;
    private JTextField textField1;
    private JTextField textField2;
    private JButton button1;
    private JButton button2;
    private JLabel label3;
    private JComboBox comboBox1;
    private JPanel panel2;
    private JCheckBox checkBox1;
    private JCheckBox checkBox2;
    private JLabel label4;
    private JComboBox comboBox2;
    private JCheckBox checkBox3;
    private JLabel label5;
    private JComboBox comboBox3;
    private JCheckBox checkBox4;
    private JCheckBox checkBox5;
    private JCheckBox checkBox6;
    private JLabel label6;
    private JComboBox comboBox4;
    private JPanel panel3;
    private JTabbedPane tabbedPane2;
    private JPanel panel5;
    private JPanel panel7;
    private JScrollPane scrollPane1;
    private JList list1;
    private JButton button4;
    private JTextField textField3;
    private JButton button5;
    private JPanel panel6;
    private JPanel panel4;
    private JLabel label7;
    private JComboBox comboBox5;
    private JButton button3;
    // JFormDesigner - End of variables declaration //GEN-END:variables
}
