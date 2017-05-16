package com.hpe.adm.octane.ideplugins.intellij.ui.components;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.hpe.adm.octane.ideplugins.intellij.PluginModule;
import com.hpe.adm.octane.ideplugins.intellij.settings.ConnectionSettingsConfigurable;
import com.hpe.adm.octane.ideplugins.intellij.ui.HasComponent;
import com.hpe.adm.octane.ideplugins.intellij.ui.treetable.nowork.NoWorkPanel;
import com.hpe.adm.octane.services.connection.ConnectionSettingsProvider;
import com.hpe.adm.octane.services.util.Constants;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import org.jdesktop.swingx.JXHyperlink;

public class WelcomeViewComponent extends JPanel implements HasComponent {

    private static final long serialVersionUID = 1L;
    private static final String WELCOME_TEXT = "Welcome to ALM Octane plugin";
    private static final String OCTANE_SETTINGS_TEXT = "To start, go to Settings and connect.";
    private static final String OCTANE_SETTINGS_RETRY = "Retry";
    private JXHyperlink hyperlinkSettings;
    private JXHyperlink hyperlinkRetry;
    private JLabel lblMessage;
    private JPanel panel;
    private boolean connectionFailed;

    public WelcomeViewComponent() {
        init();
    }

    public WelcomeViewComponent(Project project) {
        init();
        hyperlinkSettings.addActionListener(event -> ShowSettingsUtil.getInstance().showSettingsDialog(project, ConnectionSettingsConfigurable.class));
    }

    public WelcomeViewComponent(Project project, String welcomeMessage, String settingsLinkMessage, String retryMessage) {
        final ConnectionSettingsProvider connectionSettingsProvider = PluginModule.getPluginModuleForProject(project).getInstance(ConnectionSettingsProvider.class);
        connectionFailed = retryMessage != null;
        init();
        lblMessage.setText(welcomeMessage);
        hyperlinkSettings.setText(settingsLinkMessage);
        hyperlinkSettings.addActionListener(event -> ShowSettingsUtil.getInstance().showSettingsDialog(project, ConnectionSettingsConfigurable.class));
        if (connectionFailed) {
            hyperlinkRetry.setText(retryMessage);
            hyperlinkRetry.addActionListener(event -> {
                        connectionSettingsProvider.setConnectionSettings(connectionSettingsProvider.getConnectionSettings());
                    }
            );
        }
    }


    /**
     * Create the panel.
     */
    private void init() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{0, 0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
        gridBagLayout.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{1.0, 1.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);

        panel = new JPanel();
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.gridwidth = 2;
        gbc_panel.insets = new Insets(0, 0, 5, 0);
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.gridx = 0;
        gbc_panel.gridy = 0;
        add(panel, gbc_panel);

        JLabel lblCompany = new JLabel("");
        lblCompany.setIcon(new ImageIcon(NoWorkPanel.class.getResource(Constants.IMG_VENDOR_LOGO)));

        GridBagConstraints gbc_lblCompany = new GridBagConstraints();
        gbc_lblCompany.anchor = GridBagConstraints.EAST;
        gbc_lblCompany.fill = GridBagConstraints.VERTICAL;
        gbc_lblCompany.insets = new Insets(0, 0, 5, 50);
        gbc_lblCompany.gridx = 0;
        gbc_lblCompany.gridy = 1;
        add(lblCompany, gbc_lblCompany);


        JLabel lblOctane = new JLabel("");
        lblOctane.setIcon(new ImageIcon(NoWorkPanel.class.getResource(Constants.IMG_OCTANE_LOGO)));
        GridBagConstraints gbc_lblOctane = new GridBagConstraints();
        gbc_lblOctane.anchor = GridBagConstraints.WEST;
        gbc_lblOctane.fill = GridBagConstraints.VERTICAL;
        gbc_lblOctane.insets = new Insets(0, 0, 5, 0);
        gbc_lblOctane.gridx = 1;
        gbc_lblOctane.gridy = 1;
        add(lblOctane, gbc_lblOctane);

        lblMessage = new JLabel(WELCOME_TEXT);
        GridBagConstraints gbc_lblMessage = new GridBagConstraints();
        gbc_lblMessage.gridwidth = 2;
        gbc_lblMessage.insets = new Insets(0, 0, 5, 0);
        gbc_lblMessage.gridx = 0;
        gbc_lblMessage.gridy = 2;
        add(lblMessage, gbc_lblMessage);

        hyperlinkSettings = new JXHyperlink();
        hyperlinkSettings.setText(OCTANE_SETTINGS_TEXT);
        GridBagConstraints gbc_hyperlinkSettings = new GridBagConstraints();
        gbc_hyperlinkSettings.gridwidth = 2;
        gbc_hyperlinkSettings.insets = new Insets(0, 0, 5, 0);
        gbc_hyperlinkSettings.gridx = 0;
        gbc_hyperlinkSettings.gridy = 3;
        add(hyperlinkSettings, gbc_hyperlinkSettings);

        if (connectionFailed) {
            hyperlinkRetry = new JXHyperlink();
            hyperlinkRetry.setText(OCTANE_SETTINGS_RETRY);
            GridBagConstraints gbc_hyperlinkRetry = new GridBagConstraints();
            gbc_hyperlinkRetry.anchor = GridBagConstraints.NORTH;
            gbc_hyperlinkRetry.gridwidth = 2;
            gbc_hyperlinkRetry.gridx = 0;
            gbc_hyperlinkRetry.gridy = 4;
            add(hyperlinkRetry, gbc_hyperlinkRetry);
        }

    }

    @Override
    public JComponent getComponent() {
        return this;
    }

}
