/*
 * © 2017 EntIT Software LLC, a Micro Focus company, L.P.
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

package com.hpe.adm.octane.ideplugins.intellij.ui.entityicon;

import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.octane.ideplugins.intellij.ui.Constants;
import com.hpe.adm.octane.ideplugins.services.EntityLabelService;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.intellij.util.ImageLoader;
import org.jdesktop.swingx.JXLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class EntityIconFactory {

    //Detail for unmapped entity type
    private final IconDetail unmapedEntityIconDetail = new IconDetail(new Color(0, 0, 0, 0), "", true);

    private final String INITIALS = "initials";

    //map to color and short text
    private final Map<Entity, IconDetail> iconDetailMap = new HashMap<>();
    private final Map<Entity, JComponent> iconComponentMap = new HashMap<>();

    private int iconHeight = 30;
    private int iconWidth = 30;
    private Color fontColor = new Color(255, 255, 255);
    private int fontSize = 15;

    private EntityLabelService entityLabelService;

    private static final Image activeImg = ImageLoader.loadFromResource(Constants.IMG_ACTIVE_ITEM);

    public EntityIconFactory() {
        init();
    }

    public EntityIconFactory(EntityLabelService entityLabelService, int iconHeight, int iconWidth, int fontSize) {
        this.entityLabelService = entityLabelService;
        this.iconHeight = iconHeight;
        this.iconWidth = iconWidth;
        this.fontSize = fontSize;
        init();
    }

    private void init() {
        Map<String, EntityModel> entityLabels = entityLabelService.getEntityLabelDetails();

        iconDetailMap.put(Entity.USER_STORY, new IconDetail(new Color(255, 176, 0),
                entityLabels.get(Entity.USER_STORY.getSubtypeName()).getValue(INITIALS).getValue().toString()));
        iconDetailMap.put(Entity.QUALITY_STORY, new IconDetail(new Color(51, 193, 128),
                entityLabels.get(Entity.QUALITY_STORY.getEntityName()).getValue(INITIALS).getValue().toString()));
        iconDetailMap.put(Entity.DEFECT, new IconDetail(new Color(178, 22, 70),
                entityLabels.get(Entity.DEFECT.getEntityName()).getValue(INITIALS).getValue().toString()));
        iconDetailMap.put(Entity.EPIC, new IconDetail(new Color(116, 37, 173),
                entityLabels.get(Entity.EPIC.getEntityName()).getValue(INITIALS).getValue().toString()));
        iconDetailMap.put(Entity.FEATURE, new IconDetail(new Color(229, 120, 40),
                entityLabels.get(Entity.FEATURE.getEntityName()).getValue(INITIALS).getValue().toString()));

        iconDetailMap.put(Entity.TASK, new IconDetail(new Color(22, 104, 193),
                entityLabels.get(Entity.TASK.getEntityName()).getValue(INITIALS).getValue().toString()));

        iconDetailMap.put(Entity.MANUAL_TEST, new IconDetail(new Color(0, 171, 243),
                entityLabels.get(Entity.MANUAL_TEST.getEntityName()).getValue(INITIALS).getValue().toString()));
        iconDetailMap.put(Entity.GHERKIN_TEST, new IconDetail(new Color(0, 169, 137),
                entityLabels.get(Entity.GHERKIN_TEST.getEntityName()).getValue(INITIALS).getValue().toString()));

        iconDetailMap.put(Entity.TEST_SUITE, new IconDetail(new Color(39, 23, 130),
                entityLabels.get(Entity.TEST_SUITE.getEntityName()).getValue(INITIALS).getValue().toString()));
        iconDetailMap.put(Entity.MANUAL_TEST_RUN, new IconDetail(new Color(0, 171, 243),
                entityLabels.get(Entity.MANUAL_TEST_RUN.getEntityName()).getValue(INITIALS).getValue().toString()));
        iconDetailMap.put(Entity.TEST_SUITE_RUN, new IconDetail(new Color(0, 171, 243),
                entityLabels.get(Entity.TEST_SUITE_RUN.getEntityName()).getValue(INITIALS).getValue().toString()));
        iconDetailMap.put(Entity.AUTOMATED_TEST, new IconDetail(new Color(186, 71, 226),
                entityLabels.get(Entity.AUTOMATED_TEST.getEntityName()).getValue(INITIALS).getValue().toString()));


        iconDetailMap.put(Entity.COMMENT, new IconDetail(new Color(253, 225, 89),
                entityLabels.get(Entity.COMMENT.getEntityName()).getValue(INITIALS).getValue().toString()));
        iconDetailMap.put(Entity.REQUIREMENT, new IconDetail(new Color(11, 142, 172),
                entityLabels.get(Entity.REQUIREMENT.getTypeName()).getValue(INITIALS).getValue().toString()));

        iconDetailMap.keySet().forEach(entity -> iconComponentMap.put(entity, createIconAsComponent(entity)));
    }

    private JComponent createIconAsComponent(Entity entity) {
        //Make the label
        Font defaultFont = new JXLabel().getFont();
        JXLabel label = new JXLabel(new ImageIcon(createIconAsImage(entity)));
        label.setPreferredSize(new Dimension(iconWidth, iconHeight));
        label.setMinimumSize(new Dimension(iconWidth, iconHeight));
        label.setMaximumSize(new Dimension(iconWidth, iconHeight));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setBounds(0, 0, iconWidth, iconHeight);
        return label;
    }

    private Image createIconAsImage(Entity entity) {
        IconDetail iconDetail = iconDetailMap.containsKey(entity) ? iconDetailMap.get(entity) : unmapedEntityIconDetail;

        BufferedImage image = new BufferedImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = image.createGraphics();
        // make BufferedImage fully transparent
        bg.setComposite(AlphaComposite.Clear);
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bg.fillRect(0, 0, iconWidth, iconHeight);
        bg.setComposite(AlphaComposite.SrcOver);
        bg.setColor(iconDetail.getColor());

        bg.fillOval(0, 0, iconWidth, iconHeight);
        bg.setColor(fontColor);
        bg.setFont(new Font("Arial", Font.BOLD, fontSize));

        FontMetrics fm = bg.getFontMetrics();
        int fontX = (iconWidth - fm.stringWidth(iconDetail.getDisplayLabelText())) / 2;
        int fontY = (fm.getAscent() + (iconHeight - (fm.getAscent() + fm.getDescent())) / 2);

        bg.drawString(iconDetail.getDisplayLabelText(), fontX, fontY);

        return image;
    }

    public JComponent getIconAsComponent(Entity entity) {
        return iconComponentMap.get(entity);
    }

    public JComponent getIconAsComponent(Entity entity, boolean isActive) {
        if (!isActive) {
            return getIconAsComponent(entity);
        } else {
            //Overlay the run image on top of the original entity icon component
            JComponent component = getIconAsComponent(entity);

            if (component == null) {
                return new JLabel("N/A");
            }

            component.setBounds(0, 0, iconWidth, iconHeight);

            //Overlay an image on top of the component
            JPanel runImagePanel = new JPanel() {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.drawImage(activeImg, 0, 0, getWidth(), getHeight(), this);
                }
            };

            int xpercent = 60 * iconWidth / 100;
            int ypercent = 60 * iconWidth / 100;

            runImagePanel.setBounds(
                    xpercent,
                    ypercent,
                    iconWidth - xpercent,
                    iconHeight - ypercent);
            runImagePanel.setOpaque(false);

            JPanel panel = new JPanel(null);
            panel.setBorder(null);
            panel.setOpaque(false);
            panel.add(runImagePanel);
            panel.add(component);

            panel.setPreferredSize(new Dimension(iconWidth, iconHeight));
            panel.setMinimumSize(new Dimension(iconWidth, iconHeight));
            panel.setMaximumSize(new Dimension(iconWidth, iconHeight));
            return panel;
        }
    }

    public Image getIconAsImage(Entity entity, boolean isActive) {
        return createIconAsImage(entity);
    }

    public Image getIconAsImage(Entity entity) {
        return createIconAsImage(entity);
    }

}