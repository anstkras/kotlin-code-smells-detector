package org.jetbrains.research.kotlincodesmelldetector.ide.ui;

import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.research.kotlincodesmelldetector.KotlinCodeSmellDetectorBundle;
import org.jetbrains.research.kotlincodesmelldetector.ide.refactoring.moveMethod.MoveMethodRefactoring;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class MoveMethodTableModel extends AbstractTableModel {
    static final int SELECTION_COLUMN_INDEX = 0;
    private static final int ENTITY_COLUMN_INDEX = 1;
    private static final int MOVE_TO_COLUMN_INDEX = 2;
    private static final int ACCESSED_MEMBERS_COUNT_INDEX = 3;
    private static final int COLUMNS_COUNT = 4;

    private final List<MoveMethodRefactoring> refactorings = new ArrayList<>();
    private final List<Integer> virtualRows = new ArrayList<>();
    private boolean[] isSelected;

    MoveMethodTableModel(List<MoveMethodRefactoring> refactorings) {
        updateTable(refactorings);
    }

    void updateTable(List<MoveMethodRefactoring> refactorings) {
        this.refactorings.clear();
        this.refactorings.addAll(refactorings);
        isSelected = new boolean[refactorings.size()];
        IntStream.range(0, refactorings.size())
                .forEachOrdered(virtualRows::add);
        fireTableDataChanged();
    }

    void clearTable() {
        this.refactorings.clear();
        this.virtualRows.clear();
        isSelected = new boolean[0];
        fireTableDataChanged();
    }

    void selectAll() {
        for (int i = 0; i < virtualRows.size(); i++) {
            setValueAtRowIndex(true, i, false);
        }

        fireTableDataChanged();
    }

    void deselectAll() {
        for (int i = 0; i < virtualRows.size(); i++) {
            setValueAtRowIndex(false, i, false);
        }

        fireTableDataChanged();
    }

    void updateRows() {
        virtualRows.forEach(i -> {
            if (!refactorings.get(i).getOptionalMethod().isPresent()) {
                isSelected[i] = false;
            }
        });
        fireTableDataChanged();
    }

    List<MoveMethodRefactoring> pullSelected() {
        return virtualRows.stream()
                .filter(i -> isSelected[i] && refactorings.get(i).getOptionalMethod().isPresent())
                .map(refactorings::get)
                .collect(Collectors.toList());
    }

    @Override
    public int getColumnCount() {
        return COLUMNS_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case SELECTION_COLUMN_INDEX:
                return "";
            case ENTITY_COLUMN_INDEX:
                return KotlinCodeSmellDetectorBundle.message("method.column.title");
            case MOVE_TO_COLUMN_INDEX:
                return KotlinCodeSmellDetectorBundle.message("move.to.column.title");
            case ACCESSED_MEMBERS_COUNT_INDEX:
                return KotlinCodeSmellDetectorBundle.message("dependencies.column.title");
        }
        throw new IndexOutOfBoundsException("Unexpected column index: " + column);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == SELECTION_COLUMN_INDEX && refactorings.get(rowIndex).getOptionalMethod().isPresent();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == SELECTION_COLUMN_INDEX ? Boolean.class : String.class;
    }

    @Override
    public int getRowCount() {
        return virtualRows.size();
    }

    @Override
    public void setValueAt(Object value, int virtualRow, int columnIndex) {
        final int rowIndex = virtualRows.get(virtualRow);
        final boolean isRowSelected = (Boolean) value;
        setValueAtRowIndex(isRowSelected, rowIndex, true);

        fireTableDataChanged();
    }

    private void setValueAtRowIndex(boolean isRowSelected, int rowIndex, boolean forceSelectInConflicts) {
        if (!refactorings.get(rowIndex).getOptionalMethod().isPresent()) {
            return;
        }

        isSelected[rowIndex] = isRowSelected;
    }

    @Override
    @Nullable
    public Object getValueAt(int virtualRow, int columnIndex) {
        final int rowIndex = virtualRows.get(virtualRow);
        switch (columnIndex) {
            case SELECTION_COLUMN_INDEX:
                return isSelected[rowIndex];
            case ENTITY_COLUMN_INDEX:
                Optional<KtNamedFunction> method = refactorings.get(rowIndex).getOptionalMethod();
                String qualifiedMethodName = refactorings.get(rowIndex).getQualifiedMethodName();
                return method.map(psiMethod -> qualifiedMethodName).orElseGet(() -> qualifiedMethodName + " | "
                        + KotlinCodeSmellDetectorBundle.message("kotlin.member.is.not.valid"));
            case MOVE_TO_COLUMN_INDEX:
                Optional<KtClassOrObject> targetClass = refactorings.get(rowIndex).getOptionalTargetClass();
                return targetClass.map(ktClassOrObject -> ktClassOrObject.getFqName().toString()).orElseGet(()
                        -> KotlinCodeSmellDetectorBundle.message("target.class.is.not.valid"));
            case ACCESSED_MEMBERS_COUNT_INDEX:
                return refactorings.get(rowIndex).getSourceAccessedMembers() + "/" + refactorings.get(rowIndex).getTargetAccessedMembers();
        }
        throw new IndexOutOfBoundsException("Unexpected column index: " + columnIndex);
    }

    void setupRenderer(JTable table) {
        table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer() {
            private final JLabel EMPTY_LABEL = new JLabel();

            {
                EMPTY_LABEL.setBackground(JBColor.LIGHT_GRAY);
                EMPTY_LABEL.setOpaque(true);
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSel, boolean hasFocus,
                                                           int row, int column) {
                final int realRow = virtualRows.get(table.convertRowIndexToModel(row));
                if (refactorings.get(realRow).getOptionalMethod().isPresent()) {
                    return super.getTableCellRendererComponent(table, value, isSel, hasFocus, row, column);
                } else {
                    return EMPTY_LABEL;
                }
            }
        });

        table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int virtualRow, int column) {
                final int row = virtualRows.get(table.convertRowIndexToModel(virtualRow));
                if (!refactorings.get(row).getOptionalMethod().isPresent()) {
                    setBackground(JBColor.LIGHT_GRAY);
                } else if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(table.getBackground());
                }
                setEnabled(refactorings.get(row).getOptionalMethod().isPresent());
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, virtualRow, column);
            }
        });
    }
}