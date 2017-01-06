package com.hpe.adm.octane.ideplugins.intellij.ui.detail;

import com.google.inject.Inject;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.nga.sdk.model.ReferenceFieldModel;
import com.hpe.adm.octane.ideplugins.intellij.ui.Presenter;
import com.hpe.adm.octane.ideplugins.intellij.ui.util.UiUtil;
import com.hpe.adm.octane.ideplugins.intellij.util.Constants;
import com.hpe.adm.octane.ideplugins.intellij.util.RestUtil;
import com.hpe.adm.octane.ideplugins.services.EntityService;
import com.hpe.adm.octane.ideplugins.services.exception.ServiceException;
import com.hpe.adm.octane.ideplugins.services.exception.ServiceRuntimeException;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public class EntityDetailPresenter implements Presenter<EntityDetailView> {

    private EntityDetailView entityDetailView;
    @Inject
    private EntityService entityService;
    private Entity entityType;
    private Long entityId;


    public EntityDetailPresenter() {
    }

    public EntityDetailView getView() {
        return entityDetailView;
    }

    @Override
    @Inject
    public void setView(EntityDetailView entityDetailView) {
        this.entityDetailView = entityDetailView;
    }

    public void setEntity(Entity entityType, Long entityId) {
        this.entityType = entityType;
        this.entityId = entityId;

        RestUtil.runInBackground(
                () -> {
                    try {
                        return entityService.findEntity(entityType, entityId);
                    } catch (ServiceException ex) {
                        entityDetailView.setErrorMessage(ex.getMessage());
                        throw new ServiceRuntimeException(ex.getMessage());
                    }
                },
                (entityModel) -> {
                    entityDetailView.setEntityModel(entityModel);
                    entityDetailView.setSaveSelectedPhaseButton(new SaveSelectedPhaseAction());
                    entityDetailView.setRefreshEntityButton(new EntityRefreshAction());
                    setPossibleTransitions(entityModel);
                },
                null,
                "Failed to fetch entity: " + entityType.name() + ": " + entityId,
                "Loading entity " + entityType.name() + ": " + entityId);
    }

    private void setPossibleTransitions(EntityModel entityModel) {
        Collection<EntityModel> result = new HashSet<>();
        RestUtil.runInBackground(() -> {
            Long currentPhaseId = Long.valueOf(UiUtil.getUiDataFromModel(entityModel.getValue("phase"), "id"));
            return entityService.findPossibleTransitionFromCurrentPhase(Entity.getEntityType(entityModel), currentPhaseId);
        },(possibleTransitions) -> {
            if(possibleTransitions.isEmpty()){
                possibleTransitions.add(new EntityModel("target_phase","No transition"));
                entityDetailView.setPossiblePhasesForEntity(possibleTransitions);
                entityDetailView.removeSaveSelectedPhaseButton();
            }else{
                entityDetailView.setPossiblePhasesForEntity(possibleTransitions);
            }
        },null,"Failed to get possible transitions","fetching possible transitions");
    }

    private final class EntityRefreshAction extends AnAction {
        public EntityRefreshAction() {
            super("Refresh current entity", "this will refresh the current entity", IconLoader.findIcon(Constants.IMG_REFRESH_ICON));
        }
        public void actionPerformed(AnActionEvent e) {
            entityDetailView.doRefresh();
            setEntity(entityType, entityId);
        }
    }
    private final class SaveSelectedPhaseAction extends AnAction {
        public SaveSelectedPhaseAction() {
            super("Save selected phase", "this will save the new phase entity", IconLoader.findIcon("/actions/menu-saveall.png"));
        }
        public void actionPerformed(AnActionEvent e) {
            RestUtil.runInBackground(() -> {
                EntityModel selectedTransition = entityDetailView.getSelectedTransition();
                ReferenceFieldModel nextPhase =  selectedTransition.getValue("target_phase");
                return nextPhase;
            },(nextPhase)->{
                entityService.updateEntityPhase(entityDetailView.getEntityModel(),nextPhase);
                entityDetailView.doRefresh();
                setEntity(entityType, entityId);
            },null,"Failed to move to next phase","Moving to next phase");

        }
    }

}
