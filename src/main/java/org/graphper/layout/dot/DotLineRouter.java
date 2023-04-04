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

import org.graphper.def.EdgeDedigraph;
import org.graphper.api.attributes.Splines;
import org.graphper.draw.DrawGraph;

/**
 * Dot line routing interface.
 *
 * @author Jamison Jiang
 */
public interface DotLineRouter {

  /**
   * Determine whether the current router needs to process the current {@link Splines } attribute.
   *
   * @param splines Splines
   * @return <tt>true</tt> if router need handle the splines attribute
   */
  default boolean needDeal(Splines splines) {
    return splines != Splines.NONE;
  }

  /**
   * Routing the line.
   */
  void route();

  // --------------------------------- Router Factory ---------------------------------

	/**
	 * Factory of the router.
	 *
	 * @param <T> router type
	 */
  interface DotLineRouterFactory<T extends DotLineRouter> {

    T newInstance(DrawGraph drawGraph, DotDigraph dotDigraph, RankContent rankContent,
                  EdgeDedigraph<DNode, DLine> digraphProxy);
  }
}
