package com.hpe.adm.octane.ideplugins.services;

import com.google.inject.Inject;
import com.hpe.adm.nga.sdk.NGA;
import com.hpe.adm.nga.sdk.authorisation.UserAuthorisation;
import com.hpe.adm.nga.sdk.exception.NgaException;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.octane.ideplugins.services.connection.ConnectionSettings;

import java.util.Collection;

public class TestService {

    @Inject
    private ConnectionSettings connectionSettings;

    public Collection<EntityModel> getDefects(){
        return createNGA().entityList("defects").get().execute();
    }

    /**
     * Check if the current connection settings are valid
     */
    public void testConnection() throws Exception{
        try {
            createNGA();
        } catch (NgaException ex){
            throw new Exception("Connection failed with provided connections settings: " + ex);
        }
    }

    private NGA createNGA(){
        NGA.Builder builder = new NGA
                .Builder(new UserAuthorisation(connectionSettings.getUserName(), connectionSettings.getPassword()))
                .Server(connectionSettings.getBaseUrl())
                .sharedSpace(connectionSettings.getSharedSpaceId())
                .workSpace(connectionSettings.getWorkspaceId());

        return builder.build();
    }

}