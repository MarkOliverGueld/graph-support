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

package org.graphper.layout;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.graphper.def.FlatPoint;
import org.graphper.util.Asserts;
import org.graphper.util.ClassUtils;
import org.graphper.util.CollectionUtils;
import org.graphper.util.GraphvizUtils;
import org.graphper.api.Cluster;
import org.graphper.api.ClusterAttrs;
import org.graphper.api.GraphAttrs;
import org.graphper.api.GraphContainer;
import org.graphper.api.Graphviz;
import org.graphper.api.Line;
import org.graphper.api.LineAttrs;
import org.graphper.api.Node;
import org.graphper.api.NodeAttrs;
import org.graphper.api.attributes.Labeljust;
import org.graphper.api.attributes.Labelloc;
import org.graphper.api.attributes.NodeShape;
import org.graphper.api.attributes.NodeShapeEnum;
import org.graphper.draw.ClusterDrawProp;
import org.graphper.draw.ContainerDrawProp;
import org.graphper.draw.DrawGraph;
import org.graphper.draw.GraphvizDrawProp;
import org.graphper.draw.LineDrawProp;
import org.graphper.draw.NodeDrawProp;
import org.graphper.draw.RenderEngine;
import org.graphper.layout.CellLabelCompiler.RootCell;

/**
 * Layout engine common template.
 *
 * @author Jamison Jiang
 */
public abstract class AbstractLayoutEngine implements LayoutEngine {

  /**
   * Node default attribute value map.
   */
  private static final Map<String, Object> DEFAULT_NODE_ATTRS_MAP;

  /**
   * Line default attribute value map.
   */
  private static final Map<String, Object> DEFAULT_LINE_ATTRS_MAP;

  static {
    try {
      DEFAULT_NODE_ATTRS_MAP = ClassUtils.propValMap(DefaultVal.DEFAULT_NODE_ATTRS);
      DEFAULT_LINE_ATTRS_MAP = ClassUtils.propValMap(DefaultVal.DEFAULT_LINE_ATTRS);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Unable to set default properties", e);
    }
  }

  @Override
  public DrawGraph layout(Graphviz graphviz, RenderEngine renderEngine) {
    Asserts.nullArgument(graphviz, "Graphviz");
    Asserts.illegalArgument(graphviz.nodeNum() == 0, "Graphviz container is empty!");

    // Create DrawGraph and initialize some properties of GraphvizDrawProp.
    DrawGraph drawGraph = new DrawGraph(graphviz);
    handleGraphviz(drawGraph.getGraphvizDrawProp());
    Object attachment = attachment(drawGraph);

    // Various id records
    Map<Node, Integer> nodeId = new HashMap<>(graphviz.nodeNum());
    Map<Line, Integer> lineId = new HashMap<>(graphviz.lineNum());
    Map<GraphContainer, Integer> clusterId = new HashMap<>(graphviz.clusters().size());

    /*
     * For each container it recurses into, get all nodes and lines, and initialize node and line
     * attributes, and finally initialize the attributes of the current container.
     */
    Consumer<GraphContainer> containerConsumer = c ->
        nodeLineClusterHandle(attachment, drawGraph, c, nodeId, lineId, clusterId);

    // Traverse all containers in depth.
    GraphvizUtils.dfs(
        Integer.MAX_VALUE,
        Boolean.FALSE,
        new HashSet<>(),
        null,
        graphviz,
        containerConsumer::accept,
        containerConsumer::accept,
        this::dfsNeedContinue
    );
    // Finally execute the root container (Graphviz).
    nodeLineClusterHandle(attachment, drawGraph, graphviz, nodeId, lineId, clusterId);

    // The corresponding layout engine executes.
    layout(drawGraph, attachment);

    // Get all movement strategies in the layout engine and rendering engine, and perform element movement.
    moveGraph(drawGraph, renderEngine, attachment);
    return drawGraph;
  }

  /**
   * Returns the attachment carried by the layout engine.
   *
   * @param drawGraph draw graph object
   * @return attachment of layout
   */
  protected Object attachment(DrawGraph drawGraph) {
    return null;
  }

  /**
   * Post-processing of nodes by the engine.
   *
   * @param node            node
   * @param attachment      layout attachment
   * @param drawGraph       draw graph object
   * @param parentContainer parent container of node
   */
  protected void consumerNode(Node node, Object attachment, DrawGraph drawGraph,
                              GraphContainer parentContainer) {
  }

  /**
   * Post-processing of lines by the engine.
   *
   * @param line       line
   * @param attachment layout attachment
   * @param drawGraph  draw graph object
   */
  protected void consumerLine(Line line, Object attachment, DrawGraph drawGraph) {
  }

  /**
   * Layout engine move post-processing.
   *
   * @param attach layout attachment
   */
  protected void afterLayoutShifter(Object attach) {
  }

  /**
   * Renderer engine move post-processing.
   *
   * @param attach layout attachment
   */
  protected void afterRenderShifter(Object attach) {
  }

  /**
   * Returns the measured label size.
   *
   * @param label    label
   * @param fontName font name
   * @param fontSize font size
   * @return label size
   */
  protected FlatPoint labelContainer(String label, String fontName, double fontSize) {
    return LabelSizeHelper.measure(label, fontName, fontSize, 10);
  }

  /**
   * Set the label positioning of {@link Graphviz} and {@link Cluster}.
   *
   * @param drawGraph draw graph object
   */
  protected void containerLabelPos(DrawGraph drawGraph) {
    GraphvizDrawProp graphvizDrawProp = drawGraph.getGraphvizDrawProp();
    if (graphvizDrawProp.getLabelSize() != null) {
      GraphAttrs graphAttrs = graphvizDrawProp.getGraphviz().graphAttrs();
      containerLabelPos(graphvizDrawProp, graphAttrs.getLabelloc(), graphAttrs.getLabeljust());
    }

    for (ClusterDrawProp cluster : drawGraph.clusters()) {
      if (cluster.getLabelSize() == null) {
        continue;
      }

      ClusterAttrs clusterAttrs = cluster.getCluster().clusterAttrs();
      containerLabelPos(cluster, clusterAttrs.getLabelloc(), clusterAttrs.getLabeljust());
    }
  }

  // -------------------------------- abstract method --------------------------------

  /**
   * Engine layout execute.
   *
   * @param drawGraph  draw graph object
   * @param attachment layout attachment
   */
  protected abstract void layout(DrawGraph drawGraph, Object attachment);

  /**
   * The move strategy for the layout engine.
   *
   * @param drawGraph draw graph object
   * @return move strategy
   */
  protected abstract List<ShifterStrategy> shifterStrategies(DrawGraph drawGraph);

  // -------------------------------- private method --------------------------------

  private void handleGraphviz(GraphvizDrawProp graphvizDrawProp) {
    Graphviz graphviz = graphvizDrawProp.getGraphviz();
    String label = graphviz.graphAttrs().getLabel();
    if (label == null) {
      return;
    }

    // Set label of graphviz
    GraphAttrs graphAttrs = graphviz.graphAttrs();
    FlatPoint labelSize = labelContainer(label, graphAttrs.getFontName(), graphAttrs.getFontSize());
    graphvizDrawProp.setLabelSize(labelSize);
  }

  private void containerLabelPos(ContainerDrawProp containerDrawProp,
                                 Labelloc labelloc, Labeljust labeljust) {
    FlatPoint upperLeft = new FlatPoint(containerDrawProp.getLeftBorder(),
                                        containerDrawProp.getUpBorder());
    FlatPoint lowerRight = new FlatPoint(containerDrawProp.getRightBorder(),
                                         containerDrawProp.getDownBorder());

    // Adjust the position by Labelloc and Labeljust
    FlatPoint labelPoint = new FlatPoint(
        labeljust.getX(upperLeft, lowerRight, containerDrawProp.getLabelSize()),
        labelloc.getY(upperLeft, lowerRight, containerDrawProp.getLabelSize())
    );
    containerDrawProp.setLabelCenter(labelPoint);
  }

  private boolean dfsNeedContinue(GraphContainer c) {
    return !c.isSubgraph() || c.isTransparent();
  }

  private void nodeLineClusterHandle(Object attachment,
                                     DrawGraph drawGraph,
                                     GraphContainer container,
                                     Map<Node, Integer> nodeId,
                                     Map<Line, Integer> lineId,
                                     Map<GraphContainer, Integer> clusterId) {
    Iterable<Node> nodes;
    Iterable<Line> lines;
    /*
     * If dfs is not terminated in the current container, then only get the direct nodes and lines
     * of the current container, if it is terminated, get all the nodes and lines of the current
     * container and all its sub-containers.
     */
    if (dfsNeedContinue(container)) {
      nodes = container.directNodes();
      lines = container.directLines();
    } else {
      nodes = container.nodes();
      lines = container.lines();
    }

    // Handle all nodes
    for (Node node : nodes) {
      nodeHandle(attachment, drawGraph, container, nodeId, node);
    }

    // Handle all lines
    for (Line line : lines) {
      // Handle the tail and head node
      nodeHandle(attachment, drawGraph, container, nodeId, line.head());
      nodeHandle(attachment, drawGraph, container, nodeId, line.tail());

      // Handle line
      lineHandle(attachment, drawGraph, container, lineId, line);
    }

    // Handle all clusters
    if (container.isCluster()) {
      clusterHandle(drawGraph, (Cluster) container, clusterId);

      Cluster cluster = (Cluster) container;
      ClusterAttrs clusterAttrs = cluster.clusterAttrs();
      String label = clusterAttrs.getLabel();
      double fontSize = clusterAttrs.getFontSize();

      // Init cluster label size
      if (label != null) {
        FlatPoint labelContainer = labelContainer(label, clusterAttrs.getFontName(), fontSize);
        drawGraph.getClusterDrawProp(cluster).setLabelSize(labelContainer);
      }
    }
  }

  private void nodeHandle(Object attachment,
                          DrawGraph drawGraph,
                          GraphContainer container,
                          Map<Node, Integer> nodeId,
                          Node node) {
    NodeDrawProp nodeDrawProp = drawGraph.getNodeDrawProp(node);

    NodeAttrs nodeAttrs = nodeDrawProp != null
        ? nodeDrawProp.nodeAttrs()
        : node.nodeAttrs().clone();

    try {
      // Set template properties
      copyTempProperties(
          nodeAttrs,
          findFirstHaveTempParent(drawGraph.getGraphviz(), true, container),
          DEFAULT_NODE_ATTRS_MAP
      );
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Failed to access template property", e);
    }

    if (nodeDrawProp == null) {
      nodeDrawProp = new NodeDrawProp(node, nodeAttrs);
      drawGraph.nodePut(node, nodeDrawProp);
    } else {
      nodeDrawProp.setNodeAttrs(nodeAttrs);
    }

    // Node Id
    Integer n = nodeId.get(node);
    if (n == null) {
      int nz = nodeId.size();
      nodeDrawProp.setId(nz);
      nodeId.put(node, nz);
    }

    // Node container size calculate
    nodeContainerSet(nodeDrawProp, nodeAttrs, drawGraph.needFlip());

    // Node consume
    consumerNode(node, attachment, drawGraph, container);
  }

  private void lineHandle(Object attachment,
                          DrawGraph drawGraph,
                          GraphContainer container,
                          Map<Line, Integer> lineId, Line line) {
    LineDrawProp lineDrawProp = drawGraph.getLineDrawProp(line);

    LineAttrs lineAttrs = lineDrawProp != null
        ? lineDrawProp.lineAttrs()
        : line.lineAttrs().clone();

    try {
      // Set template properties
      copyTempProperties(
          lineAttrs,
          findFirstHaveTempParent(drawGraph.getGraphviz(), false, container),
          DEFAULT_LINE_ATTRS_MAP
      );
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Failed to access template property", e);
    }

    if (lineDrawProp == null) {
      lineDrawProp = new LineDrawProp(line, lineAttrs, drawGraph);
      drawGraph.linePut(line, lineDrawProp);
    }

    // Line id
    Integer n = lineId.get(line);
    if (n == null) {
      int nz = lineId.size();
      lineDrawProp.setId("line_" + nz);
      lineId.put(line, nz);
    }

    // Line consume
    consumerLine(line, attachment, drawGraph);
  }

  private void clusterHandle(DrawGraph drawGraph, Cluster cluster,
                             Map<GraphContainer, Integer> clusterId) {
    if (drawGraph.haveCluster(cluster)) {
      return;
    }

    ClusterDrawProp clusterDrawProp = new ClusterDrawProp(cluster);
    drawGraph.clusterPut(cluster, clusterDrawProp);

    // Cluster id
    Integer n = clusterId.get(cluster);
    if (n == null) {
      int nz = clusterId.size();
      clusterDrawProp.setClusterNo(nz);
      clusterDrawProp.setId("cluster_" + nz);
      clusterId.put(cluster, nz);
    }
  }

  private void nodeContainerSet(NodeDrawProp nodeDrawProp, NodeAttrs nodeAttrs, boolean needFlip) {
    NodeShape nodeShape = nodeAttrs.getNodeShape();

    // Set node box size
    double height = nodeAttrs.getHeight() == null
        ? nodeShape.getDefaultHeight() : nodeAttrs.getHeight();
    double width = nodeAttrs.getWidth() == null
        ? nodeShape.getDefaultWidth() : nodeAttrs.getWidth();

    // Inner Label Box size
    FlatPoint labelBox;
    double verMargin = 0;
    double horMargin = 0;

    if (isRecordShape(nodeShape)) {
      RootCell rootCell = CellLabelCompiler.compile(nodeAttrs.getLabel(), nodeAttrs.getFontName(),
                                                    getFontSize(nodeAttrs), nodeAttrs.getMargin(),
                                                    new FlatPoint(height, width), needFlip);
      labelBox = new FlatPoint(rootCell.getHeight(), rootCell.getWidth());
      nodeDrawProp.setLabelCell(rootCell);
    } else {
      labelBox = sizeInit(nodeAttrs);
      if (nodeAttrs.getMargin() != null && nodeShape.needMargin()) {
        verMargin += nodeAttrs.getMargin().getHeight();
        horMargin += nodeAttrs.getMargin().getWidth();
      }
    }

    FlatPoint boxSize;
    if (Objects.equals(nodeAttrs.getFixedSize(), Boolean.TRUE) || nodeShape.ignoreLabel()) {
      boxSize = new FlatPoint(height, width);
    } else {
      FlatPoint labelSize = new FlatPoint(verMargin + labelBox.getHeight(),
                                          horMargin + labelBox.getWidth());
      if (nodeAttrs.getImageSize() != null) {
        FlatPoint imageSize = nodeAttrs.getImageSize();
        double h = Math.max(imageSize.getHeight() + verMargin, labelSize.getHeight());
        double w = Math.max(imageSize.getWidth() + horMargin, labelSize.getWidth());
        boxSize = nodeShape.minContainerSize(h, w);
      } else {
        boxSize = nodeShape.minContainerSize(labelSize.getHeight(), labelSize.getWidth());
      }
      Asserts.illegalArgument(boxSize == null,
                              "Node Shape can not return null box size from minContainerSize");
      boxSize.setHeight(Math.max(boxSize.getHeight(), height));
      boxSize.setWidth(Math.max(boxSize.getWidth(), width));
      nodeShape.ratio(boxSize);
      nodeDrawProp.setLabelSize(labelSize);
    }

    nodeDrawProp.setLeftBorder(0);
    nodeDrawProp.setRightBorder(boxSize.getWidth());
    nodeDrawProp.setUpBorder(0);
    nodeDrawProp.setDownBorder(boxSize.getHeight());
    if (nodeDrawProp.getLabelSize() == null) {
      nodeDrawProp.setLabelSize(labelBox);
    }

    labelOffset(nodeDrawProp, nodeAttrs, labelBox, verMargin, horMargin);
  }

  private void labelOffset(NodeDrawProp nodeDrawProp, NodeAttrs nodeAttrs, FlatPoint labelBox,
                         double verMargin, double horMargin) {
    if (nodeAttrs.getLabelloc() == null || nodeAttrs.getLabelloc() == Labelloc.CENTER) {
      return;
    }

    double halfHeight = (verMargin + labelBox.getHeight()) / 2;
    double halfWidth = (horMargin + labelBox.getWidth()) / 2;
    Labelloc labelloc = nodeAttrs.getLabelloc();
    double offsetY = labelloc.getY(
        new FlatPoint(-halfWidth, -halfHeight),
        new FlatPoint(halfWidth, halfHeight),
        labelBox
    );
    nodeDrawProp.setLabelVerOffset(offsetY);
  }

  private GraphContainer findFirstHaveTempParent(Graphviz graphviz, boolean nodeTemp,
                                                 GraphContainer container) {
    GraphContainer p = container;

    while (p != null) {
      if ((nodeTemp && p.haveNodeTemp()) || (!nodeTemp && p.haveLineTemp())) {
        break;
      }

      p = graphviz.father(p);
    }

    return p;
  }

  private FlatPoint sizeInit(NodeAttrs nodeAttrs) {
    String label = nodeAttrs.getLabel();

    return labelContainer(label, nodeAttrs.getFontName(), getFontSize(nodeAttrs));
  }

  private double getFontSize(NodeAttrs nodeAttrs) {
    return nodeAttrs.getFontSize() != null ? nodeAttrs.getFontSize() : 0D;
  }

  @SuppressWarnings("all")
  private void copyTempProperties(Object attrs, GraphContainer container,
                                  Map<String, Object> defaultVal) throws IllegalAccessException {
    Objects.requireNonNull(defaultVal);
    if (attrs == null) {
      return;
    }

    NodeShape nodeShape = null;
    Field nodeShapeField = null;
    Class<?> cls = attrs.getClass();
    Field[] fields = cls.getDeclaredFields();
    for (Field field : fields) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      field.setAccessible(true);
      Object v = field.get(attrs);
      if (v == null) {
        Object propVal;
        if (container == null) {
          propVal = null;
        } else if (attrs instanceof NodeAttrs) {
          propVal = container.getNodeAttr(field.getName());
        } else {
          propVal = container.getLineAttr(field.getName());
        }
        propVal = propVal != null ? propVal : defaultVal.get(field.getName());

        if (propVal == null) {
          field.setAccessible(false);
          continue;
        }

        field.set(attrs, propVal);
        field.setAccessible(false);
        v = propVal;
      }

      if (v instanceof NodeShape) {
        nodeShape = (NodeShape) v;
        nodeShapeField = field;
      }
    }

    // Compile a new NodeShape description function
    if (nodeShape != null && attrs instanceof NodeAttrs) {
      nodeShapeField.setAccessible(true);
      // Node post shape
      nodeShapeField.set(attrs, nodeShape.post((NodeAttrs) attrs));
      nodeShapeField.setAccessible(false);
    }
  }

  private void moveGraph(DrawGraph drawGraph, RenderEngine renderEngine, Object attach) {
    List<ShifterStrategy> layoutShifters = shifterStrategies(drawGraph);

    Shifter shifter;
    Set<FlatPoint> pointMark = null;
    if (CollectionUtils.isNotEmpty(layoutShifters)) {
      pointMark = new HashSet<>();
      shifter = new CombineShifter(pointMark, layoutShifters);
      executeShifter(drawGraph, shifter);
    }
    afterLayoutShifter(attach);

    if (pointMark != null) {
      pointMark.clear();
    }

    List<ShifterStrategy> renderShifters = renderEngine == null
        ? null : renderEngine.shifterStrategies(drawGraph);

    if (CollectionUtils.isNotEmpty(renderShifters)) {
      if (pointMark == null) {
        pointMark = new HashSet<>();
      }
      shifter = new CombineShifter(pointMark, renderShifters);
      executeShifter(drawGraph, shifter);
    }
    afterRenderShifter(attach);
  }

  private void executeShifter(DrawGraph drawGraph, Shifter shifter) {
    shifter.graph(drawGraph.getGraphvizDrawProp());
    drawGraph.clusters().forEach(shifter::cluster);
    drawGraph.nodes().forEach(shifter::node);
    drawGraph.lines().forEach(shifter::line);
  }

  private boolean isRecordShape(NodeShape nodeShape) {
    return nodeShape == NodeShapeEnum.RECORD || nodeShape == NodeShapeEnum.M_RECORD;
  }
}
