/*
* Copyright 2017 LinkedIn Corp.
*
* Licensed under the Apache License, Version 2.0 (the “License”); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an “AS IS” BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/

package azkaban.project;

import static org.assertj.core.api.Assertions.assertThat;

import azkaban.flow.Edge;
import azkaban.flow.Flow;
import azkaban.test.executions.ExecutionsTestUtil;
import azkaban.utils.Props;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryYamlFlowLoaderTest {

  private static final Logger logger = LoggerFactory.getLogger(DirectoryYamlFlowLoaderTest
      .class);

  private static final String BASIC_FLOW_YAML_DIR = "basicflowyamltest";
  private static final String MULTIPLE_FLOW_YAML_DIR = "multipleflowyamltest";
  private static final String EMBEDDED_FLOW_YAML_DIR = "embeddedflowyamltest";
  private static final String INVALID_FLOW_YAML_DIR = "invalidflowyamltest";
  private static final String NO_FLOW_YAML_DIR = "noflowyamltest";
  private static final String BASIC_FLOW_1 = "basic_flow";
  private static final String BASIC_FLOW_2 = "basic_flow2";
  private static final String EMBEDDED_FLOW = "embedded_flow";
  private static final String EMBEDDED_FLOW_1 = "embedded_flow1";
  private static final String EMBEDDED_FLOW_2 = "embedded_flow2";
  private static final String INVALID_FLOW_1 = "dependency_not_found";
  private static final String INVALID_FLOW_2 = "cycle_found";
  private static final String DEPENDENCY_NOT_FOUND_ERROR = "Dependency not found.";
  private static final String CYCLE_FOUND_ERROR = "Cycles found.";
  private Project project;

  @Before
  public void setUp() {
    this.project = new Project(12, "myTestProject");
  }

  @Test
  public void testLoadBasicYamlFile() {
    final DirectoryYamlFlowLoader loader = new DirectoryYamlFlowLoader(new Props());
    loader.loadProjectFlow(this.project, ExecutionsTestUtil.getFlowDir(BASIC_FLOW_YAML_DIR));
    checkFlowLoaderProperties(loader, 0, 1, 1);
    checkFlowProperties(loader, BASIC_FLOW_1, 0, 4, 3, null);
  }

  @Test
  public void testLoadMultipleYamlFiles() {
    final DirectoryYamlFlowLoader loader = new DirectoryYamlFlowLoader(new Props());
    loader.loadProjectFlow(this.project, ExecutionsTestUtil.getFlowDir(MULTIPLE_FLOW_YAML_DIR));
    checkFlowLoaderProperties(loader, 0, 2, 2);
    checkFlowProperties(loader, BASIC_FLOW_1, 0, 4, 3, null);
    checkFlowProperties(loader, BASIC_FLOW_2, 0, 3, 2, null);
  }

  @Test
  public void testLoadEmbeddedFlowYamlFile() {
    final DirectoryYamlFlowLoader loader = new DirectoryYamlFlowLoader(new Props());
    loader.loadProjectFlow(this.project, ExecutionsTestUtil.getFlowDir(EMBEDDED_FLOW_YAML_DIR));
    checkFlowLoaderProperties(loader, 0, 3, 3);
    checkFlowProperties(loader, EMBEDDED_FLOW, 0, 4, 3, null);
    checkFlowProperties(loader, EMBEDDED_FLOW_1, 0, 4, 3, null);
    checkFlowProperties(loader, EMBEDDED_FLOW_2, 0, 2, 1, null);
  }

  @Test
  public void testLoadInvalidFlowYamlFiles() {
    final DirectoryYamlFlowLoader loader = new DirectoryYamlFlowLoader(new Props());
    loader.loadProjectFlow(this.project, ExecutionsTestUtil.getFlowDir(INVALID_FLOW_YAML_DIR));
    checkFlowLoaderProperties(loader, 2, 2, 2);
    // Invalid flow 1: Dependency not found.
    checkFlowProperties(loader, INVALID_FLOW_1, 1, 3, 3, DEPENDENCY_NOT_FOUND_ERROR);
    // Invalid flow 2: Cycles found.
    checkFlowProperties(loader, INVALID_FLOW_2, 1, 4, 4, CYCLE_FOUND_ERROR);
  }

  @Test
  public void testLoadNoFlowYamlFile() {
    final DirectoryYamlFlowLoader loader = new DirectoryYamlFlowLoader(new Props());
    loader.loadProjectFlow(this.project, ExecutionsTestUtil.getFlowDir(NO_FLOW_YAML_DIR));
    checkFlowLoaderProperties(loader, 0, 0, 0);
  }

  private void checkFlowLoaderProperties(final DirectoryYamlFlowLoader loader, final int numError,
      final int numFlowMap, final int numEdgeMap) {
    assertThat(loader.getErrors().size()).isEqualTo(numError);
    assertThat(loader.getFlowMap().size()).isEqualTo(numFlowMap);
    assertThat(loader.getEdgeMap().size()).isEqualTo(numEdgeMap);
  }

  private void checkFlowProperties(final DirectoryYamlFlowLoader loader, final String flowName,
      final int numError, final int numNode, final int numEdge, final String edgeError) {
    assertThat(loader.getFlowMap().containsKey(flowName)).isTrue();
    final Flow flow = loader.getFlowMap().get(flowName);
    if (numError != 0) {
      assertThat(flow.getErrors().size()).isEqualTo(numError);
    }
    assertThat(flow.getNodes().size()).isEqualTo(numNode);

    // Verify flow edges
    assertThat(loader.getEdgeMap().get(flowName).size()).isEqualTo(numEdge);
    assertThat(flow.getEdges().size()).isEqualTo(numEdge);
    for (final Edge edge : loader.getEdgeMap().get(flowName)) {
      this.logger.info(flowName + ".flow has edge: " + edge.getId());
      if (edge.getError() != null) {
        assertThat(edge.getError()).isEqualTo(edgeError);
      }
    }
  }
}
