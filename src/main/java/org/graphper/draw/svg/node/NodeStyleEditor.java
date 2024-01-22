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

package org.graphper.draw.svg.node;

import java.util.Collection;
import org.graphper.api.NodeAttrs;
import org.graphper.api.attributes.NodeShapeEnum;
import org.graphper.api.attributes.NodeStyle;
import org.graphper.draw.NodeDrawProp;
import org.graphper.draw.svg.Element;
import org.graphper.draw.svg.SvgBrush;
import org.graphper.draw.svg.SvgConstants;
import org.graphper.util.CollectionUtils;

public class NodeStyleEditor extends AbstractNodeShapeEditor {

  @Override
  public boolean edit(NodeDrawProp node, SvgBrush brush) {
    for (Element element : brush.getEleGroup(SHAPE_GROUP_KEY)) {
      setStyle(node, brush, element);
    }
    return true;
  }

  private void setStyle(NodeDrawProp node, SvgBrush brush, Element element) {
    NodeAttrs nodeAttrs = node.nodeAttrs();

    Double penWidth = nodeAttrs.getPenWidth();
    if (penWidth != null) {
      element.setAttribute(STROKE_WIDTH, String.valueOf(penWidth));
    }

    Collection<NodeStyle> styles = nodeAttrs.getStyles();
    if (CollectionUtils.isEmpty(styles)) {
      element.setAttribute(SvgConstants.FILL, SvgConstants.NONE);
      pointAddFillStyle(node, brush, nodeAttrs, element);
      return;
    }

    for (NodeStyle style : styles) {
      element.setAttribute(SvgConstants.FILL, SvgConstants.NONE);
      drawStyle(brush, node, element, style);
    }

    pointAddFillStyle(node, brush, nodeAttrs, element);
  }

  private void pointAddFillStyle(NodeDrawProp node, SvgBrush brush, NodeAttrs nodeAttrs,
                                 Element shapeElement) {
    if (nodeAttrs.getNodeShape() == NodeShapeEnum.POINT) {
      drawStyle(brush, node, shapeElement, NodeStyle.SOLID);
    }
  }

  private void drawStyle(SvgBrush brush, NodeDrawProp node, Element shape, NodeStyle nodeStyle) {
    switch (nodeStyle) {
      case DASHED:
        dashed(shape);
        break;
      case DOTTED:
        dotted(shape);
        break;
      case BOLD:
        bold(shape);
        break;
      default:
        break;
    }
  }

  private void dashed(Element shape) {
    if (shape.getAttribute(SvgConstants.FILL) == null) {
      shape.setAttribute(SvgConstants.FILL, SvgConstants.NONE);
    }
    shape.setAttribute(SvgConstants.STROKE_DASHARRAY, "5,2");
  }

  private void dotted(Element shape) {
    if (shape.getAttribute(SvgConstants.FILL) == null) {
      shape.setAttribute(SvgConstants.FILL, SvgConstants.NONE);
    }
    shape.setAttribute(SvgConstants.STROKE_DASHARRAY, "1,5");
  }

  private void bold(Element shape) {
    shape.setAttribute(SvgConstants.STROKE_WIDTH, "2");
  }
}