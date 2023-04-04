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

/**
 * Line style.
 *
 * @author Jamison Jiang
 */
public interface LineStyle extends StyleItem {

  /**
   * Dashed line consisting of a series of line segment.
   */
  LineStyle DASHED = NameStyle.DASHED;

  /**
   * Dotted line consisting of a series of points.
   */
  LineStyle DOTTED = NameStyle.DOTTED;

  /**
   * Line segments are drawn as solid lines.
   */
  LineStyle SOLID = NameStyle.SOLID;

  /**
   * Hide line segments when rendering (but the attributes are valid during layout).
   */
  LineStyle INVIS = NameStyle.INVIS;

  /**
   * Draws the line segment boldly.
   */
  LineStyle BOLD = NameStyle.BOLD;
}
