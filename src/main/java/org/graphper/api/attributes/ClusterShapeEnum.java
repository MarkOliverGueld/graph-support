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

package org.graphper.api.attributes;

import java.util.Objects;
import org.graphper.api.ext.Box;
import org.graphper.api.ext.CirclePropCalc;
import org.graphper.api.ext.EllipsePropCalc;
import org.graphper.api.ext.ParallelogramPropCalc;
import org.graphper.api.ext.RectanglePropCalc;
import org.graphper.api.ext.RegularPolylinePropCalc;
import org.graphper.api.ext.ShapePropCalc;
import org.graphper.api.ext.TrapeziumPropCalc;
import org.graphper.def.FlatPoint;

public enum ClusterShapeEnum implements ClusterShape {

  ELLIPSE("ellipse", new EllipsePropCalc()),

  CIRCLE("circle", new CirclePropCalc()),

  RECT("rect", new RectanglePropCalc()),

  TRAPEZIUM("trapezium", new TrapeziumPropCalc()),

  PARALLELOGRAM("parallelogram", new ParallelogramPropCalc()),

  PENTAGON("pentagon", new RegularPolylinePropCalc(5)),

  HEXAGON("hexagon", new RegularPolylinePropCalc(6)),

  SEPTAGON("septagon", new RegularPolylinePropCalc(7)),

  OCTAGON("octagon", new RegularPolylinePropCalc(8));

  private final String name;

  private final ShapePropCalc shapePropCalc;

  ClusterShapeEnum(String name, ShapePropCalc shapePropCalc) {
    Objects.requireNonNull(shapePropCalc);
    this.name = name;
    this.shapePropCalc = shapePropCalc;
  }

  @Override
  public FlatPoint minContainerSize(double innerHeight, double innerWidth) {
    return shapePropCalc.minContainerSize(innerHeight, innerWidth);
  }

  @Override
  public boolean in(Box box, FlatPoint point) {
    return shapePropCalc.in(box, point);
  }

  public String getName() {
    return name;
  }
}
