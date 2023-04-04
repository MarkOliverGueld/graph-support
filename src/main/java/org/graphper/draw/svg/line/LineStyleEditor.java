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

package org.graphper.draw.svg.line;

import org.graphper.api.LineAttrs;
import org.graphper.api.attributes.ArrowShape;
import org.graphper.api.attributes.Color;
import org.graphper.api.attributes.LineStyle;
import org.graphper.draw.LineDrawProp;
import org.graphper.draw.LineEditor;
import org.graphper.draw.svg.Element;
import org.graphper.draw.svg.SvgBrush;
import org.graphper.draw.svg.SvgConstants;

public class LineStyleEditor implements LineEditor<SvgBrush> {

  @Override
  public boolean edit(LineDrawProp line, SvgBrush brush) {
    setArrowProp(line.lineAttrs(), brush);

    LineStyle style = line.lineAttrs().getStyle();
    if (style == null) {
      return true;
    }

    if (style == LineStyle.INVIS) {
      brush.drawBoard().removeLine(line.getLine());
      return false;
    }

    Element pathEle = brush.getOrCreateChildElementById(
        SvgBrush.getId(brush.lineId(line), SvgConstants.PATH_ELE),
        SvgConstants.PATH_ELE
    );

    if (style == LineStyle.DASHED) {
      dashed(pathEle);
      return true;
    }

    if (style == LineStyle.DOTTED) {
      dotted(pathEle);
      return true;
    }

    if (style == LineStyle.BOLD) {
      bold(pathEle);
      return true;
    }

    return true;
  }

  private void setArrowProp(LineAttrs lineAttrs, SvgBrush brush) {
    Double penWidth = lineAttrs.getPenWidth();

    Color color = lineAttrs.getColor();
    ArrowShape arrowHead = lineAttrs.getArrowHead();
    ArrowShape arrowTail = lineAttrs.getArrowTail();
    for (Element ele : brush.getEleGroup(SvgConstants.TAIL_ARROW_GROUP_KEY)) {
      setArrowStyle(penWidth, color, ele, arrowTail.isNeedFill());
    }
    for (Element ele : brush.getEleGroup(SvgConstants.HEAD_ARROW_GROUP_KEY)) {
      setArrowStyle(penWidth, color, ele, arrowHead.isNeedFill());
    }
  }

  private static void setArrowStyle(Double penWidth, Color color, Element ele, boolean needFill) {
    ele.setAttribute(SvgConstants.STROKE, color.value());
    if (needFill) {
      ele.setAttribute(SvgConstants.FILL, color.value());
    }
    if (penWidth == null) {
      return;
    }
    ele.setAttribute(SvgConstants.STROKE_WIDTH, String.valueOf(penWidth));
  }

  private void dashed(Element pathEle) {
    pathEle.setAttribute(SvgConstants.FILL, SvgConstants.NONE);
    pathEle.setAttribute(SvgConstants.STROKE_DASHARRAY, "5,2");
  }

  private void dotted(Element pathEle) {
    pathEle.setAttribute(SvgConstants.FILL, SvgConstants.NONE);
    pathEle.setAttribute(SvgConstants.STROKE_DASHARRAY, "1,5");
  }

  private void bold(Element shape) {
    shape.setAttribute(SvgConstants.STROKE_WIDTH, "2");
  }
}
