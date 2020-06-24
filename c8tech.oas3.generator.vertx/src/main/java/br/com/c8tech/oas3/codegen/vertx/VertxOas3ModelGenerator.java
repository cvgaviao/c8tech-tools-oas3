/**
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

import java.io.File;
import java.net.URL;
import java.util.EnumSet;

import org.apache.commons.lang3.BooleanUtils;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.AbstractJavaCodegen;
import org.openapitools.codegen.meta.features.ClientModificationFeature;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.meta.features.SchemaSupportFeature;
import org.openapitools.codegen.meta.features.SecurityFeature;
import org.openapitools.codegen.meta.features.WireFormatFeature;
import org.openapitools.codegen.templating.mustache.SplitStringLambda;
import org.openapitools.codegen.templating.mustache.TrimWhitespaceLambda;
import org.openapitools.codegen.utils.URLPathUtils;

import com.samskivert.mustache.Mustache;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

public class VertxOas3ModelGenerator extends AbstractJavaCodegen
    implements CodegenConfig {

  private static final String DEFAULT_PACKAGE_BASE  = "br.com.c8tech.project";
  private static final String DEFAULT_PACKAGE_MODEL = DEFAULT_PACKAGE_BASE + ".model";
  public static final String  GENERATOR_NAME        = "vertx-dataobjects";
  private static final String TEMPLATE_FOLDER       = "vertx-dataobjects";

  protected String resourceFolder = "src/main/resources";

  public VertxOas3ModelGenerator() {
    super();

    modifyFeatureSet(features -> features
      .includeDocumentationFeatures(DocumentationFeature.Readme)
      .wireFormatFeatures(EnumSet.noneOf(WireFormatFeature.class))
      .securityFeatures(EnumSet.noneOf(SecurityFeature.class))
      .excludeSchemaSupportFeatures(SchemaSupportFeature.Polymorphism)
      .clientModificationFeatures(EnumSet.noneOf(ClientModificationFeature.class)));

    // defines java8 as default
    java8Mode = true;

    // defines java8 as default date format
    dateLibrary = "java8";

    // set the output folder here
    outputFolder = "generated-code" + File.separator + GENERATOR_NAME;

    /**
     * Template Location. This is the location which templates will be read
     * from. The generator will use the resource stream to attempt to read the
     * templates.
     */
    embeddedTemplateDir = templateDir = TEMPLATE_FOLDER;

    // set package names
    modelPackage = DEFAULT_PACKAGE_MODEL;
    apiTemplateFiles.clear();
    apiTestTemplateFiles.clear();
    apiDocTemplateFiles.clear();
    modelDocTemplateFiles.clear();

    importMapping.put("JsonObject",
                      "io.vertx.core.json.JsonObject");
    importMapping.put("DataObject",
                      "io.vertx.codegen.annotations.DataObject");
    importMapping.put("ParentModel",
                      "br.com.c8tech.oas3.codegen.vertx.AbstractModel");

    // cliOptions default redefinition need to be updated
    updateOption(CodegenConstants.MODEL_PACKAGE,
                 modelPackage);
    updateOption(CodegenConstants.TEMPLATE_DIR,
                 templateDir);

    additionalProperties.put("lambdaRemoveLineBreak",
                             (Mustache.Lambda) (fragment, writer) -> writer
                               .write(fragment.execute().replaceAll("\\r|\\n",
                                                                    "")));

    additionalProperties.put("lambdaTrimWhitespace",
                             new TrimWhitespaceLambda());

    additionalProperties.put("lambdaSplitString",
                             new SplitStringLambda());

    languageSpecificPrimitives.add("ParentModel");
  }

  @Override
  public CodegenModel fromModel(String name, @SuppressWarnings("rawtypes") Schema model) {

    CodegenModel codegenModel = super.fromModel(name,
                                                model);

    if (codegenModel.getParent() == null) {

      codegenModel.setParent("ParentModel");
    }

    if (codegenModel.imports.contains("ApiModel")) {
      // Remove io.swagger.annotations.ApiModel import
      codegenModel.imports.remove("ApiModel");
    }
    if (codegenModel.imports.contains("ApiModelProperty")) {
      // Remove io.swagger.annotations.ApiModelProperty import
      codegenModel.imports.remove("ApiModelProperty");
    }

    if (!BooleanUtils.toBoolean(codegenModel.isEnum)) {
      codegenModel.imports.add("DataObject");
      codegenModel.imports.add("JsonObject");
      codegenModel.imports.add("ParentModel");
      codegenModel.imports.add("Objects");
    }
    return codegenModel;
  }

  /**
   * Returns human-friendly help for the generator. Provide the consumer with
   * help tips, parameters here
   *
   * @return A string value for the help message
   */
  @Override
  public String getHelp() {
    return "Generates a java-Vertx Server Application project using Vert.x Web Contract API.";
  }

  /**
   * Configures a friendly name for the generator. This will be used by the
   * generator to select the library with the -g flag.
   *
   * @return the friendly name for the generator
   */
  @Override
  public String getName() {
    return GENERATOR_NAME;
  }

  /**
   * Configures the type of generator.
   *
   * @return the CodegenType for this generator
   * @see org.openapitools.codegen.CodegenType
   */
  @Override
  public CodegenType getTag() {
    return CodegenType.SCHEMA;
  }

  @Override
  public void preprocessOpenAPI(OpenAPI openAPI) {
    super.preprocessOpenAPI(openAPI);

    // add server port from the swagger file, 8080 by default
    URL url = URLPathUtils.getServerURL(openAPI,
                                        serverVariableOverrides());
    this.additionalProperties.put("serverPort",
                                  URLPathUtils.getPort(url,
                                                       8080));

    // retrieve api version from swagger file, 1.0.0-SNAPSHOT by default
    if (openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null) {
      artifactVersion = openAPI.getInfo().getVersion();
    }

    this.additionalProperties.remove("gson");
  }

  @Override
  public void processOpts() {
    super.processOpts();

    /**
     * Additional Properties. These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */

    additionalProperties.put("java8",
                             "true");
    additionalProperties.put(SUPPORT_ASYNC,
                             "true");
    apiTestTemplateFiles.clear();

    modelDocTemplateFiles.clear();
    apiDocTemplateFiles.clear();
    supportingFiles.clear();
    supportingFiles.add(new SupportingFile("openapi.mustache",
      resourceFolder,
      "openapi.yaml"));
    supportingFiles.add(new SupportingFile("package-info.mustache",
      this.getSourceFolder() + File.separator + modelPackage().replace(".",
                                                                       File.separator),
      "package-info.java"));

  }

}
