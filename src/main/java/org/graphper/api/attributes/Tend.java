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

import org.graphper.api.FloatLabel;

/**
 * The placement tendency of {@link FloatLabel}.
 *
 * @author Jamison Jiang
 */
public enum Tend {
  /**
   * Float Label as close as possible to the tail node.
   */
  TAIL,

  /**
   * Float Label as close as possible to the head node.
   */
  HEAD;
}
