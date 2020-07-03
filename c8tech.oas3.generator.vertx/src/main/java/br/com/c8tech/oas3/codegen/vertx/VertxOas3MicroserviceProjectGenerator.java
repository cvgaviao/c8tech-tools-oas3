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

import static org.openapitools.codegen.utils.StringUtils.camelize;

import java.io.File;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import org.apache.commons.lang3.BooleanUtils;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenType;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.languages.AbstractJavaCodegen;
import org.openapitools.codegen.meta.GeneratorMetadata;
import org.openapitools.codegen.meta.Stability;
import org.openapitools.codegen.meta.features.ClientModificationFeature;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.meta.features.SchemaSupportFeature;
import org.openapitools.codegen.meta.features.SecurityFeature;
import org.openapitools.codegen.meta.features.WireFormatFeature;
import org.openapitools.codegen.templating.mustache.SplitStringLambda;
import org.openapitools.codegen.templating.mustache.TrimWhitespaceLambda;
import org.openapitools.codegen.utils.URLPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.samskivert.mustache.Mustache;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;

public class VertxOas3MicroserviceProjectGenerator extends AbstractJavaCodegen
    implements CodegenConfig {

  private static final String DEFAULT_LINKS_CLASS   = "Links";
  private static final String DEFAULT_META_CLASS    = "Meta";
  private static final String DEFAULT_PROBLEM_CLASS = "ParentModelClass";

  public static final String  GENERATOR_NAME       = "vertx-oas3-microservice";
  private static final Logger LOG                  =
      LoggerFactory.getLogger(VertxOas3MicroserviceProjectGenerator.class);
  private static final String PARENT_HANDLER_CLASS = "ParentHandlerClass";
  private static final String PARENT_MODEL_CLASS   = "ParentModelClass";

  protected String resourceFolder = "src/main/resources";

  public VertxOas3MicroserviceProjectGenerator() {
    super();

    generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
      .stability(Stability.EXPERIMENTAL).build();

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

    enableMinimalUpdate = true;
    /**
     * Template Location. This is the location which templates will be read
     * from. The generator will use the resource stream to attempt to read the
     * templates.
     */
    embeddedTemplateDir = templateDir = Constants.TEMPLATE_FOLDER;

    // set package names
    modelPackage = Constants.DEFAULT_PACKAGE_MODEL;
    apiPackage = Constants.DEFAULT_PACKAGE_API;
    invokerPackage = Constants.DEFAULT_PACKAGE_INVOKER;

    importMapping.put("RoutingContext",
                      "io.vertx.ext.web.RoutingContext");
    importMapping.put("JsonObject",
                      "io.vertx.core.json.JsonObject");
    importMapping.put("RequestParameters",
                      "io.vertx.ext.web.validation.RequestParameters");
    importMapping.put("DataObject",
                      "io.vertx.codegen.annotations.DataObject");
    importMapping.put(PARENT_HANDLER_CLASS,
                      "br.com.c8tech.mmarket.backend.common.handlers.AbstractWebApiHandler");
    importMapping.put("AbstractModel",
                      "br.com.c8tech.oas3.codegen.vertx.AbstractModel");
    importMapping.put(DEFAULT_LINKS_CLASS,
                      "br.com.c8tech.mmarket.backend.common.models.Links");
    importMapping.put(DEFAULT_META_CLASS,
                      "br.com.c8tech.mmarket.backend.common.models.Meta");
    importMapping.put(DEFAULT_PROBLEM_CLASS,
                      "br.com.c8tech.mmarket.backend.common.models.BaseErrorSchema");
    typeMapping.put(PARENT_MODEL_CLASS,
                    "AbstractModel");
    typeMapping.put("BaseErrorSchemaRFC7807",
                    DEFAULT_PROBLEM_CLASS);

    // cliOptions default redefinition need to be updated
    updateOption(CodegenConstants.MODEL_PACKAGE,
                 modelPackage);
    updateOption(CodegenConstants.INVOKER_PACKAGE,
                 this.getInvokerPackage());
    updateOption(CodegenConstants.API_PACKAGE,
                 apiPackage);
    hideGenerationTimestamp = false;
    enablePostProcessFile = true;

  }

  @Override
  public void addOperationToGroup(String tag, String resourcePath, Operation operation,
    CodegenOperation op, Map<String, List<CodegenOperation>> operations) {

    LOG.info("tag: {}",
             tag);
    LOG.info("resourcePath: {}",
             resourcePath);
    LOG.info("operation: {}",
             operation.getSummary());

    super.addOperationToGroup(tag,
                              resourcePath,
                              operation,
                              op,
                              operations);
  }

  @Override
  public String apiFilename(String templateName, String tag) {
    String suffix = apiTemplateFiles().get(templateName);
    String result;
    if ("api-mock.mustache".equals(templateName)) {
      result = apiFileFolder() + File.separator + toApiFilename(tag) + "Mock" + suffix;
    } else if ("api-interface.mustache".equals(templateName)) {
      result = apiFileFolder() + File.separator + toApiFilename(tag) + suffix;

    } else if ("api-impl.mustache".equals(templateName)) {
      result = apiFileFolder() + File.separator + toApiFilename(tag) + "Impl" + suffix;

    } else {
      result = super.apiFilename(templateName,
                                 tag);
    }
    return result;
  }

  private String computeServiceId(String pathname, Entry<HttpMethod, Operation> entry) {
    String operationId = entry.getValue().getOperationId();
    return (operationId != null) ? operationId
        : entry.getKey().name() + pathname.replace('-',
                                                   '_')
          .replace('/',
                   '_')
          .replaceAll("[{}]",
                      "");
  }

  protected String extractPortFromHost(String host) {
    if (host != null) {
      int portSeparatorIndex = host.indexOf(':');
      if (portSeparatorIndex >= 0 && portSeparatorIndex + 1 < host.length()) {
        return host.substring(portSeparatorIndex + 1);
      }
    }
    return "8080";
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
        String newType = typeMapping().getOrDefault(PARENT_MODEL_CLASS,
                                                    PARENT_MODEL_CLASS);
        codegenModel.imports.add(newType);
        codegenModel.setParent(newType);
      }
    }
    return codegenModel;
  }

  @Override
  public CodegenOperation fromOperation(String path, String httpMethod,
    Operation operation, List<Server> servers) {
    CodegenOperation codegenOperation = super.fromOperation(path,
                                                            httpMethod,
                                                            operation,
                                                            servers);
    String newType = typeMapping().getOrDefault(PARENT_HANDLER_CLASS,
                                                PARENT_HANDLER_CLASS);
    codegenOperation.vendorExtensions.put("x-codegen-parent-class",
                                          newType);
    String newImport = importMapping().get(newType);

    codegenOperation.vendorExtensions.put("x-codegen-parent-import",
                                          newImport);

    codegenOperation.imports.add("JsonObject");
    codegenOperation.imports.add("RoutingContext");
    codegenOperation.imports.add("RequestParameters");

    return codegenOperation;
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

  }

  @Override
  public void processOpts() {
    super.processOpts();

    /**
     * Additional Properties. These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */

    apiTestTemplateFiles.clear();
    apiTemplateFiles.clear();
    modelDocTemplateFiles.clear();
    apiDocTemplateFiles.clear();

    apiTemplateFiles.put("api-interface.mustache",
                         Constants.JAVA_EXTENSION);
    apiTemplateFiles.put("api-impl.mustache",
                         Constants.JAVA_EXTENSION);
    apiTemplateFiles.put("api-mock.mustache",
                         Constants.JAVA_EXTENSION);

    supportingFiles.add(new SupportingFile("openapi-generator-ignore.mustache",
      "",
      ".openapi-generator-ignore").doNotOverwrite());
    supportingFiles.add(new SupportingFile("openapi.mustache",
      resourceFolder,
      "openapi.yaml"));
    supportingFiles.add(new SupportingFile("package-info.mustache",
      this.getSourceFolder() + File.separator + modelPackage().replace(".",
                                                                       File.separator),
      "package-info.java"));

    // add lambda for mustache templates
    additionalProperties.put("lambdaEscapeDoubleQuote",
                             (Mustache.Lambda) (fragment, writer) -> writer.write(fragment
                               .execute().replaceAll("\"",
                                                     Matcher.quoteReplacement("\\\""))));
    additionalProperties.put("lambdaSplitString",
                             new SplitStringLambda());
    additionalProperties.put("lambdaRemoveLineBreak",
                             (Mustache.Lambda) (fragment, writer) -> writer
                               .write(fragment.execute().replaceAll("\\r|\\n",
                                                                    "")));
    additionalProperties.put("lambdaTrimWhitespace",
                             new TrimWhitespaceLambda());
  }

  @Override
  public String toApiFilename(String tagName) {
    return camelize(tagName) + "Handler";
  }

  @Override
  public String toApiName(String name) {
    if (name.length() == 0) {
      return "DefaultController";
    }
    name = name.replaceAll("[^a-zA-Z0-9]+",
                           "_");
    return camelize(name) + "Handler";
  }

  @Override
  public String toOperationId(String pOperationId) {
    return super.toOperationId(camelize(pOperationId));
  }

}
