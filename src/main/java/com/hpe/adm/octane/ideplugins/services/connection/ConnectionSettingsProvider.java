package com.hpe.adm.octane.ideplugins.services.connection;

public interface ConnectionSettingsProvider {

    ConnectionSettings getConnectionSettings();

    void setConnectionSettings(ConnectionSettings connectionSettings);

}