package com.hpe.adm.octane.ideplugins.intellij.ui.treetable;

import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.intellij.util.ui.tree.AbstractTreeModel;

import javax.swing.tree.TreePath;
import java.util.*;

public class EntityTreeModel extends AbstractTreeModel {

    private static final String ROOT = "root";

    public enum EntityCategory {
        BACKLOG("Backlog", Entity.STORY, Entity.DEFECT),
        TASK("Tasks", Entity.TASK),
        TEST("Tests", Entity.GHERKIN_TEST, Entity.MANUAL_TEST);

        private String name;
        private List<Entity> entityTypes = new ArrayList<>();

        EntityCategory(String name, Entity... entityTypes){
            this.name = name;
            this.entityTypes = Arrays.asList(entityTypes);
        }

        public List<Entity> getEntityTypes() {
            return entityTypes;
        }

        public static EntityCategory getCategory(EntityModel entityModel){
            for(EntityCategory category : EntityCategory.values()) {
                if (category.getEntityTypes().contains(Entity.getEntityType(entityModel))) {
                    return category;
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }
    }

    private TreeMap<EntityCategory, List<EntityModel>> groupedEntities = new TreeMap<>();

    public EntityTreeModel(){
        init();
    }

    public EntityTreeModel(Collection<EntityModel> entityModels){
        init();
        setEntities(entityModels);
    }

    private void init(){
        groupedEntities.put(EntityCategory.BACKLOG, new ArrayList<>());
        groupedEntities.put(EntityCategory.TASK, new ArrayList<>());
        groupedEntities.put(EntityCategory.TEST, new ArrayList<>());
    }

    private void clear(){
        groupedEntities.get(EntityCategory.BACKLOG).clear();
        groupedEntities.get(EntityCategory.TASK).clear();
        groupedEntities.get(EntityCategory.TEST).clear();
    }

    public void setEntities(Collection<EntityModel> entityModels){
        clear();

        for(EntityModel entityModel : entityModels){
            EntityCategory category = EntityCategory.getCategory(entityModel);

            if(category != null) {
                groupedEntities.get(category).add(entityModel);
            }
        }
    }

    @Override
    public Object getRoot() {
        return ROOT;
    }

    @Override
    public Object getChild(Object parent, int index) {

        if(parent.equals(getRoot())){
            //TreeMap keeps the keys sorted in the same manner all the time
            return groupedEntities.keySet().toArray(new EntityCategory[]{})[index];
        }
        else if(parent instanceof EntityCategory){
            return groupedEntities.get(parent).get(index);
        }

        //Entities cannot have children with this type of model
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if(parent.equals(getRoot())){
            return groupedEntities.keySet().size();
        }
        else if(parent instanceof EntityCategory){
            return groupedEntities.get(parent).size();
        }

        //Entities cannot have children with this type of model
        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        if(node.equals(getRoot())){
           return false;
        }
        else if(node instanceof EntityCategory){
            return groupedEntities.get(node) == null || groupedEntities.get(node).size() == 0;
        }
        return true;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        //not needed
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if(parent.equals(getRoot())){
            List<EntityCategory> categories = new ArrayList<>(groupedEntities.keySet());
            return categories.indexOf(child);
        }
        else if(parent instanceof EntityCategory){
            return groupedEntities.get(parent).indexOf(child);
        }

        //root
        return 0;
    }

}