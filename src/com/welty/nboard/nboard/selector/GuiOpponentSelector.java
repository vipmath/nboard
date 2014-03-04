package com.welty.nboard.nboard.selector;

import com.orbanova.common.jsb.JsbGridLayout;
import com.welty.othello.api.OpponentSelection;
import com.welty.othello.api.OpponentSelector;
import com.welty.othello.gui.ExternalEngineManager;
import com.welty.othello.gui.prefs.PrefInt;
import com.welty.othello.gui.prefs.PrefString;
import com.welty.othello.gui.selector.EngineSelector;
import com.welty.othello.gui.selector.ExternalEngineSelector;
import com.welty.othello.gui.selector.InternalEngineSelectorManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.prefs.BackingStoreException;

import static com.orbanova.common.jsb.JSwingBuilder.*;

/**
 * A Window that allows the user to select an Opponent to play
 */
public class GuiOpponentSelector extends OpponentSelector {

    private final PrefInt levelPref;
    private final PrefString enginePref;


    private final JDialog frame;
    private final JList<Integer> levels = new JList<>();
    private final EngineListModel engineListModel;
    private final JList<EngineSelector> engineSelectors;
    private final JLabel strengthLabel = new JLabel("Strength");

    // these are written to when the user clicks "OK"
    private int selectedLevel;
    private @NotNull EngineSelector selectedEngine;

    /**
     * Create a window that allows the user to select an Opponent (=engine + depth)
     *
     * @param windowTitle        title of the selection window
     * @param includeWeakEngines if true, weak engines are included in the selection list. If false, they are not
     * @param preferencePrefix   prefix for saving the user's choices.
     */
    public GuiOpponentSelector(String windowTitle, boolean includeWeakEngines, String preferencePrefix) {
        levelPref = new PrefInt(GuiOpponentSelector.class, preferencePrefix + "Level", includeWeakEngines ? 1 : 12);
        enginePref = new PrefString(GuiOpponentSelector.class, preferencePrefix + "Opponent", includeWeakEngines ? "Abigail" : "Vegtbl");
        engineListModel = new EngineListModel(InternalEngineSelectorManager.internalOpponentSelectors(includeWeakEngines));

        // Level selection list box.
        // Need to create this before Opponent selection list box because the
        // Opponent selection list box modifies it.
        final DefaultListModel<Integer> levelModel = new DefaultListModel<>();
        setLevelElements(levelModel, EngineSelector.advancedLevels);
        levels.setModel(levelModel);
        setUpList(levels);
        levels.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        levels.setVisibleRowCount(EngineSelector.advancedLevels.length / 2);
        levels.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    setStrength();
                }
            }
        });


        // Opponent selection list box.
        engineSelectors = new JList<>(engineListModel);
        setUpList(engineSelectors);

        engineSelectors.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    final EngineSelector engineSelector = engineSelectors.getSelectedValue();
                    setLevelElements(levelModel, engineSelector.availableLevels);
                    levels.setSelectedIndex(findNearestLevel(selectedLevel, engineSelector.availableLevels));
                    setStrength();
                }
            }
        });

        selectUsersPreferredEngine();
        selectUsersPreferredLevel();
        setStrength();

        final JButton ok = button(new AbstractAction("OK") {
            @Override public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);
                selectedLevel = levels.getSelectedValue();
                levelPref.put(levels.getSelectedValue());
                setSelectedEngine();
                enginePref.put(selectedEngine.name);
                fireOpponentChanged();
            }
        });


        final JButton cancel = button(new AbstractAction("Cancel") {
            @Override public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);

            }
        });

        final JButton addEngine = button(new AbstractAction("Add engine...") {
            @Override public void actionPerformed(ActionEvent e) {
                new AddEngineDialog(frame);
            }
        });

        frame = new JDialog(null, windowTitle, Dialog.ModalityType.APPLICATION_MODAL);
        frame.setLayout(new JsbGridLayout(1));
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.add(
                vBox(
                        grid(2, 0, -1,
                                wrap("Opponent", engineSelectors), wrap("Level", levels)
                        ),
                        strengthLabel,
                        buttonBar(false, addEngine),
                        buttonBar(true, ok, cancel)
                )
        );
        frame.pack();
        frame.setVisible(false);

        frame.getRootPane().setDefaultButton(ok);

        ExternalEngineManager.instance.addListener(new ExternalEngineManager.Listener() {
            @Override public void engineAdded(String name, String wd, String command) {
                engineListModel.put(new ExternalEngineSelector(name, wd, command));
            }
        });
    }

    private void setSelectedEngine() {
        selectedEngine = engineSelectors.getSelectedValue();
    }

    private void setStrength() {
        final EngineSelector selector = engineSelectors.getSelectedValue();
        if (selector != null) {
            final Integer selectedLevel = levels.getSelectedValue();
            if (selectedLevel != null) {
                final String strength = selector.strengthEstimate(selectedLevel);
                strengthLabel.setText(strength);
            }
        }
    }

    /**
     * Notify all listeners that the opponent was changed.
     */
    private void fireOpponentChanged() {
        final List<Listener> listeners = getListeners();
        for (Listener listener : listeners) {
            listener.opponentChanged();
        }
    }

    /**
     * Select the User's preferred level both in the dialog box and in the
     * persistent variables (so it will be used even if the user presses "cancel").
     */
    private void selectUsersPreferredLevel() {
        selectedLevel = levelPref.get();
        levels.setSelectedIndex(findNearestLevel(selectedLevel, selectedEngine.availableLevels));
        selectedLevel = levels.getSelectedValue();
    }

    /**
     * Select the User's preferred engine both in the dialog box and in the
     * persistent variables (so it will be used even if the user presses "cancel").
     */
    private void selectUsersPreferredEngine() {
        final String preferredEngineName = enginePref.get();
        final int i = engineListModel.find(preferredEngineName);
        engineSelectors.setSelectedIndex(Math.max(0, i));
        setSelectedEngine();
    }

    /**
     * Outline a list with a scrollPane which has a titled border
     *
     * @param title bordered title
     * @param list  list
     * @return the scrollPane
     */
    private static JComponent wrap(String title, JList list) {
        final Dimension preferredSize = list.getPreferredSize();
        list.setBorder(null);
        final JLabel jLabel = new JLabel(title);
        jLabel.setFont(UIManager.getFont("TitledBorder.font"));
        final int minWidth = 50 + Math.max(preferredSize.width, jLabel.getPreferredSize().width);
        final JScrollPane scrollPane = scrollPane(list);
        scrollPane.setPreferredSize(new Dimension(minWidth, 50 + preferredSize.height));
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title));
        return scrollPane;
    }

    /**
     * Set the contents of the ListModel to the given levels.
     */
    private static void setLevelElements(DefaultListModel<Integer> ListModel, Integer[] levels) {
        ListModel.removeAllElements();
        for (Integer level : levels) {
            ListModel.addElement(level);
        }
    }

    /**
     * Find the index of the highest level <= targetLevel.
     * <p/>
     * This implementation assumes the levels are in order.
     *
     * @param targetLevel desired search depth
     * @param levels      available search depth
     * @return index of search depth
     */
    private static int findNearestLevel(int targetLevel, Integer[] levels) {
        int i;
        for (i = 0; i < levels.length; i++) {
            if (levels[i] > targetLevel) {
                break;
            }
        }
        if (i > 0) {
            i--;
        }

        return i;
    }

    private static <T> void setUpList(JList<T> ops) {
        ops.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        ops.setFont(UIManager.getFont("TextField.font"));
        ops.setAlignmentY(0.0f);
        ops.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ops.setSelectedIndex(0);
    }

    /**
     * Display this window
     */
    public void show() {
        frame.setVisible(true);
    }

    /**
     * Nuke engine selectors
     */
    public static void main(String[] args) throws BackingStoreException {
        ExternalEngineManager.instance.removeAll();
    }

    @NotNull @Override public OpponentSelection getOpponent() {
        return new OpponentSelection(selectedEngine, selectedLevel);
    }
}
