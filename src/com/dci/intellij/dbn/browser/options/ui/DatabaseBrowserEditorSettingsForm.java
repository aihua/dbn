package com.dci.intellij.dbn.browser.options.ui;

import com.dci.intellij.dbn.browser.options.DatabaseBrowserEditorSettings;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.ui.Borders;
import com.dci.intellij.dbn.common.ui.DBNForm;
import com.dci.intellij.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dci.intellij.dbn.common.ui.table.DBNEditableTable;
import com.dci.intellij.dbn.common.ui.table.DBNEditableTableModel;
import com.dci.intellij.dbn.common.ui.table.DBNTable;
import com.dci.intellij.dbn.object.common.editor.DefaultEditorOption;
import com.dci.intellij.dbn.object.common.editor.DefaultEditorType;
import com.dci.intellij.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static com.dci.intellij.dbn.common.ui.GUIUtil.updateBorderTitleForeground;

public class DatabaseBrowserEditorSettingsForm extends ConfigurationEditorForm<DatabaseBrowserEditorSettings> {
    private JPanel mainPanel;
    private JBScrollPane editorTypesScrollPanel;
    private JTable editorTypeTable;


    public DatabaseBrowserEditorSettingsForm(DatabaseBrowserEditorSettings settings) {
        super(settings);
        updateBorderTitleForeground(mainPanel);
        editorTypeTable = new EditorTypeTable(this, settings.getOptions());
        editorTypesScrollPanel.setViewportView(editorTypeTable);
        editorTypesScrollPanel.getViewport().setBackground(editorTypeTable.getBackground());
        registerComponent(editorTypeTable);
    }


    @Override
    public void applyFormChanges() throws ConfigurationException {
        EditorTypeTableModel model = (EditorTypeTableModel) editorTypeTable.getModel();
        getConfiguration().setOptions(model.options);
    }

    @Override
    public void resetFormChanges() {
        editorTypeTable.setModel(new EditorTypeTableModel(getConfiguration().getOptions()));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public class EditorTypeTable extends DBNEditableTable<EditorTypeTableModel> {

        EditorTypeTable(DBNForm parent, List<DefaultEditorOption> options) {
            super(parent, new EditorTypeTableModel(options), true);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            adjustRowHeight(3);
            setDefaultRenderer(DBObjectType.class, new DBNColoredTableCellRenderer() {
                @Override
                protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                    DBObjectType objectType = (DBObjectType) value;
                    if (objectType != null) {
                        setIcon(objectType.getIcon());
                        append(objectType.getName().toUpperCase(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    } else {
                        append("");
                    }
                    setBorder(SELECTION_BORDER);
                }
            });

            setDefaultRenderer(DefaultEditorType.class, new DBNColoredTableCellRenderer() {
                @Override
                protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                    DefaultEditorType editorType = (DefaultEditorType) value;
                    append(editorType.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    setBorder(SELECTION_BORDER);
                }
            });

            ComboBoxTableRenderer<DefaultEditorType> editor = new ComboBoxTableRenderer<DefaultEditorType>(DefaultEditorType.values());
            editor.setBorder(Borders.TEXT_FIELD_BORDER);
            setDefaultEditor(DefaultEditorType.class, editor);

            getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        //editCellAt(getSelectedRows()[0], getSelectedColumns()[0]);
                    }
                }
            });
        }

        @Override
        public TableCellEditor getCellEditor(int row, int column) {
            if (column == 1) {
                EditorTypeTableModel model = (EditorTypeTableModel) getModel();
                DefaultEditorOption editorOption = model.options.get(row);
                DBObjectType objectType = editorOption.getObjectType();
                DefaultEditorType[] editorTypes = DefaultEditorType.getEditorTypes(objectType);
                return new ComboBoxTableRenderer<DefaultEditorType>(editorTypes);
            }
            return null;
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            Point mouseLocation = e.getPoint();
            int columnIndex = columnAtPoint(mouseLocation);
            if (columnIndex == 1) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
            super.processMouseMotionEvent(e);
        }
    }

    public class EditorTypeTableModel extends DBNEditableTableModel {
        private List<DefaultEditorOption> options = new ArrayList<DefaultEditorOption>();

        public EditorTypeTableModel(List<DefaultEditorOption> options) {
            this.options = new ArrayList<DefaultEditorOption>(options);
        }

        @Override
        public int getRowCount() {
            return options.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0: return "Object Type";
                case 1: return "Default Editor";
            }
            return null;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0: return DBObjectType.class;
                case 1: return DefaultEditorType.class;
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0: return false;
                case 1: return true;
            }
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DefaultEditorOption option = options.get(rowIndex);
            switch (columnIndex) {
                case 0: return option.getObjectType();
                case 1: return option.getEditorType();
            }
            return null;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                DefaultEditorType editorType = (DefaultEditorType) value;
                if (editorType != null) {
                    DefaultEditorOption option = options.remove(rowIndex);
                    options.add(rowIndex, new DefaultEditorOption(option.getObjectType(), editorType));
                }
            }
        }

        @Override
        public void insertRow(int rowIndex) {
            throw new UnsupportedOperationException("Row mutation not supported");
        }

        @Override
        public void removeRow(int rowIndex) {
            throw new UnsupportedOperationException("Row mutation not supported");
        }
    }
}
