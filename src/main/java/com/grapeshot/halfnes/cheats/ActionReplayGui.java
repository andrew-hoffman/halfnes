/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.cheats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import static com.grapeshot.halfnes.utils.*;

/**
 * Dialog box to configure and apply Pro Action Replay cheat codes.
 *
 * @author Thomas Lorblanches
 */
public class ActionReplayGui extends javax.swing.JDialog {

    private static final int ADDRESS_LENGTH = 6;
    private static final int DATA_LENGTH = 2;
    private static final int CODE_LENGTH = ADDRESS_LENGTH + DATA_LENGTH;
    private Patch patch = null;
    private final ActionReplay actionReplay;

    /**
     * Creates new form ActionReplayGui
     */
    public ActionReplayGui(java.awt.Frame parent, boolean modal, ActionReplay actionReplay) {
        super(parent, modal);
        initComponents();
        setLocationRelativeTo(null);
        textCode.requestFocusInWindow();
        textCode.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
                if (str == null) {
                    return;
                }
                if (((getLength() + str.length()) <= CODE_LENGTH) && (str.matches("[0-9a-fA-FAEGIKLNOPSTUVXYZaegiklnopstuvxyz]*"))) {
                    super.insertString(offset, str, attr);
                }
            }
        });
        textFindData.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
                if (str == null) {
                    return;
                }
                if (((getLength() + str.length()) <= 3) && (str.matches("[0-9]*"))) {
                    super.insertString(offset, str, attr);
                }
            }
        });
        listPossibleAddresses.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                if (listPossibleAddresses.getSelectedIndex() == -1) {
                    btnTry.setEnabled(false);
                } else {
                    btnTry.setEnabled(true);
                }
            }
        });
        this.actionReplay = actionReplay;
        updateCurrentCodesList();
        updateListPossibleCodes();
    }

    private void updateCurrentCodesList() {
        final Patch[] patches = actionReplay.getPatches().values().toArray(new Patch[0]);
        final String[] patchesStr = new String[patches.length];
        for (int i = 0; i < patches.length; i++) {
            patchesStr[i] = patches[i].toString().toUpperCase();
        }
        currentCodesList.setListData(patchesStr);
    }

    private void updateListPossibleCodes() {
        List<String> adrStr = new ArrayList<>();
        for (Integer adr : actionReplay.getFoundAddresses()) {
            String str = Integer.toHexString(adr).toUpperCase();
            while (str.length() < ADDRESS_LENGTH) {
                str = "0" + str;
            }
            adrStr.add(str);
        }
        listPossibleAddresses.setListData(adrStr.toArray());
    }

    private boolean isCodeValid() {
        final String code = textCode.getText();
        if ((code.length() == 8) && code.matches("[0-9a-fA-F]*")) {
            btnApply.setEnabled(true);
            final int data = (Integer.parseInt(textCode.getText().substring(textCode.getText().length() - 2), 16) & 0xFF);
            final int address = Integer.parseInt(textCode.getText().substring(0, textCode.getText().length() - 2), 16);
            patch = new Patch(address, data);
            return true;
        } else if ((code.length() == 6) && code.matches("[AEGIKLNOPSTUVXYZaegiklnopstuvxyz]*")) {
            //game genie type 1 code
            long c = GGtoHex(code);
            if (!((c & (BIT15)) != 0)) { //check bit 15 is false
                //now descramble this value into address and data
                final int address = 0x8000
                        | ((int) ((c >> 10) & 1) << 14)
                        | ((int) ((c >> 9) & 1) << 13)
                        | ((int) ((c >> 8) & 1) << 12)
                        | ((int) ((c >> 7) & 1) << 11)
                        | ((int) ((c >> 2) & 1) << 10)
                        | ((int) ((c >> 1) & 1) << 9)
                        | ((int) ((c >> 0) & 1) << 8)
                        | ((int) ((c >> 19) & 1) << 7)
                        | ((int) ((c >> 14) & 1) << 6)
                        | ((int) ((c >> 13) & 1) << 5)
                        | ((int) ((c >> 12) & 1) << 4)
                        | ((int) ((c >> 11) & 1) << 3)
                        | ((int) ((c >> 6) & 1) << 2)
                        | ((int) ((c >> 5) & 1) << 1)
                        | ((int) ((c >> 4) & 1));
                final int data = ((int) ((c >> 23) & 1) << 7)
                        | ((int) ((c >> 18) & 1) << 6)
                        | ((int) ((c >> 17) & 1) << 5)
                        | ((int) ((c >> 16) & 1) << 4)
                        | ((int) ((c >> 3) & 1) << 3)
                        | ((int) ((c >> 22) & 1) << 2)
                        | ((int) ((c >> 21) & 1) << 1)
                        | (int) ((c >> 20) & 1);
                btnApply.setEnabled(true);
                patch = new Patch(address, data);
                return true;
            }

        } else if ((code.length() == 8) && code.matches("[AEGIKLNOPSTUVXYZaegiklnopstuvxyz]*")) {
            //game genie type 2 code
            long c = GGtoHex(code);
            if (((c & (1 << 23)) != 0)) { //check bit 15 is true
                //now descramble this value into address and data
                final int address
                        = 0x8000
                        | ((int) ((c >> 18) & 1) << 14)
                        | ((int) ((c >> 17) & 1) << 13)
                        | ((int) ((c >> 16) & 1) << 12)
                        | ((int) ((c >> 15) & 1) << 11)
                        | ((int) ((c >> 10) & 1) << 10)
                        | ((int) ((c >> 9) & 1) << 9)
                        | ((int) ((c >> 8) & 1) << 8)
                        | ((int) ((c >> 27) & 1) << 7)
                        | ((int) ((c >> 22) & 1) << 6)
                        | ((int) ((c >> 21) & 1) << 5)
                        | ((int) ((c >> 20) & 1) << 4)
                        | ((int) ((c >> 19) & 1) << 3)
                        | ((int) ((c >> 14) & 1) << 2)
                        | ((int) ((c >> 13) & 1) << 1)
                        | ((int) ((c >> 12) & 1));
                final int data
                        = ((int) ((c >> 31) & 1) << 7)
                        | ((int) ((c >> 26) & 1) << 6)
                        | ((int) ((c >> 25) & 1) << 5)
                        | ((int) ((c >> 24) & 1) << 4)
                        | ((int) ((c >> 3) & 1) << 3)
                        | ((int) ((c >> 30) & 1) << 2)
                        | ((int) ((c >> 29) & 1) << 1)
                        | ((int) ((c >> 28) & 1));

                final int check
                        = //8 char game genie codes use a check byte so val is only
                        //patched when rom value matches this.
                        ((int) ((c >> 7) & 1) << 7)
                        | ((int) ((c >> 2) & 1) << 6)
                        | ((int) ((c >> 1) & 1) << 5)
                        | ((int) ((c >> 0) & 1) << 4)
                        | ((int) ((c >> 11) & 1) << 3)
                        | ((int) ((c >> 6) & 1) << 2)
                        | ((int) ((c >> 5) & 1) << 1)
                        | ((int) ((c >> 4) & 1));
                btnApply.setEnabled(true);
                patch = new Patch(address, data, check);
                return true;
            }
        }
        btnApply.setEnabled(false);
        patch = null;
        return false;
    }
    private final static String ggMap = "APZLGITYEOXUKSVN";

    private long GGtoHex(String code) {
        //converts a game genie code to a hex string
        //the individual bits still need to be unscrambled
        code = code.toUpperCase(Locale.ENGLISH);
        long ret = 0;
        for (int i = 0; i < code.length(); ++i) {
            ret *= 16;
            ret += ggMap.indexOf(code.charAt(i));
        }
        return ret;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        tabbedPan = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        panelApplyCode = new javax.swing.JPanel();
        textCode = new javax.swing.JTextField();
        btnApply = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        currentCodesList = new javax.swing.JList();
        btnRemoveAll = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        textFindData = new javax.swing.JTextField();
        btnSearch = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        listPossibleAddresses = new javax.swing.JList();
        btnTry = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Cheat Codes");
        setModal(true);
        setName("actionReplayDialog"); // NOI18N
        setResizable(false);

        tabbedPan.setMinimumSize(new java.awt.Dimension(10, 128));

        jTextArea1.setEditable(false);
        jTextArea1.setBackground(new java.awt.Color(240, 240, 240));
        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Tahoma", 0, 11)); // NOI18N
        jTextArea1.setRows(5);
        jTextArea1.setText("Enter a Game Genie or Pro Action Replay cheat code.\nA Pro Action Replay code is composed of 8 hexadecimal characters\n(numbers from 0 to 9 and letters from A to F).\nThe first 6 characters represent the in-memory address, and the last two the value to write in.\nIt is used to patch the NES memory to give you extra lives, ammo, time etc...");
        jScrollPane1.setViewportView(jTextArea1);

        panelApplyCode.setBorder(javax.swing.BorderFactory.createTitledBorder("Add new code"));

        textCode.setColumns(8);
        textCode.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        textCode.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                textCodeCaretUpdate(evt);
            }
        });

        btnApply.setText("Apply");
        btnApply.setEnabled(false);
        btnApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnApplyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelApplyCodeLayout = new javax.swing.GroupLayout(panelApplyCode);
        panelApplyCode.setLayout(panelApplyCodeLayout);
        panelApplyCodeLayout.setHorizontalGroup(
            panelApplyCodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelApplyCodeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(textCode, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnApply)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelApplyCodeLayout.setVerticalGroup(
            panelApplyCodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelApplyCodeLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelApplyCodeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(textCode, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnApply))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Currently applied codes"));

        currentCodesList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        currentCodesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane4.setViewportView(currentCodesList);

        btnRemoveAll.setText("Remove all");
        btnRemoveAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveAllActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnRemoveAll)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(btnRemoveAll)
                        .addGap(0, 96, Short.MAX_VALUE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 468, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(panelApplyCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(13, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelApplyCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        tabbedPan.addTab("Apply", jPanel1);

        jTextArea2.setEditable(false);
        jTextArea2.setBackground(new java.awt.Color(240, 240, 240));
        jTextArea2.setColumns(20);
        jTextArea2.setFont(new java.awt.Font("Tahoma", 0, 11)); // NOI18N
        jTextArea2.setRows(5);
        jTextArea2.setText("You can find new Pro Action Replay codes by searching into memory the location of\nsome data like remaining lives, time left, etc...\nStart a new search with the current value you want to search, then continue the search\nwith new values of the same data until the number of locations found is low, and then\ntry the codes until you found the good one!");
        jScrollPane3.setViewportView(jTextArea2);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Search"));

        textFindData.setColumns(8);
        textFindData.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        textFindData.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                textFindDataCaretUpdate(evt);
            }
        });

        btnSearch.setText("Search");
        btnSearch.setEnabled(false);
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        btnReset.setText("Reset Search");
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(textFindData, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSearch)
                .addContainerGap(42, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnReset)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(textFindData, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearch))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addComponent(btnReset)
                .addContainerGap())
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Possible addresses"));

        listPossibleAddresses.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listPossibleAddresses.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane5.setViewportView(listPossibleAddresses);

        btnTry.setText("Try");
        btnTry.setEnabled(false);
        btnTry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTryActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addComponent(btnTry)
                .addGap(18, 18, 18))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnTry)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 468, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(13, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        tabbedPan.addTab("Find", jPanel2);

        btnClose.setText("Close");
        btnClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPan, javax.swing.GroupLayout.PREFERRED_SIZE, 496, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnClose)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPan, javax.swing.GroupLayout.PREFERRED_SIZE, 315, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnClose)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabbedPan.getAccessibleContext().setAccessibleName("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnCloseActionPerformed
    {//GEN-HEADEREND:event_btnCloseActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    private void btnApplyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnApplyActionPerformed
    {//GEN-HEADEREND:event_btnApplyActionPerformed
        if (isCodeValid()) {
            actionReplay.addMemoryPatch(patch);
            this.setVisible(false);
        }
    }//GEN-LAST:event_btnApplyActionPerformed

    private void textCodeCaretUpdate(javax.swing.event.CaretEvent evt)//GEN-FIRST:event_textCodeCaretUpdate
    {//GEN-HEADEREND:event_textCodeCaretUpdate
        isCodeValid();
    }//GEN-LAST:event_textCodeCaretUpdate

    private void btnRemoveAllActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnRemoveAllActionPerformed
    {//GEN-HEADEREND:event_btnRemoveAllActionPerformed
        actionReplay.clear();
        updateCurrentCodesList();
    }//GEN-LAST:event_btnRemoveAllActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnResetActionPerformed
    {//GEN-HEADEREND:event_btnResetActionPerformed
        actionReplay.getFoundAddresses().clear();
        updateListPossibleCodes();
    }//GEN-LAST:event_btnResetActionPerformed

    private void textFindDataCaretUpdate(javax.swing.event.CaretEvent evt)//GEN-FIRST:event_textFindDataCaretUpdate
    {//GEN-HEADEREND:event_textFindDataCaretUpdate
        if (textFindData.getText().matches("[0-9]+")) {
            btnSearch.setEnabled(true);
        } else {
            btnSearch.setEnabled(false);
        }
    }//GEN-LAST:event_textFindDataCaretUpdate

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnSearchActionPerformed
    {//GEN-HEADEREND:event_btnSearchActionPerformed
        if (actionReplay.getFoundAddresses().isEmpty()) {
            actionReplay.newSearchInMemory((byte) Integer.parseInt(textFindData.getText(), 16));
        } else {
            actionReplay.continueSearch((byte) Integer.parseInt(textFindData.getText(), 16));
        }
        updateListPossibleCodes();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnTryActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnTryActionPerformed
    {//GEN-HEADEREND:event_btnTryActionPerformed
        textCode.setText(listPossibleAddresses.getSelectedValue().toString());
        tabbedPan.setSelectedIndex(0);
    }//GEN-LAST:event_btnTryActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnApply;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnRemoveAll;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnTry;
    private javax.swing.JList currentCodesList;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JList listPossibleAddresses;
    private javax.swing.JPanel panelApplyCode;
    private javax.swing.JTabbedPane tabbedPan;
    private javax.swing.JTextField textCode;
    private javax.swing.JTextField textFindData;
    // End of variables declaration//GEN-END:variables
}
