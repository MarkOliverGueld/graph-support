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

import org.graphper.api.Node;
import org.graphper.api.NodeAttrs;
import org.graphper.api.attributes.Port;
import org.graphper.def.FlatPoint;
import org.graphper.def.Vectors;
import org.graphper.draw.NodeDrawProp;
import org.graphper.layout.Cell;
import org.graphper.layout.dot.DotLayoutEngine.PortPoll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PortPollTest {

  @Test
  public void testIterator() {
    Cell cell = new Cell(true);
    NodeDrawProp node = new NodeDrawProp(Node.builder().build(), new NodeAttrs());
    PortPoll portPoll = PortPoll.newPort(cell, node);
    assertSize(portPoll, 0);

    cell.setHeight(2);
    cell.setWidth(2);
    cell.setOffset(Vectors.ZERO);
    node.setLeftBorder(-1);
    node.setRightBorder(1);
    node.setUpBorder(-1);
    node.setDownBorder(1);
    portPoll = PortPoll.newPort(cell, node);
    assertSize(portPoll, 4);

    Assertions.assertEquals(Port.WEST, portPoll.nextPort());
    assertPorts(portPoll, Port.EAST, Port.NORTH, Port.SOUTH, Port.WEST);
    assertPorts(portPoll, Port.EAST, Port.NORTH, Port.SOUTH, Port.WEST);
    Assertions.assertEquals(Port.EAST, portPoll.nextPort());
    Assertions.assertEquals(Port.NORTH, portPoll.nextPort());
    assertPorts(portPoll, Port.SOUTH, Port.WEST, Port.EAST, Port.NORTH);

    cell.setWidth(0.5);
    cell.setHeight(0.5);
    cell.setOffset(new FlatPoint(0, 0.5));
    portPoll = PortPoll.newPort(cell, node);
    assertSize(portPoll, 1);
    Assertions.assertEquals(Port.WEST, portPoll.nextPort());
    Assertions.assertEquals(Port.WEST, portPoll.nextPort());
    Assertions.assertEquals(Port.WEST, portPoll.nextPort());
    Assertions.assertEquals(Port.WEST, portPoll.nextPort());
    assertPorts(portPoll, Port.WEST);
  }

  private void assertPorts(PortPoll portPoll, Port... expectPorts) {
    assertSize(portPoll, expectPorts.length);

    int i = 0;
    for (Port port : portPoll) {
      Assertions.assertEquals(port, expectPorts[i++]);
    }
  }

  private void assertSize(PortPoll portPoll, int expectSize) {
    Assertions.assertEquals(expectSize, portPoll.size());
    Assertions.assertEquals(expectSize, sizeByIterator(portPoll));
  }

  private int sizeByIterator(PortPoll portPoll) {
    int size = 0;
    for (Port port : portPoll) {
      size++;
    }
    return size;
  }
}
