package dev.sim0n.caesium.gui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import dev.sim0n.caesium.PreRuntime;

import java.awt.*;
import java.io.File;
import java.util.List;

public class LibraryTab extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 4696970146752109196L;

    public LibraryTab() {
        GridBagLayout gbl_this = new GridBagLayout();
        gbl_this.columnWidths = new int[] { 0, 0, 0, 0, 0 };
        gbl_this.rowHeights = new int[] { 0, 0, 0 };
        gbl_this.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
        gbl_this.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
        this.setLayout(gbl_this);

        JScrollPane ScrollPane = new JScrollPane();
        GridBagConstraints gbc_ScrollPane = new GridBagConstraints();
        gbc_ScrollPane.gridwidth = 4;
        gbc_ScrollPane.insets = new Insets(5, 5, 5, 5);
        gbc_ScrollPane.fill = GridBagConstraints.BOTH;
        gbc_ScrollPane.gridx = 0;
        gbc_ScrollPane.gridy = 0;
        this.add(ScrollPane, gbc_ScrollPane);

        JList<String> libList = new JList<>(PreRuntime.libraries);
        ScrollPane.setViewportView(libList);

        JTextField librariesField = new JTextField();
        GridBagConstraints gbc_librariesField = new GridBagConstraints();
        gbc_librariesField.insets = new Insets(0, 0, 5, 5);
        gbc_librariesField.fill = GridBagConstraints.HORIZONTAL;
        gbc_librariesField.gridx = 1;
        gbc_librariesField.gridy = 1;
        this.add(librariesField, gbc_librariesField);
        librariesField.setColumns(10);

        JButton addButton = new JButton("Add");
        GridBagConstraints gbc_addButton = new GridBagConstraints();
        gbc_addButton.insets = new Insets(0, 0, 5, 5);
        gbc_addButton.gridx = 2;
        gbc_addButton.gridy = 1;
        addButton.addActionListener((e) -> {
            if (librariesField.getText().length() > 1) {
                String path = librariesField.getText();

                File file = new File(path);

                if (file.exists()) {
                    PreRuntime.libraries.addElement(path);
                    librariesField.setText(null);
                } else {
                    JOptionPane.showMessageDialog(null, "Could not locate dependency.", "Caesium Dependency",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                final JFileChooser chooser = new JFileChooser();
                final FileNameExtensionFilter filter = new FileNameExtensionFilter("Java File", "jar");
                chooser.setFileFilter(filter);
                chooser.setMultiSelectionEnabled(true);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                int result = chooser.showOpenDialog(librariesField);

                if (result == 0) {
                    File[] libs = chooser.getSelectedFiles();
                    for (File lib : libs) {
                        PreRuntime.libraries.addElement(lib.toString());
                    }
                }
            }
        });

        this.add(addButton, gbc_addButton);

        JButton removeButton = new JButton("Remove");
        GridBagConstraints gbc_removeButton = new GridBagConstraints();
        gbc_removeButton.insets = new Insets(0, 0, 5, 5);
        gbc_removeButton.gridx = 3;
        gbc_removeButton.gridy = 1;
        removeButton.addActionListener((e) -> {
            List<String> removeList = libList.getSelectedValuesList();
            if (removeList.isEmpty())
                return;

            for (String s : removeList) {
                PreRuntime.libraries.removeElement(s);
            }
        });
        this.add(removeButton, gbc_removeButton);
    }

}
