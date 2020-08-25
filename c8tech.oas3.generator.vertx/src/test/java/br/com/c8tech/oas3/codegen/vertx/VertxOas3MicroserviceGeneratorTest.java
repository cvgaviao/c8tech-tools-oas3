/*
 * ============================================================================
 *  Copyright ©  2020,    Cristiano V. Gavião
 *
 *  All rights reserved.
 *  This program and the accompanying materials are made available under
 *  the terms of the Eclipse Public License v1.0 which accompanies this
 *  distribution and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * ============================================================================
 */
package br.com.c8tech.oas3.codegen.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;

/***
 * This test allows you to easily launch your code generation software under a
 * debugger. Then run this test under debug mode. You will be able to step
 * through your java code and then see the results in the out directory.
 *
 * To experiment with debugging your code generator: 1) Set a break point in
 * MyCodegenGenerator.java in the postProcessOperationsWithModels() method. 2)
 * To launch this test in Eclipse: right-click | Debug As | JUnit Test
 *
 */
class VertxOas3MicroserviceGeneratorTest {

  // use this test to launch you code generator in the debugger.
  // this allows you to easily set break points in MyclientcodegenGenerator.
  @Test
  void testVertxMicroserviceCodeGenerator() {
    // to understand how the 'openapi-generator-cli' module is using 'CodegenConfigurator', have a look at the 'Generate' class:
    // https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator-cli/src/main/java/org/openapitools/codegen/cmd/Generate.java 
    final CodegenConfigurator configurator = new CodegenConfigurator()
      .setGeneratorName(VertxOas3MicroserviceProjectGenerator.GENERATOR_NAME) // use this codegen library
      .setModelPackage("io.dataobjetcs")
      .setInputSpec("src/test/resources/3.x/petstore.yaml") // sample OpenAPI file
      .setOutputDir("target/gen/" + VertxOas3MicroserviceProjectGenerator.GENERATOR_NAME);
    configurator.addTypeMapping("ParentModel",
                                "AbstractModel");
    configurator.addImportMapping("AbstractModel",
                                  "br.com.c8tech.oas3.codegen.vertx.AbstractModel");
    final ClientOptInput clientOptInput = configurator.toClientOptInput();
    DefaultGenerator generator = new DefaultGenerator();
    generator.opts(clientOptInput).generate();

    assertThat(configurator).isNotNull();
  }

}
