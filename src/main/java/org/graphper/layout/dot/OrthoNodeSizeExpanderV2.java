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

package org.graphper.layout.dot;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.graphper.def.FlatPoint;
import org.graphper.draw.DrawGraph;
import org.graphper.draw.LineDrawProp;
import org.graphper.draw.NodeDrawProp;

public class OrthoNodeSizeExpanderV2 extends PortNodeSizeExpanderV2 {

  private Map<GroupKey, List<GroupEntry>> groupKeyListMap;

  public OrthoNodeSizeExpanderV2(DrawGraph drawGraph, DNode node) {
    super(drawGraph, node);
  }

  @Override
  protected Map<GroupKey, List<GroupEntry>> groupSelfLine(DrawGraph drawGraph, DNode node,
                                                          NodeDrawProp nodeDrawProp) {
    groupKeyListMap = super.groupSelfLine(drawGraph, node, nodeDrawProp);
    return groupKeyListMap;
  }

  public void drawSelfLine(DrawGraph drawGraph) {
    if (groupKeyListMap == null) {
      return;
    }
    double interval = minSelfInterval(node) / 2;
    double topHeight = node.realTopHeight();
    double bottomHeight = node.realBottomHeight();
    double topHeightOffset = 0;
    double bottomHeightOffset = 0;
    double rightWidthOffset = 0;

    for (Entry<GroupKey, List<GroupEntry>> entry : groupKeyListMap.entrySet()) {
      GroupKey key = entry.getKey();
      FlatPoint tailPoint = adjustPortPoint(key.getTailPoint(), node);
      FlatPoint headPoint = adjustPortPoint(key.getHeadPoint(), node);
      List<GroupEntry> groupLines = entry.getValue();
      double nodeInternalInterval = node.getWidth() / (groupLines.size() + 1);

      for (int lineNo = 0; lineNo < groupLines.size(); lineNo++) {
        GroupEntry groupEntry = groupLines.get(lineNo);
        DLine line = groupEntry.getLine();

        LineDrawProp lineDrawProp = drawGraph.getLineDrawProp(line.getLine());
        if (lineDrawProp == null || lineDrawProp.isInit()) {
          continue;
        }

        lineDrawProp.clear();

        FlatPoint labelSize = line.getLabelSize();
        double height = 0;
        double width = 0;

        if (labelSize != null) {
          height = labelSize.getHeight();
          width = labelSize.getWidth();
        }

        topHeightOffset = Math.max(topHeight + topHeightOffset + interval, height / 2) - topHeight;
        bottomHeightOffset =
            Math.max(bottomHeight + bottomHeightOffset + interval, height / 2) - bottomHeight;
        rightWidthOffset += Math.max(width, interval);

        double right = node.getRightBorder() + rightWidthOffset;
        double top = node.getUpBorder() - topHeightOffset;
        double bottom = node.getDownBorder() + bottomHeightOffset;

        if (key.havePortOrCell()) {
          lineDrawProp.add(new FlatPoint(tailPoint.getX(), tailPoint.getY()));
          lineDrawProp.add(new FlatPoint(tailPoint.getX(), top));
          lineDrawProp.add(new FlatPoint(right, top));
          lineDrawProp.add(new FlatPoint(right, bottom));
          lineDrawProp.add(new FlatPoint(headPoint.getX(), bottom));
          lineDrawProp.add(new FlatPoint(headPoint.getX(), headPoint.getY()));
        } else {
          double left = node.getRightBorder() - nodeInternalInterval * (lineNo + 1);
          FlatPoint center = new FlatPoint(left, node.getY());
          lineDrawProp.add(center);
          lineDrawProp.add(new FlatPoint(left, top));
          lineDrawProp.add(new FlatPoint(right, top));
          lineDrawProp.add(new FlatPoint(right, bottom));
          lineDrawProp.add(new FlatPoint(left, bottom));
          lineDrawProp.add(center);
        }

        if (labelSize != null) {
          lineDrawProp.setLabelCenter(new FlatPoint(right - labelSize.getWidth() / 2, node.getY()));
        }
      }
    }
  }

  private FlatPoint adjustPortPoint(FlatPoint point, DNode node) {
    point.setX(point.getX() + node.getX());
    point.setY(point.getY() + node.getY());
    return point;
  }
}
