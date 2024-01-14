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

import static org.graphper.draw.svg.SvgConstants.POLYGON_ELE;

import org.graphper.api.attributes.NodeShapeEnum;
import org.graphper.api.ext.Box;
import org.graphper.draw.ClusterDrawProp;
import org.graphper.draw.CustomizeShapeRender;
import org.graphper.draw.NodeDrawProp;
import org.graphper.draw.svg.Element;
import org.graphper.draw.svg.SvgBrush;
import org.graphper.draw.svg.SvgConstants;
import org.graphper.draw.svg.SvgEditor;

public class TrapeziumShapeRender extends CustomizeShapeRender {

  @Override
  public String getShapeName() {
    return NodeShapeEnum.TRAPEZIUM.getName();
  }

  @Override
  public void drawNodeSvg(SvgBrush nodeBrush, NodeDrawProp nodeDrawProp) {
    Element clusterEle = nodeBrush.getShapeElement(nodeDrawProp, POLYGON_ELE);
    draw(nodeDrawProp, clusterEle);
  }

  @Override
  public void drawClusterSvg(SvgBrush clusterBrush, ClusterDrawProp clusterDrawProp) {
    Element clusterEle = clusterBrush.getShapeElement(clusterDrawProp, POLYGON_ELE);
    draw(clusterDrawProp, clusterEle);
  }

  private void draw(Box box, Element shapeElement) {
    double leftTopX = box.getLeftBorder() + box.getWidth() / 4;
    double rightTopX = box.getRightBorder() - box.getWidth() / 4;
    String points = SvgEditor.generatePolylinePoints(box.getLeftBorder(), box.getDownBorder(),
                                                     leftTopX, box.getUpBorder(),
                                                     rightTopX, box.getUpBorder(),
                                                     box.getRightBorder(), box.getDownBorder(),
                                                     box.getLeftBorder(), box.getDownBorder());
    shapeElement.setAttribute(SvgConstants.POINTS, points);
  }
}
