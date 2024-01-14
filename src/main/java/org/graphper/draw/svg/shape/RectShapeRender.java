/*
 * Copyright 2022 The graph-support project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.graphper.draw.svg.shape;

import static org.graphper.draw.svg.SvgConstants.D;
import static org.graphper.draw.svg.SvgConstants.PATH_ELE;
import static org.graphper.draw.svg.SvgConstants.POLYGON_ELE;
import static org.graphper.draw.svg.SvgConstants.SHAPE_GROUP_KEY;
import static org.graphper.draw.svg.SvgConstants.STROKE_WIDTH;

import org.graphper.api.Cluster;
import org.graphper.api.attributes.ClusterStyle;
import org.graphper.api.attributes.NodeShapeEnum;
import org.graphper.draw.ClusterDrawProp;
import org.graphper.draw.CustomizeShapeRender;
import org.graphper.draw.NodeDrawProp;
import org.graphper.draw.svg.Element;
import org.graphper.draw.svg.SvgBrush;
import org.graphper.draw.svg.SvgConstants;
import org.graphper.draw.svg.SvgEditor;

public class RectShapeRender extends CustomizeShapeRender {

  private static final int MAX_CLUSTER_ROUNDED = 60;

  @Override
  public String getShapeName() {
    return NodeShapeEnum.RECT.getName();
  }

  @Override
  public void drawNodeSvg(SvgBrush nodeBrush, NodeDrawProp nodeDrawProp) {
    Element shapeEle = nodeBrush.getShapeElement(nodeDrawProp, POLYGON_ELE);
    String points = SvgEditor.generateBox(nodeDrawProp);
    shapeEle.setAttribute(SvgConstants.POINTS, points);
  }

  @Override
  public void drawClusterSvg(SvgBrush clusterBrush, ClusterDrawProp clusterDrawProp) {
    Element clusterEle;
    Cluster cluster = clusterDrawProp.getCluster();
    ClusterStyle style = cluster.clusterAttrs().getStyle();
    String points;

    if (style == ClusterStyle.ROUNDED) {
      clusterEle = clusterBrush.getShapeElement(clusterDrawProp, PATH_ELE);
      points = SvgEditor.roundedBox(MAX_CLUSTER_ROUNDED, clusterDrawProp);
      clusterEle.setAttribute(D, points);
    } else {
      clusterEle = clusterBrush.getShapeElement(clusterDrawProp, POLYGON_ELE);
      points = SvgEditor.generateBox(clusterDrawProp);
      clusterEle.setAttribute(SvgConstants.POINTS, points);
    }

    clusterBrush.addGroup(SHAPE_GROUP_KEY, clusterEle);
    double penWidth = cluster.clusterAttrs().getPenWidth();
    clusterEle.setAttribute(STROKE_WIDTH, String.valueOf(penWidth));
  }
}
