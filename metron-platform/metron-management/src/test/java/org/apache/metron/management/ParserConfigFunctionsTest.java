/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.management;

import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.stellar.shell.StellarExecutor;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.metron.TestConstants.A_PARSER_CONFIGS_PATH_FMT;
import static org.apache.metron.management.utils.FileUtils.slurp;
import static org.apache.metron.common.configuration.ConfigurationType.PARSER;
import static org.apache.metron.common.utils.StellarProcessorUtils.run;

public class ParserConfigFunctionsTest {

  String emptyTransformationsConfig = slurp(String.format(A_PARSER_CONFIGS_PATH_FMT,"bro") + "/parsers/bro.json");
  String existingTransformationsConfig = slurp(String.format(A_PARSER_CONFIGS_PATH_FMT,"squid") + "/parsers/squid.json");
  Map<String, StellarExecutor.VariableResult> variables ;
  Context context = null;
  @Before
  public void setup() {
    variables = ImmutableMap.of(
            "upper" , new StellarExecutor.VariableResult("TO_UPPER('foo')", "FOO"),
            "lower" , new StellarExecutor.VariableResult("TO_LOWER('FOO')", "foo")
    );

    context = new Context.Builder()
            .with(StellarExecutor.SHELL_VARIABLES , () -> variables)
            .build();
  }

  public Map<String, Object> transform(String parserConfig){
    return transform(parserConfig, new HashMap<>());
  }

  public Map<String, Object> transform(String parserConfig, Map<String, Object> variables){
    JSONObject ret = new JSONObject(variables);
    SensorParserConfig sensorParserConfig = (SensorParserConfig) PARSER.deserialize(parserConfig);
    sensorParserConfig.init();
    for (FieldTransformer handler : sensorParserConfig.getFieldTransformations()) {
      if (handler != null) {
        handler.transformAndUpdate(ret, sensorParserConfig.getParserConfig(), context);
      }
    }
    return ret;
  }

  @Test
  public void testAddEmpty() {
    String newConfig = (String)run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('upper'))", ImmutableMap.of("config", emptyTransformationsConfig), context);
    Map<String, Object> transformations = transform(newConfig);
    Assert.assertEquals(1, transformations.size());
    Assert.assertEquals("FOO", transformations.get("upper") );
  }

  @Test
  public void testAddHasExisting() {
    String newConfig = (String)run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('upper'))"
            , ImmutableMap.of("config", existingTransformationsConfig )
            , context
    );
    Map<String, Object> transformations = transform(newConfig, ImmutableMap.of("url", "http://www.google.com"));
    //squid already has 2 transformations, we just added url, which makes 3
    Assert.assertEquals(4, transformations.size());
    Assert.assertEquals("FOO", transformations.get("upper") );
  }

  @Test
  public void testAddMalformed() {
    String newConfig = (String)run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('blah'))", ImmutableMap.of("config", emptyTransformationsConfig), context);
    Map<String, Object> transformations = transform(newConfig);
    Assert.assertEquals(0, transformations.size());
  }

  @Test
  public void testAddDuplicate() {
    String newConfig = (String)run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('upper'))", ImmutableMap.of("config", emptyTransformationsConfig), context);
    newConfig = (String)run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('upper'))", ImmutableMap.of("config", newConfig), context);
    Map<String, Object> transformations = transform(newConfig);
    Assert.assertEquals(1, transformations.size());
    Assert.assertEquals("FOO", transformations.get("upper") );
  }

  @Test
  public void testRemove() {
    String newConfig = (String)run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('upper'))", ImmutableMap.of("config", emptyTransformationsConfig), context);
    newConfig = (String)run("PARSER_STELLAR_TRANSFORM_REMOVE(config, ['upper'])", ImmutableMap.of("config", newConfig), context);
    Map<String, Object> transformations = transform(newConfig);
    Assert.assertEquals(0, transformations.size());
  }

  @Test
  public void testRemoveMultiple() {
    String newConfig = (String)run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('upper', 'lower'))", ImmutableMap.of("config", emptyTransformationsConfig), context);
    newConfig = (String)run("PARSER_STELLAR_TRANSFORM_REMOVE(config, ['upper', 'lower'])", ImmutableMap.of("config", newConfig), context);
    Map<String, Object> transformations = transform(newConfig);
    Assert.assertEquals(0, transformations.size());
  }

  @Test
  public void testRemoveMissing() {
    {
      String newConfig = (String) run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('upper'))", ImmutableMap.of("config", emptyTransformationsConfig), context);
      newConfig = (String) run("PARSER_STELLAR_TRANSFORM_REMOVE(config, ['lower'])", ImmutableMap.of("config", newConfig), context);
      Map<String, Object> transformations = transform(newConfig);
      Assert.assertEquals(1, transformations.size());
      Assert.assertEquals("FOO", transformations.get("upper"));
    }
    {
      String newConfig = (String) run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('upper'))", ImmutableMap.of("config", emptyTransformationsConfig), context);
      newConfig = (String) run("PARSER_STELLAR_TRANSFORM_REMOVE(config, [''])", ImmutableMap.of("config", newConfig), context);
      Map<String, Object> transformations = transform(newConfig);
      Assert.assertEquals(1, transformations.size());
      Assert.assertEquals("FOO", transformations.get("upper"));
    }
  }

  /**
╔═══════╤═════════════════╗
║ Field │ Transformation  ║
╠═══════╪═════════════════╣
║ upper │ TO_UPPER('foo') ║
╚═══════╧═════════════════╝
   */
  @Multiline
  static String testPrintExpected;
  @Test
  public void testPrint() {
    String newConfig = (String) run("PARSER_STELLAR_TRANSFORM_ADD(config, SHELL_VARS2MAP('upper'))", ImmutableMap.of("config", emptyTransformationsConfig), context);
    String out = (String) run("PARSER_STELLAR_TRANSFORM_PRINT(config )", ImmutableMap.of("config", newConfig), context);
    Assert.assertEquals(testPrintExpected, out);
  }
  /**
╔═══════╤════════════════╗
║ Field │ Transformation ║
╠═══════╧════════════════╣
║ (empty)                ║
╚════════════════════════╝
   */
  @Multiline
  static String testPrintEmptyExpected;

  @Test
  public void testPrintEmpty() {
    String out = (String) run("PARSER_STELLAR_TRANSFORM_PRINT(config )", ImmutableMap.of("config", emptyTransformationsConfig), context);
    Assert.assertEquals(testPrintEmptyExpected, out);
  }

  @Test
  public void testPrintNull() {

    String out = (String) run("PARSER_STELLAR_TRANSFORM_PRINT(config )", new HashMap<>(), context);
    Assert.assertNull( out);
  }
}
