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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.graphper.api.LineAttrs;
import org.graphper.api.attributes.Rankdir;
import org.graphper.def.FlatPoint;
import org.graphper.draw.DrawGraph;
import org.graphper.draw.LineDrawProp;
import org.graphper.draw.NodeDrawProp;
import org.graphper.layout.Cell;
import org.graphper.layout.Cell.RootCell;
import org.graphper.layout.FlipShifterStrategy;
import org.graphper.util.Asserts;
import org.graphper.util.CollectionUtils;
import org.graphper.util.ValueUtils;
import org.graphper.api.attributes.Port;

public class PortNodeSizeExpanderV2 extends NodeSizeExpander {

  private static final int LEFT = 0;

  private static final int RIGHT = 1;

  private static final int UP = 2;

  private static final int DOWN = 3;

  public PortNodeSizeExpanderV2(DrawGraph drawGraph, DNode node) {
    Asserts.nullArgument(node, "node");
    Asserts.illegalArgument(node.isVirtual(), "Node is virtual node");
    Asserts.illegalArgument(!node.haveSelfLine(), "Node do not have self lines");
    this.node = node;

    NodeDrawProp nodeDrawProp = drawGraph.getNodeDrawProp(node.getNode());
    Asserts.illegalArgument(nodeDrawProp == null, "Not found the node draw properties!");

    initExpander(drawGraph, groupSelfLine(drawGraph, node, nodeDrawProp));
  }

  protected Map<GroupKey, List<GroupEntry>> groupSelfLine(DrawGraph drawGraph, DNode node,
                                                        NodeDrawProp nodeDrawProp) {
    Map<GroupKey, List<GroupEntry>> selfLineGroup = new HashMap<>(1);

    for (int i = 0; i < node.getSelfLoopCount(); i++) {
      DLine selfLine = node.selfLine(i);
      LineDrawProp line = drawGraph.getLineDrawProp(selfLine.getLine());
      if (line == null) {
        continue;
      }

      LineAttrs lineAttrs = line.lineAttrs();
      GroupKey key = newGroupKey(lineAttrs.getTailPort(), lineAttrs.getHeadPort(), nodeDrawProp,
                                 drawGraph, lineAttrs.getTailCell(), lineAttrs.getHeadCell());

      addLineToGroup(selfLineGroup, selfLine, key);
    }

    return selfLineGroup;
  }

  private void initExpander(DrawGraph drawGraph, Map<GroupKey, List<GroupEntry>> selfGroups) {
    for (Entry<GroupKey, List<GroupEntry>> entry : selfGroups.entrySet()) {
      List<GroupEntry> groupEntries = entry.getValue();
      double interval = minSelfInterval(groupEntries.size(), node.getNodeSep());
      double nextInterval = interval;

      for (GroupEntry groupEntry : groupEntries) {
        DLine dLine = groupEntry.line;
        GroupKey groupKey = groupEntry.groupKey;
        LineDrawProp line = drawGraph.getLineDrawProp(dLine.getLine());

        nextInterval += setSamePointLine(groupKey, drawGraph.rankdir(),
                                         nextInterval, line, dLine);

        nextInterval += setUpDownLine(groupKey, drawGraph.rankdir(),
                                      nextInterval, line, dLine);

        nextInterval += setLeftRightLine(groupKey, drawGraph.rankdir(),
                                         nextInterval, line, dLine);

        nextInterval += interval;
      }
    }
  }

  private double setSamePointLine(GroupKey groupKey, Rankdir rankdir,
                                  double interval, LineDrawProp line, DLine dLine) {
    if (!groupKey.samePoint()) {
      return 0;
    }

    Port port = groupKey.tailPort;
    FlatPoint point = groupKey.getTailPoint();
    int direction = portDirection(port, rankdir);
    addPoint(line, point);

    if (direction == LEFT) {
      double leftBorder = node.getLeftBorder();
      addPoint(line, new FlatPoint(leftBorder - interval, point.getY()));
      return addLabelByLastPoint(true, false, dLine, line);
    }

    if (direction == RIGHT) {
      double rightBorder = node.getRightBorder();
      addPoint(line, new FlatPoint(rightBorder + interval, point.getY()));
      return addLabelByLastPoint(true, true, dLine, line);
    }

    if (direction == UP) {
      double upBorder = node.getUpBorder();
      addPoint(line, new FlatPoint(point.getX(), upBorder - interval));
      return addLabelByLastPoint(false, false, dLine, line);
    }

    if (direction == DOWN) {
      double downBorder = node.getDownBorder();
      addPoint(line, new FlatPoint(point.getX(), downBorder + interval));
    }
    return addLabelByLastPoint(false, true, dLine, line);
  }

  private double setUpDownLine(GroupKey groupKey, Rankdir rankdir,
                               double interval, LineDrawProp line, DLine dLine) {
    if (!groupKey.isOnlySameHor()) {
      return 0;
    }

    FlatPoint tailPoint = groupKey.getTailPoint();
    FlatPoint headPoint = groupKey.getHeadPoint();
    addPoint(line, tailPoint);

    double x = (tailPoint.getX() + headPoint.getX()) / 2;
    int direction = sameCellHorDirection(groupKey.tailPort, rankdir);
    if (direction == UP) {
      double upBorder = node.getUpBorder();
      addPoint(line, new FlatPoint(x, upBorder - interval));
      interval = addLabelByLastPoint(false, false, dLine, line);
    }

    if (direction == DOWN) {
      double downBorder = node.getDownBorder();
      addPoint(line, new FlatPoint(x, downBorder + interval));
      interval = addLabelByLastPoint(false, true, dLine, line);
    }

    addPoint(line, headPoint);
    return interval;
  }

  private double setLeftRightLine(GroupKey groupKey, Rankdir rankdir,
                                  double interval, LineDrawProp line, DLine dLine) {
    if (groupKey.samePoint() || groupKey.isOnlySameHor()) {
      return 0;
    }

    FlatPoint tailPoint = groupKey.getTailPoint();
    FlatPoint headPoint = groupKey.getHeadPoint();
    addPoint(line, tailPoint);

    double y = (tailPoint.getY() + headPoint.getY()) / 2;
    int direction;
    if (groupKey.isOnlySameVer()) {
      direction = sameCellVerDirection(groupKey.tailPort, rankdir);
    } else if (groupKey.sameCell()) {
      direction = sameCellHorDirection(groupKey.tailPort, groupKey.headPort, rankdir);
    } else {
      direction = diagonalPointDirection(groupKey);
    }

    if (direction == LEFT) {
      double leftBorder = node.getLeftBorder();
      addPoint(line, new FlatPoint(leftBorder - interval, y));
      interval = addLabelByLastPoint(true, false, dLine, line);
    }

    if (direction == RIGHT) {
      double rightBorder = node.getRightBorder();
      addPoint(line, new FlatPoint(rightBorder + interval, y));
      interval = addLabelByLastPoint(true, true, dLine, line);
    }

    addPoint(line, headPoint);
    return interval;
  }

  private double addLabelByLastPoint(boolean isHor, boolean isAdd,
                                     DLine dLine, LineDrawProp line) {
    if (dLine.getLabelSize() == null || CollectionUtils.isEmpty(line)) {
      return 0;
    }

    FlatPoint lastPoint = line.get(line.size() - 1);
    FlatPoint labelSize = dLine.getLabelSize();

    return addLabel(isHor, isAdd, line, lastPoint, labelSize);
  }

  private double addLabel(boolean isHor, boolean isAdd, LineDrawProp line,
                          FlatPoint lastPoint, FlatPoint labelSize) {
    FlatPoint labelCenter;
    if (isHor) {
      if (isAdd) {
        labelCenter = new FlatPoint(lastPoint.getX() + labelSize.getWidth() / 2,
                                    lastPoint.getY());
      } else {
        labelCenter = new FlatPoint(lastPoint.getX() - labelSize.getWidth() / 2,
                                    lastPoint.getY());
      }
      addLabel(line, labelSize, labelCenter);
      return labelSize.getWidth();
    }

    if (isAdd) {
      labelCenter = new FlatPoint(lastPoint.getX(),
                                  lastPoint.getY() + labelSize.getHeight() / 2);
    } else {
      labelCenter = new FlatPoint(lastPoint.getX(),
                                  lastPoint.getY() - labelSize.getHeight() / 2);
    }
    addLabel(line, labelSize, labelCenter);
    return labelSize.getHeight();
  }

  private static void addLineToGroup(Map<GroupKey, List<GroupEntry>> selfLineGroup,
                                     DLine selfLine, GroupKey groupKey) {
    selfLineGroup.compute(groupKey, (g, v) -> {
      if (v == null) {
        v = new ArrayList<>(1);
      }
      v.add(new GroupEntry(groupKey, selfLine));
      return v;
    });
  }

  private void addLabel(LineDrawProp line, FlatPoint labelSize, FlatPoint labelCenter) {
    line.setLabelCenter(labelCenter);
    refreshByLabel(labelSize, labelCenter);
  }

  private void refreshByLabel(FlatPoint labelSize, FlatPoint labelCenter) {
    double halfWidth = labelSize.getWidth() / 2;
    double halfHeight = labelSize.getHeight() / 2;
    refreshVolume(labelCenter.getX() - halfWidth, labelCenter.getY() - halfHeight);
    refreshVolume(labelCenter.getX() - halfWidth, labelCenter.getY() + halfHeight);
    refreshVolume(labelCenter.getX() + halfWidth, labelCenter.getY() - halfHeight);
    refreshVolume(labelCenter.getX() + halfWidth, labelCenter.getY() + halfHeight);
  }

  private Cell getCell(NodeDrawProp nodeDrawProp, String cellId) {
    RootCell rootCell = nodeDrawProp.getCell();
    if (rootCell == null) {
      return null;
    }
    return rootCell.getCellById(cellId);
  }

  private GroupKey newGroupKey(Port tailPort, Port headPort, NodeDrawProp nodeDrawProp,
                               DrawGraph drawGraph, String tailCell, String headCell) {
    if (tailPort == null && headPort == null && tailCell == null && headCell == null) {
      GroupKey groupKey = new GroupKey();
      groupKey.tailPoint = new FlatPoint(node.getX(), node.getY());
      groupKey.headPoint = groupKey.tailPoint.clone();
      return groupKey;
    }

    GroupKey groupKey = new GroupKey();
    groupKey.tailPort = tailPort;
    groupKey.headPort = headPort;
    groupKey.tailCell = getCell(nodeDrawProp, tailCell);
    groupKey.headCell = getCell(nodeDrawProp, headCell);

    groupKey.tailPoint = PortHelper.endPoint(tailCell, tailPort, node.getNode(), drawGraph, node);
    if (groupKey.samePoint()) {
      groupKey.headPoint = groupKey.tailPoint;
    } else {
      groupKey.headPoint = PortHelper.endPoint(headCell, headPort, node.getNode(), drawGraph, node);
    }
    return groupKey;
  }

  private int portDirection(Port port, Rankdir rankdir) {
    return portDirection(FlipShifterStrategy.movePort(port, rankdir));
  }

  private int sameCellHorDirection(Port port, Rankdir rankdir) {
    return sameCellHorDirection(FlipShifterStrategy.movePort(port, rankdir));
  }

  private int sameCellVerDirection(Port port, Rankdir rankdir) {
    return sameCellVerDirection(FlipShifterStrategy.movePort(port, rankdir));
  }

  private int sameCellHorDirection(Port tailPort, Port headPort, Rankdir rankdir) {
    tailPort = FlipShifterStrategy.movePort(tailPort, rankdir);
    headPort = FlipShifterStrategy.movePort(headPort, rankdir);

    if (isNW_S(tailPort, headPort) || isNW_S(headPort, tailPort)
        || isN_SW(tailPort, headPort) || isN_SW(headPort, tailPort)
        || isN_W(tailPort, headPort) || isN_W(headPort, tailPort)
        || isNE_W(tailPort, headPort) || isNE_W(headPort, tailPort)
        || isNE_SW(tailPort, headPort) || isNE_SW(headPort, tailPort)
        || isSE_W(tailPort, headPort) || isSE_W(headPort, tailPort)
        || isS_W(tailPort, headPort) || isS_W(headPort, tailPort)
        || isC_NW(tailPort, headPort) || isC_NW(headPort, tailPort)
        || isC_SW(tailPort, headPort) || isC_SW(headPort, tailPort)) {
      return LEFT;
    }

    return RIGHT;
  }

  private boolean isNW_S(Port p1, Port p2) {
    return p1 == Port.NORTH_WEST && p2 == Port.SOUTH;
  }

  private boolean isN_SW(Port p1, Port p2) {
    return p1 == Port.NORTH && p2 == Port.SOUTH_WEST;
  }

  private boolean isN_W(Port p1, Port p2) {
    return p1 == Port.NORTH && p2 == Port.WEST;
  }

  private boolean isNE_W(Port p1, Port p2) {
    return p1 == Port.NORTH_EAST && p2 == Port.WEST;
  }

  private boolean isNE_SW(Port p1, Port p2) {
    return p1 == Port.NORTH_EAST && p2 == Port.SOUTH_WEST;
  }

  private boolean isSE_W(Port p1, Port p2) {
    return p1 == Port.SOUTH_EAST && p2 == Port.WEST;
  }

  private boolean isS_W(Port p1, Port p2) {
    return p1 == Port.SOUTH && p2 == Port.WEST;
  }

  private boolean isC_NW(Port p1, Port p2) {
    return p1 == null && p2 == Port.NORTH_WEST;
  }

  private boolean isC_SW(Port p1, Port p2) {
    return p1 == null && p2 == Port.SOUTH_WEST;
  }

  private int diagonalPointDirection(GroupKey groupKey) {
    if ((groupKey.tailPoint.getX() + groupKey.headPoint.getX()) / 2 < node.getX()) {
      return LEFT;
    }
    return RIGHT;
  }

  private int portDirection(Port port) {
    if (port == Port.NORTH) {
      return UP;
    }

    if (port == Port.SOUTH) {
      return DOWN;
    }

    if (port == Port.NORTH_WEST || port == Port.WEST || port == Port.SOUTH_WEST) {
      return LEFT;
    }

    return RIGHT;
  }

  private int sameCellHorDirection(Port port) {
    if (port == Port.SOUTH_WEST || port == Port.SOUTH || port == Port.SOUTH_EAST) {
      return DOWN;
    }

    return UP;
  }

  private int sameCellVerDirection(Port port) {
    if (port == Port.NORTH_WEST || port == Port.WEST || port == Port.SOUTH_WEST) {
      return LEFT;
    }

    return RIGHT;
  }

  protected static class GroupEntry {

    private final GroupKey groupKey;

    private final DLine line;

    public GroupEntry(GroupKey groupKey, DLine line) {
      Asserts.nullArgument(groupKey, "groupKey");
      Asserts.nullArgument(line, "line");
      this.groupKey = groupKey;
      this.line = line;
    }

    public DLine getLine() {
      return line;
    }
  }

  protected static class GroupKey {

    private Port tailPort;

    private Port headPort;

    private Cell tailCell;

    private Cell headCell;

    private FlatPoint tailPoint;

    private FlatPoint headPoint;

    public Port getTailPort() {
      return tailPort;
    }

    public Port getHeadPort() {
      return headPort;
    }

    public Cell getTailCell() {
      return tailCell;
    }

    public Cell getHeadCell() {
      return headCell;
    }

    public boolean samePoint() {
      return tailCell == headCell && tailPort == headPort;
    }

    public FlatPoint getTailPoint() {
      Asserts.illegalArgument(tailPoint == null, "GroupKey Not Ready");
      return tailPoint.clone();
    }

    public FlatPoint getHeadPoint() {
      Asserts.illegalArgument(headPoint == null, "GroupKey Not Ready");
      return headPoint.clone();
    }

    public boolean isOnlySameHor() {
      if (samePoint() || tailCell != headCell) {
        return false;
      }

      if (Objects.equals(tailPoint, headPoint) || tailPoint == null || headPoint == null) {
        return false;
      }

      return ValueUtils.approximate(tailPoint.getY(), headPoint.getY(), 0.1)
          && !ValueUtils.approximate(tailPoint.getX(), headPoint.getX(), 0.1);
    }

    public boolean isOnlySameVer() {
      if (samePoint() || tailCell != headCell) {
        return false;
      }

      if (Objects.equals(tailPoint, headPoint) || tailPoint == null || headPoint == null) {
        return false;
      }

      return ValueUtils.approximate(tailPoint.getX(), headPoint.getX(), 0.1)
          && !ValueUtils.approximate(tailPoint.getY(), headPoint.getY(), 0.1);
    }

    public boolean sameCell() {
      return tailCell == headCell;
    }

    public boolean notSameCell() {
      return !sameCell();
    }

    public boolean havePortOrCell() {
      return tailPort != null || headPort != null || tailCell != null || headCell != null;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GroupKey groupKey = (GroupKey) o;
      return (tailPort == groupKey.tailPort && headPort == groupKey.headPort
          && Objects.equals(tailCell, groupKey.tailCell)
          && Objects.equals(headCell, groupKey.headCell))
          || (tailPort == groupKey.headPort && headPort == groupKey.tailPort
          && Objects.equals(tailCell, groupKey.headCell)
          && Objects.equals(headCell, groupKey.tailCell));
    }

    @Override
    public int hashCode() {
      return Objects.hash(tailPort, headPort, tailCell, headCell)
          + Objects.hash(headPort, tailPort, headCell, tailCell);
    }
  }
}