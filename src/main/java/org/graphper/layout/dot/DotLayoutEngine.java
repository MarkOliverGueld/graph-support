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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache_gs.commons.lang3.StringUtils;
import org.graphper.api.Assemble;
import org.graphper.api.GraphAttrs;
import org.graphper.api.GraphContainer;
import org.graphper.api.Graphviz;
import org.graphper.api.Line;
import org.graphper.api.LineAttrs;
import org.graphper.api.Node;
import org.graphper.api.attributes.Rankdir;
import org.graphper.api.attributes.Splines;
import org.graphper.def.EdgeDedigraph;
import org.graphper.def.FlatPoint;
import org.graphper.draw.ClusterDrawProp;
import org.graphper.draw.DrawGraph;
import org.graphper.draw.GraphvizDrawProp;
import org.graphper.draw.LineDrawProp;
import org.graphper.draw.NodeDrawProp;
import org.graphper.layout.AbstractLayoutEngine;
import org.graphper.layout.FlipShifterStrategy;
import org.graphper.layout.LayoutAttach;
import org.graphper.layout.ShifterStrategy;
import org.graphper.layout.dot.DotLineRouter.DotLineRouterFactory;
import org.graphper.layout.dot.LineHandler.LineRouterBuilder;
import org.graphper.layout.dot.OrthogonalRouter.OrthogonalRouterFactory;
import org.graphper.layout.dot.PolyLineRouter.PolyLineRouterFactory;
import org.graphper.layout.dot.RoundedRouter.RoundedRouterFactory;
import org.graphper.layout.dot.SplineRouter.SplineRouterFactory;
import org.graphper.util.Asserts;
import org.graphper.util.CollectionUtils;

/**
 * Hierarchical or layered drawings of directed graphs. The layout algorithm aims edges in the same
 * direction (top to bottom, or left to right) and then attempts to avoid edge crossings and reduce
 * edge length.
 *
 * <p>Dot layout mainly has the following aesthetic principles:
 * <ul>
 *   <li>A1: Expose hierarchical structure in the graph. In particular, aim edges in the same general
 *   direction if possible. This aids finding directed paths and highlights source and sink nodes;
 *   <li>A2: Avoid visual anomalies that do not convey information about the underlying graph.
 *   For example, avoid edge crossings and sharp bends;
 *   <li>A3: Keep edges short. This makes it easier to find related nodes and contributes to A2;
 *   <li>A4: Favor symmetry and balance.
 * </ul>
 *
 * <p>The overall process of Dot layout is as follows:
 * <pre>{@code
 * 1. procedure draw_graph()
 * 2. begin
 * // An efficient way of ranking the nodes using a network simplex algorithm
 * 3. rank();
 * // Improved heuristics to reduce edge crossings;
 * 4. ordering();
 * // A method for computing the node coordinates as a rank assignment problem
 * 5. position();
 * // Method for setting spline control points
 * 6. make_splines();
 * 7. end
 * }</pre>
 *
 * @author Jamison Jiang
 */
public class DotLayoutEngine extends AbstractLayoutEngine implements Serializable {

  private static final long serialVersionUID = 1932138711284862609L;

  /**
   * Spline router factory
   */
  private static final List<DotLineRouterFactory<?>> SPLINES_HANDLERS;

  static {
    SPLINES_HANDLERS = Arrays.asList(new RoundedRouterFactory(), new SplineRouterFactory(),
                                     new PolyLineRouterFactory(), new LineRouterBuilder(),
                                     new OrthogonalRouterFactory());
  }

  @Override
  public List<ShifterStrategy> shifterStrategies(DrawGraph drawGraph) {
    if (drawGraph.getGraphviz().graphAttrs().getRankdir() == Rankdir.TB) {
      return Collections.emptyList();
    }
    return Collections.singletonList(new FlipShifterStrategy(drawGraph));
  }

  @Override
  protected LayoutAttach attachment(DrawGraph drawGraph) {
    Map<Node, DNode> nodeRecord = new HashMap<>(drawGraph.getGraphviz().nodeNum());
    DotDigraph dotDigraph = new DotDigraph(drawGraph.getGraphviz().nodeNum(),
                                           drawGraph.getGraphviz(), nodeRecord);

    return new DotAttachment(dotDigraph, drawGraph, nodeRecord);
  }

  @Override
  protected void consumerNode(Node node, LayoutAttach attachment, DrawGraph drawGraph,
                              GraphContainer parentContainer) {
    DotAttachment dotAttachment = (DotAttachment) attachment;

    DNode dn = dotAttachment.get(node);
    boolean dnIsNull = dn == null;
    if (dnIsNull) {
      if (drawGraph.needFlip()) {
        NodeDrawProp nodeDrawProp = drawGraph.getNodeDrawProp(node);
        nodeDrawProp.flip();
      }

      dn = dotAttachment.mappingToDNode(node);
    }

    // Set node parent container
    if (dn.getContainer() == null || dn.getContainer().isGraphviz()) {
      if (parentContainer.isSubgraph()) {
        if (!parentContainer.isTransparent()) {
          dotAttachment.markHaveSubgraph();
        }
        parentContainer = drawGraph.getGraphviz().effectiveFather(parentContainer);
      }
      dn.setContainer(parentContainer);
    }

    dn.setNodeAttrs(drawGraph.getNodeDrawProp(node).nodeAttrs());
    dotAttachment.put(node, dn);
    dotAttachment.addNode(dn);

    if (parentContainer.isCluster()) {
      dotAttachment.markHaveCluster();
    }
  }

  @Override
  protected void consumerLine(Line line, LayoutAttach attachment, DrawGraph drawGraph) {
    DotAttachment dotAttachment = (DotAttachment) attachment;
    // must not be null
    DNode source = dotAttachment.get(line.tail());
    DNode target = dotAttachment.get(line.head());

    FlatPoint labelSize = null;
    LineDrawProp lineDrawProp = drawGraph.getLineDrawProp(line);
    LineAttrs lineAttrs = lineDrawProp.lineAttrs();

    Assemble assemble = lineDrawProp.getAssemble();
    if (assemble != null) {
      labelSize = assemble.size();
    } else if (needLabelNode(drawGraph, line)) {
      labelSize = lineLabelSizeInit(lineAttrs);
    }

    if (labelSize != null && drawGraph.needFlip()) {
      labelSize.flip();
    }

    DLine dLine = new DLine(source, target, line, lineAttrs,
                            lineAttrs.getWeight() == null ? line.weight() : lineAttrs.getWeight(),
                            lineAttrs.getMinlen() != null ? lineAttrs.getMinlen() : 1, labelSize);

    dotAttachment.addEdge(dLine);
  }

  @Override
  protected void afterLayoutShifter(LayoutAttach attach) {
    DotAttachment dotAttachment = (DotAttachment) attach;
    DrawGraph drawGraph = dotAttachment.getDrawGraph();

    for (NodeDrawProp nodeDrawProp : drawGraph.nodes(true)) {
      nodeLabelSet(nodeDrawProp, drawGraph, true);
    }

    for (LineDrawProp line : drawGraph.lines()) {
      Assemble assemble = line.getAssemble();
      setCellNodeOffset(drawGraph, line.getLabelCenter(), assemble, true);
    }
    drawGraph.syncToGraphvizBorder();
  }

  @Override
  protected void afterRenderShifter(LayoutAttach attach) {
    DotAttachment dotAttachment = (DotAttachment) attach;
    DrawGraph drawGraph = dotAttachment.getDrawGraph();
    if (drawGraph.needFlip()) {
      containerLabelPos(drawGraph);
    }

    for (ClusterDrawProp cluster : drawGraph.clusters()) {
      Assemble assemble = cluster.getAssemble();
      setCellNodeOffset(drawGraph, cluster.getLabelCenter(), assemble, true);
    }

    GraphvizDrawProp graphvizDrawProp = drawGraph.getGraphvizDrawProp();
    Assemble assemble = graphvizDrawProp.getAssemble();
    if (assemble != null) {
      setCellNodeOffset(drawGraph, graphvizDrawProp.getLabelCenter(), assemble, true);
    }
    // Line clip
    dotAttachment.clipAllLines();
  }

  @Override
  protected void layout(DrawGraph drawGraph, LayoutAttach attach) {
    Asserts.nullArgument(drawGraph, "DrawGraph");

    DotAttachment dotAttachment = (DotAttachment) attach;
    DotDigraph dotDigraph = dotAttachment.getDotDigraph();
    Graphviz graphviz = drawGraph.getGraphviz();
    GraphAttrs graphAttrs = graphviz.graphAttrs();
    dotAttachment.initLineClip();

    // Collapse subgraphs and clusters, then assign the rank for per node
    ContainerCollapse containerCollapse = new ContainerCollapse(dotAttachment, graphviz);
    RankContent rankContent = containerCollapse.getRankContent();

    if (dotAttachment.haveClusters() || dotAttachment.haveSubgraphs()) {
      /*
       * 1.Find self loop line, remove it.
       * 2. If there is an edge where the rank of from is greater
       * than the rank of to, it needs to be flipped.
       * */
      handleLegalLine(dotDigraph);
      // Primitive graph RankContent
      rankContent = new RankContent(dotDigraph, graphAttrs.getRankSep(), true, null);
    }

    // Best node sorting between ranks.
    MinCross minCross = new MinCross(rankContent, dotAttachment);
    EdgeDedigraph<DNode, DLine> digraphProxy = minCross.getDigraphProxy();

    // Handle various line label.
    new LabelSupplement(rankContent, dotAttachment, digraphProxy);

    // Node coordinate
    if (Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty("dot.coordinate.v1"))) {
      new Coordinate(graphAttrs.getNslimit(), rankContent, dotAttachment, digraphProxy);
    } else {
      new CoordinateV2(graphAttrs.getNslimit(), rankContent, dotAttachment, digraphProxy);
    }

    if (!drawGraph.needFlip()) {
      containerLabelPos(drawGraph);
    }
    splines(drawGraph, dotDigraph, rankContent, digraphProxy);
  }

  // --------------------------------------------- private method ---------------------------------------------

  private void handleLegalLine(DotDigraph dotDigraph) {
    List<DLine> reverseLines = null;
    List<DLine> selfLoopLines = null;
    for (DNode node : dotDigraph) {
      for (DLine line : dotDigraph.adjacent(node)) {
        if (line.from().getRank() <= line.to().getRank()) {
          if (line.from() == line.to()) {
            if (selfLoopLines == null) {
              selfLoopLines = new ArrayList<>(2);
            }
            selfLoopLines.add(line);
          }

          continue;
        }

        if (reverseLines == null) {
          reverseLines = new ArrayList<>();
        }
        reverseLines.add(line);
      }
    }

    if (CollectionUtils.isNotEmpty(reverseLines)) {
      for (DLine reverseLine : reverseLines) {
        dotDigraph.reverseEdge(reverseLine);
      }
    }

    if (CollectionUtils.isNotEmpty(selfLoopLines)) {
      for (DLine selfLoopLine : selfLoopLines) {
        if (dotDigraph.removeEdge(selfLoopLine)) {
          selfLoopLine.from().addSelfLine(selfLoopLine);
        }
      }
    }
  }

  private void splines(DrawGraph drawGraph, DotDigraph dotDigraph, RankContent rankContent,
                       EdgeDedigraph<DNode, DLine> digraphProxy) {
    Splines splines = drawGraph.getGraphviz().graphAttrs().getSplines();
    Map<Line, LineDrawProp> lineDrawPropMap = drawGraph.getLineDrawPropMap();

    if (splines == null || splines == Splines.NONE || lineDrawPropMap == null
        || digraphProxy.vertexNum() == 0) {
      return;
    }

    // spline handler hand out
    for (DotLineRouterFactory<?> linesHandlerFactory : SPLINES_HANDLERS) {
      DotLineRouter dotLineRouter = linesHandlerFactory.newInstance(drawGraph, dotDigraph,
                                                                    rankContent, digraphProxy);

      if (dotLineRouter.needDeal(splines)) {
        dotLineRouter.route();
        break;
      }
    }
  }

  private boolean needLabelNode(DrawGraph drawGraph, Line line) {
    Map<Line, LineDrawProp> lineDrawPropMap = drawGraph.getLineDrawPropMap();
    // ignore Spline.NONE and self loop
    if (lineDrawPropMap == null) {
      return false;
    }

    return StringUtils.isNotEmpty(drawGraph.lineAttrs(line).getLabel());
  }

  private FlatPoint lineLabelSizeInit(LineAttrs lineAttrs) {
    String label = lineAttrs.getLabel();

    if (StringUtils.isEmpty(label)) {
      return null;
    }

    double fontSize = lineAttrs.getFontSize() != null ? lineAttrs.getFontSize() : 0D;
    return labelContainer(label, lineAttrs.getFontName(), fontSize);
  }

  private static class LinePortGroup {

    private TreeSet<LineDrawProp> sameRankLines;

    private TreeSet<LineDrawProp> diffRankLines;

    private void addLine(LineDrawProp line, DotDigraph digraph) {
      DNode tail = digraph.getDNode(line.getLine().tail());
      DNode head = digraph.getDNode(line.getLine().head());
      if (tail.getRank() == head.getRank()) {
        if (sameRankLines == null) {
          sameRankLines = new TreeSet<>((l, r) -> compare(l, r, digraph));
        }
        sameRankLines.add(line);
      } else {
        if (diffRankLines == null) {
          diffRankLines = new TreeSet<>((l, r) -> compare(l, r, digraph));
        }
        diffRankLines.add(line);
      }
    }

    private int compare(LineDrawProp l, LineDrawProp r, DotDigraph digraph) {
      DNode lh = digraph.getDNode(l.getLine().head());
      DNode rh = digraph.getDNode(r.getLine().head());
      return Integer.compare(lh.getRankIndex(), rh.getRankIndex());
    }
  }
}
