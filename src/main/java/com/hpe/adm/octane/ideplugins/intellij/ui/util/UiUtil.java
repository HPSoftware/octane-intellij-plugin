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

package com.hpe.adm.octane.ideplugins.intellij.ui.util;

import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.octane.ideplugins.intellij.ui.treetable.EntityTreeCellRenderer;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

/**
 * Created by marineal on 2/14/2017.
 */
public class UiUtil {

    public static void showWarningBalloon(Project project, String title, String htmlText, NotificationType type){
        Notification notification =
                new Notification(
                        "Octane IntelliJ Plugin",
                        title,
                        htmlText,
                        type,
                        null);

        notification.notify(project);
    }


    public static String entityToString(EntityModel entityModel){
        StringBuilder result = new StringBuilder();

        if(Entity.getEntityType(entityModel) != null){
            result.append(EntityTreeCellRenderer.getSubtypeName(Entity.getEntityType(entityModel).getEntityName()));
            result.append(" ");
        }

        if(entityModel.getValue("id") != null){
            result.append(entityModel.getValue("id").getValue().toString());
            result.append(": ");
        }

        if(entityModel.getValue("name") != null){
            result.append(entityModel.getValue("name").getValue().toString());
        }

        return result.toString();
    }
}
