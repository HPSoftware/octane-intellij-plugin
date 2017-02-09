package com.hpe.adm.octane.ideplugins.services;

import com.google.inject.Inject;
import com.hpe.adm.nga.sdk.Octane;
import com.hpe.adm.nga.sdk.Query;
import com.hpe.adm.nga.sdk.metadata.FieldMetadata;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.nga.sdk.model.MultiReferenceFieldModel;
import com.hpe.adm.octane.ideplugins.services.connection.ConnectionSettingsProvider;
import com.hpe.adm.octane.ideplugins.services.connection.OctaneProvider;
import com.hpe.adm.octane.ideplugins.services.exception.ServiceException;
import com.hpe.adm.octane.ideplugins.services.exception.ServiceRuntimeException;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.hpe.adm.octane.ideplugins.services.util.EntityUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.hpe.adm.octane.ideplugins.services.filtering.Entity.*;

public class MyWorkService {

    @Inject
    private EntityService entityService;

    @Inject
    private UserService userService;

    @Inject
    private OctaneProvider octaneProvider;

    @Inject
    private ConnectionSettingsProvider connectionSettingsProvider;

    public static final String FOLLOW_ITEMS_OWNER_FIELD = "my_follow_items_owner";
    public static final String NEW_ITEMS_OWNER_FIELD = "my_new_items_owner";

    public Collection<EntityModel> getMyWork() {
        return getMyWork(new HashMap<>());
    }

    /**
     * Can specify which fields to fetch for which entity
     *
     * @param fieldListMap if there is no entry for an entity type all fields are fetched
     * @return
     */
    public Collection<EntityModel> getMyWork(Map<Entity, Set<String>> fieldListMap) {

        final Map<Entity, Set<String>> fieldListMapCopy = cloneFieldListMap(fieldListMap);

        Map<Entity, Query.QueryBuilder> filterCriteria = new HashMap<>();

        filterCriteria.put(GHERKIN_TEST,
                createCurrentUserQuery("owner")
                        .and(GHERKIN_TEST.createMatchSubtypeQueryBuilder())
                        .and(createPhaseQuery(TEST, "new", "indesign"))
        );

        filterCriteria.put(MANUAL_TEST,
                createCurrentUserQuery("owner")
                        .and(MANUAL_TEST.createMatchSubtypeQueryBuilder())
                        .and(createPhaseQuery(TEST, "new", "indesign"))

        );

        filterCriteria.put(DEFECT,
                createCurrentUserQuery("owner")
                        .and(DEFECT.createMatchSubtypeQueryBuilder())
                        .and(createPhaseQuery(DEFECT, "new", "inprogress", "intesting"))
        );

        filterCriteria.put(USER_STORY,
                createCurrentUserQuery("owner")
                        .and(USER_STORY.createMatchSubtypeQueryBuilder())
                        .and(createPhaseQuery(USER_STORY, "new", "inprogress", "intesting"))
        );

        filterCriteria.put(QUALITY_STORY,
                createCurrentUserQuery("owner")
                        .and(QUALITY_STORY.createMatchSubtypeQueryBuilder())
                        .and(createPhaseQuery(QUALITY_STORY, "new", "inprogress"))
        );

        filterCriteria.put(TASK,
                createCurrentUserQuery("owner")
                        .and(createPhaseQuery(TASK, "new", "inprogress"))
        );


        filterCriteria.put(MANUAL_TEST_RUN,
                createCurrentUserQuery("run_by")
                        .and(MANUAL_TEST_RUN.createMatchSubtypeQueryBuilder())
                        .and(new Query.QueryBuilder("parent_suite", Query::equalTo, null))
                        .and(createNativeStatusQuery("list_node.run_native_status.blocked", "list_node.run_native_status.not_completed"))
        );

        filterCriteria.put(TEST_SUITE_RUN,
                createCurrentUserQuery("run_by")
                        .and(TEST_SUITE_RUN.createMatchSubtypeQueryBuilder())
                        .and(createNativeStatusQuery("list_node.run_native_status.blocked", "list_node.run_native_status.not_completed")
                                .and(new Query.QueryBuilder("parent_suite", Query::equalTo, null)))
        );

        filterCriteria.put(COMMENT, createCurrentUserQuery("mention_user"));

        //In case it's supported, also get the work items you are following
        filterCriteria
                .keySet()
                .stream()
                .filter(this::isFollowingEntitySupported)
                .forEach(key -> {

                    Query.QueryBuilder qb;
                    if (key.isSubtype()) {
                        qb = key.createMatchSubtypeQueryBuilder().and(createCurrentUserQuery(FOLLOW_ITEMS_OWNER_FIELD))
                                .or(filterCriteria.get(key));
                    } else {
                        qb = createCurrentUserQuery(FOLLOW_ITEMS_OWNER_FIELD).or(filterCriteria.get(key));
                    }

                    filterCriteria.put(key, qb);
                    if (fieldListMapCopy != null && fieldListMapCopy.containsKey(key)) {
                        fieldListMapCopy.get(key).add(FOLLOW_ITEMS_OWNER_FIELD);
                        fieldListMapCopy.get(key).add(NEW_ITEMS_OWNER_FIELD);
                    }
                });

        Map<Entity, Collection> resultMap = new ConcurrentHashMap<>();

        //TODO, known subtypes should be under same rest call
        filterCriteria
                .keySet()
                .parallelStream()
                .forEach(
                        entityType ->
                                resultMap.put(entityType,
                                        entityService.findEntities(
                                                entityType.getApiEntityName(),
                                                filterCriteria.get(entityType),
                                                fieldListMapCopy.get(entityType)
                                        )
                                )
                );

        List<EntityModel> result = new ArrayList<>();

        resultMap
                .keySet()
                .stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(resultMap::get)
                .forEach(result::addAll);

        return result;
    }

    /**
     * Constructs a metaphase query builder to match "logical_name":"metaphase.entity.phasename",
     *
     * @param entity
     * @param phases
     * @return
     */
    private Query.QueryBuilder createPhaseQuery(Entity entity, String... phases) {
        Query.QueryBuilder phaseQueryBuilder = null;
        for (String phaseName : phases) {
            String phaseLogicalName = "metaphase." + entity.getTypeName() + "." + phaseName;
            Query.QueryBuilder currentPhaseQueryBuilder =
                    new Query.QueryBuilder("metaphase", Query::equalTo,
                            new Query.QueryBuilder("logical_name", Query::equalTo, phaseLogicalName)
                    );
            if (phaseQueryBuilder == null) {
                phaseQueryBuilder = currentPhaseQueryBuilder;
            } else {
                phaseQueryBuilder = phaseQueryBuilder.or(currentPhaseQueryBuilder);
            }
        }

        return new Query.QueryBuilder("phase", Query::equalTo, phaseQueryBuilder);
    }

    /**
     * @param logicalNames
     * @return
     */
    private Query.QueryBuilder createNativeStatusQuery(String... logicalNames) {
        Query.QueryBuilder nativeStatusQueryBuilder = null;
        for (String logicalName : logicalNames) {
            Query.QueryBuilder currentNativeStatusQueryBuilder =
                    new Query.QueryBuilder("logical_name", Query::equalTo, logicalName);
            if (nativeStatusQueryBuilder == null) {
                nativeStatusQueryBuilder = currentNativeStatusQueryBuilder;
            } else {
                nativeStatusQueryBuilder = nativeStatusQueryBuilder.or(currentNativeStatusQueryBuilder);
            }
        }
        return new Query.QueryBuilder("native_status", Query::equalTo, nativeStatusQueryBuilder);
    }

    private Query.QueryBuilder createCurrentUserQuery(String fieldName) {
        return new Query.QueryBuilder(fieldName, Query::equalTo,
                new Query.QueryBuilder("id", Query::equalTo, userService.getCurrentUserId()));
    }


    private Map<Entity, Boolean> followingSupportEntityMap;

    public boolean isFollowingEntitySupported(Entity entityType) {

        //init cache map
        if (followingSupportEntityMap == null) {
            followingSupportEntityMap = new HashMap<>();
            //Clear on settings changed
            //connectionSettingsProvider.addChangeHandler(() -> followingSupportEntityMap.clear());
        }

        if (followingSupportEntityMap.containsKey(entityType)) {
            return followingSupportEntityMap.get(entityType);
        }

        Octane octane = octaneProvider.getOctane();
        Collection<FieldMetadata> fields = octane.metadata().fields(entityType.getEntityName()).execute();
        boolean followFieldExits =
                fields.stream().anyMatch(fieldMetadata -> FOLLOW_ITEMS_OWNER_FIELD.equals(fieldMetadata.getName()));
        boolean newFieldExits =
                fields.stream().anyMatch(fieldMetadata -> NEW_ITEMS_OWNER_FIELD.equals(fieldMetadata.getName()));

        followingSupportEntityMap.put(entityType, followFieldExits && newFieldExits);
        return followFieldExits && newFieldExits;
    }

    public boolean isCurrentUserFollowing(EntityModel entityModel) {
        if (entityModel.getValue(FOLLOW_ITEMS_OWNER_FIELD) == null ||
                entityModel.getValue(FOLLOW_ITEMS_OWNER_FIELD).getValue() == null) {

            entityModel = fetchEntityFields(entityModel, FOLLOW_ITEMS_OWNER_FIELD);
        }

        EntityModel currentUser = userService.getCurrentUser();
        MultiReferenceFieldModel fieldModel = entityModel.getValue(FOLLOW_ITEMS_OWNER_FIELD);

        return EntityUtil.containsEntityModel(fieldModel.getValue(), currentUser);
    }

    public boolean addCurrentUserToFollowers(EntityModel entityModel) {
        EntityModel updateEntityModel = createUpdateEntityModelForFollow(entityModel);
        EntityModel currentUser = userService.getCurrentUser();

        MultiReferenceFieldModel fieldModelFollow = updateEntityModel.getValue(FOLLOW_ITEMS_OWNER_FIELD);
        MultiReferenceFieldModel fieldModelNew = updateEntityModel.getValue(NEW_ITEMS_OWNER_FIELD);

        if (!EntityUtil.containsEntityModel(fieldModelFollow.getValue(), currentUser)) {
            fieldModelFollow.getValue().add(currentUser);

            if (!EntityUtil.containsEntityModel(fieldModelNew.getValue(), currentUser)) {
                fieldModelNew.getValue().add(currentUser);
            }

            //Do update
            Octane octane = octaneProvider.getOctane();
            Integer id = Integer.valueOf(entityModel.getValue("id").getValue().toString());
            octane.entityList(Entity.getEntityType(entityModel).getApiEntityName())
                    .at(id)
                    .update()
                    .entity(updateEntityModel)
                    .execute();

            //Was added
            return true;
        }

        //No need to add
        return false;
    }

    public boolean removeCurrentUserFromFollowers(EntityModel entityModel) {
        EntityModel updateEntityModel = createUpdateEntityModelForFollow(entityModel);
        EntityModel currentUser = userService.getCurrentUser();
        MultiReferenceFieldModel fieldModel = updateEntityModel.getValue(FOLLOW_ITEMS_OWNER_FIELD);

        if (EntityUtil.removeEntityModel(fieldModel.getValue(), currentUser)) {
            //Do update
            Octane octane = octaneProvider.getOctane();

            try {
                Integer id = Integer.valueOf(entityModel.getValue("id").getValue().toString());
                octane.entityList(Entity.getEntityType(entityModel).getApiEntityName())
                        .at(id)
                        .update()
                        .entity(updateEntityModel)
                        .execute();

            } catch (Exception ex) {
                //Re-add it if the call failed
                fieldModel.getValue().add(currentUser);
                throw ex;
            }

            //Was removed
            return true;
        }

        //No need to remove
        return false;
    }

    private EntityModel createUpdateEntityModelForFollow(EntityModel entityModel) {

        if (entityModel.getValue(FOLLOW_ITEMS_OWNER_FIELD) == null ||
                entityModel.getValue(FOLLOW_ITEMS_OWNER_FIELD).getValue() == null ||
                entityModel.getValue(NEW_ITEMS_OWNER_FIELD) == null ||
                entityModel.getValue(NEW_ITEMS_OWNER_FIELD).getValue() == null) {

            entityModel = fetchEntityFields(entityModel, FOLLOW_ITEMS_OWNER_FIELD, NEW_ITEMS_OWNER_FIELD);
        }

        EntityModel updateEntityModel = new EntityModel();
        updateEntityModel.setValue(entityModel.getValue(FOLLOW_ITEMS_OWNER_FIELD));
        updateEntityModel.setValue(entityModel.getValue(NEW_ITEMS_OWNER_FIELD));
        return updateEntityModel;
    }

    private EntityModel fetchEntityFields(EntityModel entityModel, String... fields) {
        try {
            return entityService.findEntity(
                    Entity.getEntityType(entityModel),
                    Long.parseLong(entityModel.getValue("id").getValue().toString()),
                    new HashSet<>(Arrays.asList(fields))
            );
        } catch (ServiceException e) {
            throw new ServiceRuntimeException(e);
        }
    }

    private Map<Entity, Set<String>> cloneFieldListMap(Map<Entity, Set<String>> fieldListMap){
        Map<Entity, Set<String>> fieldListMapCopy = new HashMap<>();
        if(fieldListMap == null){
            fieldListMapCopy = null;
        } else {
            for(Entity key : fieldListMap.keySet()){
                Set<String> value = fieldListMap.get(key);
                if(value == null){
                    fieldListMapCopy.put(key, null);
                } else {
                    fieldListMapCopy.put(key, new HashSet<>(value));
                }
            }
        }
        return fieldListMapCopy;
    }
}