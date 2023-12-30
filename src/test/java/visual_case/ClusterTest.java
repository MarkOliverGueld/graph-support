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
import org.graphper.api.Cluster;
import org.graphper.api.Graphviz;
import org.graphper.api.Line;
import org.graphper.api.Node;
import org.graphper.api.attributes.Labeljust;
import org.graphper.api.attributes.Labelloc;
import org.graphper.api.attributes.NodeShapeEnum;
import org.graphper.api.attributes.Rank;
import org.graphper.api.attributes.Rankdir;
import org.junit.jupiter.api.Test;

public class ClusterTest extends GraphvizVisual {

  @Test
  public void case1() {
    Node n1 = Node.builder().label("1").build();
    Node n2 = Node.builder().label("2").build();
    Node n3 = Node.builder().label("33333333333333333333333333333333333333333\n"
                                       + "333333333333333333333333333333\n3333333333333333333333333").build();
    Node n4 = Node.builder().label("4").build();
    Node n5 = Node.builder().label("5").build();
    Node n6 = Node.builder().label("6").build();
    Node n7 = Node.builder().label("7").build();
    Node n8 = Node.builder().label("8").build();
    Node n9 = Node.builder().label("9").build();
    Node n10 = Node.builder().label("10").build();
    Node n11 = Node.builder().label("11").build();
    Node n12 = Node.builder().label("12").build();
    Node n13 = Node.builder().label("13").build();

    Graphviz graphviz = Graphviz.digraph()
        .scale(0.6)
        .tempNode(Node.builder().shape(NodeShapeEnum.TRIANGLE).build())
        .rankdir(Rankdir.TB)
        .compound(true)
        .addLine(n1, n4)
        .addLine(Line.builder(n1, n2).label("1 --> 2").build())
        .label("GRAPHVIZ DEMO")
        .addLine(n1, n2)
        .addLine(n1, n3)
        .addLine(n4, n6)
        .addLine(n2, n5)
        .addLine(n2, n8)
        .addLine(n3, n6)
        .addLine(n3, n8)
        .addLine(Line.builder(n5, n7).ltail("cluster_2").lhead("cluster_3").build())
        .addLine(n5, n7)
        .addLine(Line.builder(n6, n7).build())
        .addLine(n8, n7)
        .addLine(n2, n9)
        .addLine(n2, n10)
        .addLine(Line.builder(n12, n6).build())
        .addLine(n12, n11)
        .addLine(n7, n13)
        .addLine(n3, n9)
        .cluster(
            Cluster.builder()
                .id("cluster_1")
                .labeljust(Labeljust.LEFT)
                .labelloc(Labelloc.BOTTOM)
                .label("cluster_1 Nice to meet you!\nHow old are you?\nI'm fine thank you, and you?\nMe too!\nWhere are you from?")
                .fontSize(24)
                .addNode(n2)
                .addNode(n3)
                .addNode(n4)
                .addNode(n5)
                .addNode(n6)
                .addLine(n6, n11)
                .cluster(
                    Cluster.builder()
                        .id("cluster_2")
                        .labelloc(Labelloc.BOTTOM)
                        .label("cluster_2 1111111111111111111111111\n22222222222222222\n3333333333333\n4444444444444444\n555555\n66666\n77777\n888888\n9999999")
                        .fontSize(36)
                        .addNode(n3)
                        .addNode(n5)
                        .addNode(n6)

                        .startSub()
                        .rank(Rank.SAME)
                        .addLine(Line.builder(n3, n5).label("3 --> 5").build())
                        .endSub()

                        .build()
                )
                .build()
        )
        .cluster(
            Cluster.builder()
                .id("cluster_3")
                .labeljust(Labeljust.RIGHT)
                .label("cluster_3 Hello World|Hello World|Hello World\nHello World\nHello World\nHello World\nHello World\nHello World\nHello World")
                .addNode(n7)
                .addNode(n8)
                .addNode(n9)
                .build()
        )
        .build();

    visual(graphviz);
  }
}
