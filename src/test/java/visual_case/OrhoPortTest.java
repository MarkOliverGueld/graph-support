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

package visual_case;

import helper.GraphvizVisual;
import org.graphper.api.Graphviz;
import org.graphper.api.Line;
import org.graphper.api.Node;
import org.graphper.api.attributes.Port;
import org.graphper.api.attributes.Splines;
import org.junit.jupiter.api.Test;

public class OrhoPortTest extends GraphvizVisual {

  @Test
  public void portCase1() {
    Node a = Node.builder().label("a").build();
    Node b = Node.builder().label("b").build();
    Node c = Node.builder().label("c").build();
    Node d = Node.builder().label("d").build();
    Node e = Node.builder().label("e").build();
    Node f = Node.builder().label("f").build();
    Node g = Node.builder().label("g").build();

    Graphviz graphviz = Graphviz.digraph()
        .splines(Splines.ORTHO)
        .showGrid(true)
        .addLine(Line.builder(a, b)
                     .tailPort(Port.EAST)
                     .headPort(Port.SOUTH)
                     .label("line 1").build())
        .addLine(Line.builder(a, b)
                     .tailPort(Port.EAST)
                     .headPort(Port.SOUTH)
                     .label("line 1").build())
        .addLine(Line.builder(a, c)
                     .tailPort(Port.EAST).headPort(Port.EAST)
                     .label("line 2").build())
        .addLine(Line.builder(a, d)
                     .tailPort(Port.NORTH).headPort(Port.EAST)
                     .label("line 3").build())
        .addLine(Line.builder(a, e)
                     .tailPort(Port.SOUTH).headPort(Port.EAST)
                     .label("line 4").build())
        .build();

    visual(graphviz);
  }

  @Test
  public void portCase2() {
    Node a = Node.builder().label("a").build();
    Node b = Node.builder().label("b").build();
    Node c = Node.builder().label("c").build();
    Node d = Node.builder().label("d").build();

    Graphviz graphviz = Graphviz.digraph()
        .splines(Splines.ORTHO)
        .showGrid(true)
        .addLine(Line.builder(a, b)
                     .tailPort(Port.WEST).headPort(Port.WEST)
                     .build())
        .addLine(Line.builder(a, b)
                     .tailPort(Port.WEST).headPort(Port.WEST)
                     .build())
        .addLine(Line.builder(a, b)
                     .tailPort(Port.WEST).headPort(Port.WEST)
                     .build())
        .addLine(Line.builder(a, b)
                     .tailPort(Port.WEST).headPort(Port.WEST)
                     .build())
        .addLine(Line.builder(a, b)
                     .tailPort(Port.WEST).headPort(Port.WEST)
                     .build())
        .addLine(Line.builder(a, b)
                     .tailPort(Port.EAST).headPort(Port.EAST)
                     .build())
        .addLine(Line.builder(a, b)
                     .tailPort(Port.EAST).headPort(Port.EAST)
                     .build())
        .addLine(Line.builder(a, b)
                     .tailPort(Port.EAST).headPort(Port.EAST)
                     .build())
        .addLine(Line.builder(a, b)
                     .tailPort(Port.EAST).headPort(Port.EAST)
                     .build())
        .addLine(Line.builder(c, d)
                     .tailPort(Port.EAST).headPort(Port.SOUTH)
                     .build())
        .addLine(Line.builder(c, d)
                     .tailPort(Port.NORTH).headPort(Port.WEST)
                     .build())
        .addLine(Line.builder(c, d)
                     .tailPort(Port.NORTH).headPort(Port.WEST)
//                     .label("666")
                     .build())
        .build();

    visual(graphviz);
  }
}
