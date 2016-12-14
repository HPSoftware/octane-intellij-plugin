package com.hpe.adm.octane.ideplugins.intellij.ui.tabbedpane;

import com.hpe.adm.octane.ideplugins.intellij.ui.View;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import com.intellij.util.BitUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

public class TabbedPaneView implements View {

    private static final String TABBED_PANE_PLACE = "Octane plugin tabbed pane";

    private final JPanel rootPanel;
    private JBEditorTabs editorTabs;

    public TabbedPaneView(){
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(0, 0));

        //Init tabbed pane
        DataContext dataContext = DataManager.getInstance().getDataContext();
        Project project = DataKeys.PROJECT.getData(dataContext);
        editorTabs = new JBEditorTabs(project, ActionManager.getInstance(), IdeFocusManager.getGlobalInstance(), () -> {});
        editorTabs.setTabDraggingEnabled(true);

        rootPanel.add(editorTabs.getComponent(), BorderLayout.CENTER);
    }

    @Override
    public JComponent getComponent() {
        return editorTabs.getComponent();
    }


    public TabInfo addTab(String title, String tooltip, Icon icon, JComponent component) {
        return addTab(title, tooltip, icon, component, true);
    }

    public TabInfo addTab(String title, String tooltip, Icon icon, JComponent component, boolean isClosable) {
        TabInfo tabInfo = new TabInfo(component);

        if(icon != null) {
            tabInfo.setIcon(icon);
        }
        if(tooltip != null){
            tabInfo.setTooltipText(tooltip);
        }

        tabInfo.setText(title);

        if(isClosable) {
            addCloseActionToTab(tabInfo);
        }

        editorTabs.addTab(tabInfo);
        editorTabs.select(tabInfo, false);

        return tabInfo;
    }

    private void addCloseActionToTab(TabInfo tabInfo){
        DefaultActionGroup defaultActionGroup = new DefaultActionGroup();
        defaultActionGroup.addAction(new CloseTab(tabInfo));
        tabInfo.setTabLabelActions(defaultActionGroup, TABBED_PANE_PLACE);
    }

    private void closeAllExcept(TabInfo exceptTabInfo){
        editorTabs.getTabs().forEach(tabInfo -> {
           if(isClosable(tabInfo) && !tabInfo.equals(exceptTabInfo)){
               editorTabs.removeTab(tabInfo);
           }
        });
    }

    private boolean isClosable(TabInfo tabInfo){
        return (tabInfo.getTabLabelActions()!=null && hasActionType(tabInfo.getTabLabelActions(), CloseTab.class));
    }

    private boolean hasActionType(ActionGroup actionGroup, Class<? extends AnAction> actionType){
        for(AnAction anAction : actionGroup.getChildren(null)){
            if(anAction.getClass().equals(actionType)){
                return true;
            }
        }
        return false;
    }

    public boolean hasTabWithTabInfo(TabInfo tabInfo){
        return editorTabs.getTabs().contains(tabInfo);
    }

    public void selectTabWithTabInfo(TabInfo tabInfo, boolean requestFocus){
        editorTabs.select(tabInfo, requestFocus);
    }

    private class CloseTab extends AnAction implements DumbAware {

        private final TabInfo myTabInfo;

        CloseTab(TabInfo info) {
            myTabInfo = info;
        }

        @Override
        public void update(final AnActionEvent e) {
            e.getPresentation().setIcon(AllIcons.Actions.Close);
            e.getPresentation().setHoveredIcon(AllIcons.Actions.CloseHovered);
            e.getPresentation().setVisible(UISettings.getInstance().SHOW_CLOSE_BUTTON);
            e.getPresentation().setText("Close. Alt-click to close others.");
        }

        @Override
        public void actionPerformed(final AnActionEvent e) {
            if (BitUtil.isSet(e.getModifiers(), InputEvent.ALT_MASK)) {
                closeAllExcept(myTabInfo);
            } else {
                if (isClosable(myTabInfo)) {
                    editorTabs.removeTab(myTabInfo);
                }
            }
        }
    }

}
