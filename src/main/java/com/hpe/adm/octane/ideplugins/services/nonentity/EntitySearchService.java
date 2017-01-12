package com.hpe.adm.octane.ideplugins.services.nonentity;

import com.google.inject.Inject;
import com.hpe.adm.nga.sdk.authorisation.UserAuthorisation;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.nga.sdk.model.StringFieldModel;
import com.hpe.adm.nga.sdk.network.HttpClient;
import com.hpe.adm.nga.sdk.network.HttpRequest;
import com.hpe.adm.nga.sdk.network.HttpRequestFactory;
import com.hpe.adm.nga.sdk.network.HttpResponse;
import com.hpe.adm.octane.ideplugins.services.connection.ConnectionSettings;
import com.hpe.adm.octane.ideplugins.services.connection.ConnectionSettingsProvider;
import com.hpe.adm.octane.ideplugins.services.exception.ServiceRuntimeException;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

public class EntitySearchService {

    private static final String JSON_DATA_NAME = "data";
    private static final String GLOBAL_TEXT_SEARCH_RESULT_TAG = "global_text_search_result";

    @Inject
    protected ConnectionSettingsProvider connectionSettingsProvider;

    public Collection<EntityModel> searchGlobal(String queryString, Entity entity) {

        ConnectionSettings connectionSettings = connectionSettingsProvider.getConnectionSettings();

        //auth
        UserAuthorisation auth = new UserAuthorisation(connectionSettings.getUserName(), connectionSettings.getPassword());
        HttpClient httpClient = HttpClient.getInstance();
        HttpRequestFactory requestFactory = httpClient.getRequestFactory(connectionSettings.getBaseUrl(), auth);
        if(! httpClient.authenticate()){
            throw new ServiceRuntimeException("Failed to authenticate with current connection settings");
        }

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http");
        uriBuilder.setHost(connectionSettings.getBaseUrl().replace("http://", ""));
        uriBuilder.setPath(
                "/api"
                + "/shared_spaces/" + connectionSettings.getSharedSpaceId()
                + "/workspaces/" + connectionSettings.getWorkspaceId()
                + "/" + entity.getApiEntityName());

        uriBuilder.setParameter("text_search", "{\"type\":\"global\",\"text\":\""+queryString+"\"}");

        if(entity.isSubtype()) {
            uriBuilder.setParameter("query", "\"(subtype='" + entity.getSubtypeName() + "')\"");
        }

        try {
            HttpRequest request = requestFactory.buildGetRequest( uriBuilder.build().toASCIIString());
            HttpResponse response = request.execute();
            String responseString = response.parseAsString();

            if(response.isSuccessStatusCode() && StringUtils.isNotBlank(responseString)){
                return searchResponseToEntityModels(responseString);
            } else {
                throw new ServiceRuntimeException("Failed to get search response JSON");
            }

        } catch (Exception ex) {
            throw new ServiceRuntimeException(ex);
        }
    }

    /**
     * Convert it to standard entity model for re-using exiting UI,
     * only has: id, name, type/subtype, description
     * @param responseString
     * @return
     */
    private Collection<EntityModel> searchResponseToEntityModels(String responseString){

        Collection<EntityModel> result = new ArrayList<>();

        JSONObject json = new JSONObject(responseString);
        JSONArray data = json.getJSONArray(JSON_DATA_NAME);

        data.forEach(jsonObj -> {
            JSONObject jsonObject = (JSONObject) jsonObj;

            //Create an entity model from the json, the json format is fixed
            String name = getStringOrBlank(jsonObject.getJSONObject(GLOBAL_TEXT_SEARCH_RESULT_TAG), "name" );
            String description = getStringOrBlank(jsonObject.getJSONObject(GLOBAL_TEXT_SEARCH_RESULT_TAG), "description" );
            String id = getStringOrBlank(jsonObject, "id" );
            String type = getStringOrBlank(jsonObject, "type" );
            String subtype = getStringOrBlank(jsonObject, "subtype" );

            EntityModel entityModel = new EntityModel();
            entityModel.setValue(new StringFieldModel("name", name));
            entityModel.setValue(new StringFieldModel("description", description));
            entityModel.setValue(new StringFieldModel("id", id));
            entityModel.setValue(new StringFieldModel("type", type));
            entityModel.setValue(new StringFieldModel("subtype", subtype));

            result.add(entityModel);
        });

        return result;
    }

    private static String getStringOrBlank(JSONObject jsonObject, String key){
        if(jsonObject.has(key) && !jsonObject.isNull(key)){
            return jsonObject.getString(key);
        } else {
            return "";
        }
    }

}