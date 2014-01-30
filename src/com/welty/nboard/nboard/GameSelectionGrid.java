package com.welty.nboard.nboard;

import com.welty.nboard.gui.Grid;
import com.welty.nboard.gui.GridColumn;
import com.welty.nboard.gui.GridTableModel;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: HP_Administrator
 * Date: Jun 25, 2009
 * Time: 8:44:13 PM
 * To change this template use File | Settings | File Templates.
 */
class GameSelectionGrid extends Grid {
    private static final GridColumn[] gridColumns = {
            new GridColumn(80, "Black"),
            new GridColumn(80, "White"),
            new GridColumn(160, "Date"),
            new GridColumn(40, "Result"),
            new GridColumn(100, "Opening")};


    GameSelectionGrid(ReversiWindow pwTarget) {
        this(new GameSelectionTableModel(pwTarget));
    }

    private GameSelectionGrid(GameSelectionTableModel tableModel) {
        super(tableModel, new JTable(tableModel), true, false, true);
        final JTable table = getTable();
        table.setAutoCreateRowSorter(true);
        ((DefaultRowSorter) table.getRowSorter()).setComparator(4, new AsDoubleSort());
    }

    public void MouseDataClick(int modelRow, int modelCol) {
        getTableModel().MouseDataClick(modelRow, modelCol);
    }

    public GameSelectionTableModel getTableModel() {
        return ((GameSelectionTableModel) super.getTableModel());
    }

    void Load(final File file) {
        getTableModel().Load(file);
    }

    private static final int[] gameTextFieldFromColumn = {0, 1, 2, 4, 6};

    static class GameSelectionTableModel extends GridTableModel {
        private ArrayList<GgfGameText> m_gts = new ArrayList<GgfGameText>();
        private final ReversiWindow m_pwTarget;

        public GameSelectionTableModel(ReversiWindow pwTarget) {
            super(gridColumns);
            this.m_pwTarget = pwTarget;
        }

        public int getRowCount() {
            return m_gts.size();
        }

        public Object getValueAt(int item, int field) {
            return m_gts.get(item).GetText(gameTextFieldFromColumn[field]);
        }

        /**
         * Send the game to the pwTarget.
         * <p/>
         * \note since we're storing a raw pointer, we have to hope the target is still there.
         * In this case it should be; the target is the main application window and if it dies
         * the program ends. Is it possible to click on this window after closing the main window
         * but before the main window's PostQuitMessage() has been sent? Not sure about the relative
         * message priorities.
         */
        public void MouseDataClick(int modelRow, int modelCol) {
            if (modelRow < getRowCount() && modelRow >= 0) {
                m_pwTarget.m_pd.Update(m_gts.get(modelRow).m_text, true);
                m_pwTarget.BringToTop();
            }
        }

        /**
         * Load a file into the grid. The filename is given by fn.
         * <p/>
         * \post Pops up a message box if some games appear invalid.
         */
        void Load(final File fn) {
            m_gts = GgfGameText.Load(fn);
            fireTableDataChanged();
            m_pwTarget.repaint();
        }
    }
}