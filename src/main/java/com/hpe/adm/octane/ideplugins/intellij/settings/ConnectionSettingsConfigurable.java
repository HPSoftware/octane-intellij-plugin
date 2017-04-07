package com.hpe.adm.octane.ideplugins.intellij.settings;

import com.hpe.adm.octane.ideplugins.intellij.PluginModule;
import com.hpe.adm.octane.ideplugins.intellij.ui.components.ConnectionSettingsComponent;
import com.hpe.adm.octane.services.TestService;
import com.hpe.adm.octane.services.connection.ConnectionSettings;
import com.hpe.adm.octane.services.connection.ConnectionSettingsProvider;
import com.hpe.adm.octane.services.exception.ServiceException;
import com.hpe.adm.octane.services.exception.ServiceRuntimeException;
import com.hpe.adm.octane.services.nonentity.OctaneVersionService;
import com.hpe.adm.octane.services.util.Constants;
import com.hpe.adm.octane.services.util.OctaneVersion;
import com.hpe.adm.octane.services.util.UrlParser;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ConnectionSettingsConfigurable implements SearchableConfigurable {

    private static final String NAME = "Octane";
    private static final String DYNAMO_VERSION = "12.53.20";
    private Project currentProject = null;

    //@Inject is not supported here, this class is instantiated by intellij
    private ConnectionSettingsProvider connectionSettingsProvider;
    private TestService testService;
    private IdePluginPersistentState idePluginPersistentState;
    private OctaneVersionService versionService;
    private ConnectionSettingsComponent connectionSettingsView = new ConnectionSettingsComponent();
    private boolean pinMessage = false;

    @NotNull
    @Override
    public String getId() {
        return "settings.octane";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "settings.octane";
    }

    public ConnectionSettingsConfigurable(@NotNull final Project currentProject) {
        PluginModule module = PluginModule.getPluginModuleForProject(currentProject);
        connectionSettingsProvider = module.getInstance(ConnectionSettingsProvider.class);
        testService = module.getInstance(TestService.class);
        idePluginPersistentState = module.getInstance(IdePluginPersistentState.class);
        versionService = module.getInstance(OctaneVersionService.class);
        this.currentProject = currentProject;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ConnectionSettings connectionSettings = connectionSettingsProvider.getConnectionSettings();

        //Setting the base url will fire the even handler in the view, this will set the shared space and workspace fields
        connectionSettingsView.setServerUrl(UrlParser.createUrlFromConnectionSettings(connectionSettings));
        connectionSettingsView.setUserName(connectionSettings.getUserName());
        connectionSettingsView.setPassword(connectionSettings.getPassword());

        connectionSettingsView.setTestConnectionActionListener(event -> {
            //Clear previous message
            connectionSettingsView.setConnectionStatusLoading();
            new SwingWorker() {
                @Override
                protected Void doInBackground() throws Exception {
                    testConnection();
                    return null;
                }
            }.execute();
        });
        return connectionSettingsView.getComponent();
    }

    @Override
    public boolean isModified() {
        //If it's empty and different allow apply
        if (isViewConnectionSettingsEmpty() && !connectionSettingsProvider.getConnectionSettings().isEmpty()) {
            return true;
        } else if (isViewConnectionSettingsEmpty()) {
            return false;
        }

        if (!pinMessage) {
            connectionSettingsView.setConnectionStatusLabelVisible(false);
        }

        ConnectionSettings currentConnectionSettings = connectionSettingsProvider.getConnectionSettings();
        ConnectionSettings viewConnectionSettings;
        try {
            viewConnectionSettings = validateClientSide();
        } catch (ServiceException ex) {
            pinMessage = false;
            return false;
        }

        return !viewConnectionSettings.equals(currentConnectionSettings);
    }

    @Override
    public void apply() throws ConfigurationException {
        //If the connection settings are empty then save them, only way to clear and save
        if (isViewConnectionSettingsEmpty()) {
            connectionSettingsProvider.setConnectionSettings(new ConnectionSettings());
            return;
        }

        ConnectionSettings newConnectionSettings = testConnection();

        //apply if valid
        if (newConnectionSettings != null) {

            //If anything other than the password was changed, wipe open tabs and active tab item
            if (!newConnectionSettings.equalsExceptPassword(connectionSettingsProvider.getConnectionSettings())) {
                idePluginPersistentState.clearState(IdePluginPersistentState.Key.ACTIVE_WORK_ITEM);
                idePluginPersistentState.clearState(IdePluginPersistentState.Key.SELECTED_TAB);
                idePluginPersistentState.clearState(IdePluginPersistentState.Key.OPEN_TABS);
            }

            connectionSettingsProvider.setConnectionSettings(newConnectionSettings);
            //remove the hash and remove extra stuff if successful
            connectionSettingsView.setServerUrl(UrlParser.createUrlFromConnectionSettings(newConnectionSettings));
            connectionSettingsView.setConnectionStatusSuccess();
        }
    }

    private void testOctaneVersion(ConnectionSettings connectionSettings) {
        OctaneVersion version = OctaneVersionService.getOctaneVersion(connectionSettings);
        version.discardBuildNumber();
        if (version.compareTo(OctaneVersion.DYNAMO) < 0) {
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(currentProject);
            String message = "Octane version not supported. This plugin works with Octane versions starting " + DYNAMO_VERSION;
            Balloon balloon = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message,
                    MessageType.WARNING, null)
                    .setCloseButtonEnabled(true)
                    .createBalloon();
            balloon.show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
        }
    }

    /**
     * Test the connection with the given info from the view, sets error labels
     *
     * @return ConnectionSettings if valid, null otherwise
     */
    private ConnectionSettings testConnection() {

        connectionSettingsView.setConnectionStatusLoading();

        ConnectionSettings newConnectionSettings;
        try {
            newConnectionSettings = validateClientSide();
        } catch (ServiceException ex) {
            return null;
        }

        pinMessage = true;

        //This will attempt a connection
        try {
            testService.testConnection(newConnectionSettings);
            testOctaneVersion(newConnectionSettings);
            SwingUtilities.invokeLater(connectionSettingsView::setConnectionStatusSuccess);
        } catch (ServiceException | ServiceRuntimeException ex) {
            SwingUtilities.invokeLater(() -> connectionSettingsView.setConnectionStatusError(ex.getMessage()));
            return null;
        }

        return newConnectionSettings;
    }


    private ConnectionSettings getConnectionSettingsFromView() throws ServiceException {
        //Parse server url
        return UrlParser.resolveConnectionSettings(
                connectionSettingsView.getServerUrl(),
                connectionSettingsView.getUserName(),
                connectionSettingsView.getPassword());
    }

    private boolean isViewConnectionSettingsEmpty() {
        return StringUtils.isEmpty(connectionSettingsView.getServerUrl()) &&
                StringUtils.isEmpty(connectionSettingsView.getUserName()) &&
                StringUtils.isEmpty(connectionSettingsView.getPassword());
    }

    private void validateUsernameAndPassword() throws ServiceException {
        StringBuilder errorMessageBuilder = new StringBuilder();
        if (StringUtils.isEmpty(connectionSettingsView.getUserName())) {
            errorMessageBuilder.append("Username cannot be blank.");
        }
        if (errorMessageBuilder.length() != 0) {
            errorMessageBuilder.append(" ");
        }
        if (StringUtils.isEmpty(connectionSettingsView.getPassword())) {
            errorMessageBuilder.append("Password cannot be blank.");
        }

        if (errorMessageBuilder.length() != 0) {
            throw new ServiceException(errorMessageBuilder.toString());
        }
    }

    private ConnectionSettings validateClientSide() throws ServiceException {
        ConnectionSettings newConnectionSettings;

        // Validation that does not require connection to the server,
        // only this one shows and example for a correct message
        try {
            newConnectionSettings = getConnectionSettingsFromView();
        } catch (ServiceException ex) {

            final StringBuilder errorMessageBuilder = new StringBuilder();

            errorMessageBuilder.append(ex.getMessage());
            errorMessageBuilder.append("<br>");
            errorMessageBuilder.append(Constants.CORRECT_URL_FORMAT_MESSAGE);

            SwingUtilities.invokeLater(() -> connectionSettingsView.setConnectionStatusError(errorMessageBuilder.toString()));
            throw ex;
        }

        //Validation of username and password
        try {
            validateUsernameAndPassword();
        } catch (ServiceException ex) {
            SwingUtilities.invokeLater(() -> connectionSettingsView.setConnectionStatusError(ex.getMessage()));
            throw ex;
        }

        return newConnectionSettings;
    }

    @Override
    public void reset() {
        ConnectionSettings connectionSettings = connectionSettingsProvider.getConnectionSettings();
        connectionSettingsView.setServerUrl(UrlParser.createUrlFromConnectionSettings(connectionSettings));
        connectionSettingsView.setUserName(connectionSettings.getUserName());
        connectionSettingsView.setPassword(connectionSettings.getPassword());
    }

    @Override
    public void disposeUIResources() {
        connectionSettingsView = null;
    }

}
