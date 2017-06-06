/*
 * Copyright 2017 Hewlett-Packard Enterprise Development Company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.adm.octane.ideplugins.intellij.ui.tabbedpane;

import com.hpe.adm.octane.ideplugins.intellij.ui.searchresult.CustomSearchTextField;
import com.intellij.execution.ui.layout.impl.JBRunnerTabs;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

class CustomJBRunnerTabs extends JBRunnerTabs {

    private static final int HISTORY_SIZE = 5;

    /**
     * Horrible workaround
     */
    Map<TabInfo, CustomSearchTextField> searchFields = new HashMap<>();
    private String lastSearchText = "";

    public CustomJBRunnerTabs(@Nullable Project project, @NotNull ActionManager actionManager, IdeFocusManager focusManager, @NotNull Disposable parent) {
        super(project, actionManager, focusManager, parent);

        addListener(new TabsListener.Adapter() {
            @Override
            public void beforeSelectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                //sync text and history
                if(searchFields.containsKey(oldSelection) && searchFields.containsKey(newSelection)){
                    searchFields.get(newSelection).setText(lastSearchText);
                    //sync history for newly open tabs
                    searchFields.get(newSelection).setHistory(searchFields.get(oldSelection).getHistory());
                }
            }

            @Override
            public void tabRemoved(TabInfo tabToRemove) {
                searchFields.remove(tabToRemove);
            }
        });

    }

    private CustomSearchTextField createTextField(){
        CustomSearchTextField newSearchTextField = new CustomSearchTextField();

        newSearchTextField.setHistorySize(5);
        newSearchTextField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                lastSearchText = newSearchTextField.getText();
            }
        });

        newSearchTextField.setHistoryItemClickedHandler(()-> search(newSearchTextField));

        newSearchTextField.addKeyboardListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER &&
                        searchRequestHandler != null &&
                        StringUtils.isNotBlank(newSearchTextField.getText())){

                    search(newSearchTextField);
                }
            }
        });

        return newSearchTextField;
    }

    private void search(CustomSearchTextField searchTextField){
        addToSearchHistory(searchTextField.getText());
        //sync history
        searchFields.values().forEach(textField -> textField.setHistory(getSearchHistory()));
        searchRequestHandler.searchedQuery(searchTextField.getText());
    }

    @NotNull
    @Override
    public TabInfo addTab(TabInfo info) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        CustomSearchTextField searchTextField = createTextField();
        searchPanel.add(searchTextField);
        searchFields.put(info, searchTextField);

        info.setSideComponent(searchPanel);
        return super.addTab(info);
    }

    public interface SearchRequestHandler {
        void searchedQuery(String query);
    }

    private SearchRequestHandler searchRequestHandler;

    public void setSearchRequestHandler(SearchRequestHandler searchRequestHandler){
        this.searchRequestHandler = searchRequestHandler;
    }

    public void setSearchHistory(List<String> searchHistory){
        this.searchHistory = searchHistory;
        searchFields.values().forEach(searchTextField -> searchTextField.setHistory(searchHistory));
    }

    public List<String> getSearchHistory(){
        return searchHistory;
    }

    private List<String> searchHistory = new ArrayList<>();

    private void addToSearchHistory(String string){
        if(searchHistory.contains(string)){
            searchHistory.remove(string);
        }
        searchHistory.add(0, string);
        if(searchHistory.size() > HISTORY_SIZE){
            searchHistory.remove(HISTORY_SIZE);
        }
    }


}