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

import com.samskivert.mustache.Mustache;

import io.swagger.v3.oas.models.media.Schema;

public class VertxOas3ModelGenerator extends AbstractJavaCodegen
    implements CodegenConfig {

  public static final String  GENERATOR_NAME = "vertx-oas3-dataobjects";
  private static final String PARENT_MODEL   = "ParentModel";

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
    outputFolder = "generated-code" + File.separator + "java";

    setSortModelPropertiesByRequiredFlag(true);

    /**
     * Template Location. This is the location which templates will be read
     * from. The generator will use the resource stream to attempt to read the
     * templates.
     */
    embeddedTemplateDir = templateDir = Constants.TEMPLATE_FOLDER;

    // set package names
    modelPackage = Constants.DEFAULT_PACKAGE_MODEL;
    apiTemplateFiles.clear();
    apiTestTemplateFiles.clear();
    apiDocTemplateFiles.clear();
    modelDocTemplateFiles.clear();

    importMapping.put("JsonObject",
                      "io.vertx.core.json.JsonObject");
    importMapping.put("DataObject",
                      "io.vertx.codegen.annotations.DataObject");
    typeMapping.put(PARENT_MODEL,
                    "br.com.c8tech.oas3.codegen.vertx.AbstractModel");

    // cliOptions default redefinition need to be updated
    updateOption(CodegenConstants.MODEL_PACKAGE,
                 modelPackage);
    updateOption(CodegenConstants.INVOKER_PACKAGE,
                 this.getInvokerPackage());
    updateOption(CodegenConstants.API_PACKAGE,
                 apiPackage);

    additionalProperties.put("lambdaRemoveLineBreak",
                             (Mustache.Lambda) (fragment, writer) -> writer
                               .write(fragment.execute().replaceAll("\\r|\\n",
                                                                    "")));
    additionalProperties.put("lambdaTrimWhitespace",
                             new TrimWhitespaceLambda());

    additionalProperties.put("lambdaSplitString",
                             new SplitStringLambda());
  }

  @SuppressWarnings("rawtypes")
  @Override
  public CodegenModel fromModel(String name, Schema model) {

    CodegenModel codegenModel = super.fromModel(name,
                                                model);
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
      codegenModel.imports.add("Objects");

      if (codegenModel.getParent() == null) {
        String newType = typeMapping().getOrDefault(PARENT_MODEL,
                                                    PARENT_MODEL);
        codegenModel.imports.add(newType);
        codegenModel.setParent(newType);
      }
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
    return "Generates a java vert.x data-objects classes based on the OAS3 specification schema models.";
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
    return CodegenType.SERVER;
  }

  @Override
  public void processOpts() {
    super.processOpts();

    /**
     * Additional Properties. These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */

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
