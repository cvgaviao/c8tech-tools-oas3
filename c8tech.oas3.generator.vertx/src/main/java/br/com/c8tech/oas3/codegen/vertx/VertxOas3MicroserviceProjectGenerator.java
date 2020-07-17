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

import static java.util.stream.Collectors.toMap;
import static org.openapitools.codegen.utils.StringUtils.camelize;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenModelFactory;
import org.openapitools.codegen.CodegenModelType;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenProperty;
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
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.samskivert.mustache.Mustache;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.util.SchemaTypeUtil;

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

  @SuppressWarnings("rawtypes")
  private static Boolean isAliasOfSimpleTypes(Schema schema) {
    return (!ModelUtils.isObjectSchema(schema) && !ModelUtils.isArraySchema(schema)
        && !ModelUtils.isMapSchema(schema) && !ModelUtils.isComposedSchema(schema)
        && schema.getEnum() == null);
  }

  @SuppressWarnings({ "rawtypes" })
  private Map<String, Schema> allDefinitions;

  private Map<String, CodegenModel> models = new HashMap<>();

  protected String resourceFolder = "src/main/resources";

  public VertxOas3MicroserviceProjectGenerator() {
    super();

    generatorMetadata = GeneratorMetadata.newBuilder(generatorMetadata)
      .stability(Stability.EXPERIMENTAL).build();

    modifyFeatureSet(features -> features
      .includeDocumentationFeatures(DocumentationFeature.Readme)
      .wireFormatFeatures(EnumSet.noneOf(WireFormatFeature.class))
      .securityFeatures(EnumSet.noneOf(SecurityFeature.class))
      .includeSchemaSupportFeatures(SchemaSupportFeature.Polymorphism)
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

  /**
   * Add variables (properties) to codegen model (list of properties, various
   * flags, etc)
   *
   * @param pCodegenModel
   *          Codegen model
   * @param pVars
   *          list of codegen properties (e.g. vars, allVars) to be updated with
   *          the new properties
   * @param pProperties
   *          a map of properties (schema)
   * @param pMandatory
   *          a set of required properties' name
   */
  @SuppressWarnings("rawtypes")
  private void addVars(CodegenModel pCodegenModel, List<CodegenProperty> pVars,
    Map<String, Schema> pProperties, Set<String> pMandatory) {

    for (Map.Entry<String, Schema> entry : pProperties.entrySet()) {

      final String key = entry.getKey();
      final Schema prop = entry.getValue();

      if (prop == null) {
        LOG.warn("Please report the issue. There shouldn't be null property for {}",
                 key);
      } else {

        final CodegenProperty cp = fromProperty(key,
                                                prop);
        if (cp.isEnum) {
          // FIXME: if supporting inheritance, when called a second time for allProperties it is possible for
          // m.hasEnums to be set incorrectly if allProperties has enumerations but properties does not.
          pCodegenModel.hasEnums = true;
        }

        // set model's hasOnlyReadOnly to false if the property is read-only
        if (!Boolean.TRUE.equals(cp.isReadOnly)) {
          pCodegenModel.hasOnlyReadOnly = false;
        }

        if (cp.isContainer) {
          // TODO revise the logic to include map
          addImport(pCodegenModel,
                    typeMapping.get("array"));
          CodegenProperty innerCp = cp;
          while (innerCp != null) {
            addImport(pCodegenModel,
                      innerCp.complexType);
            innerCp = innerCp.items;
          }
        } else {
          addImport(pCodegenModel,
                    cp.baseType);
        }
        pVars.add(cp);

        cp.required = pMandatory.contains(key);
        // if required, add to the list "requiredVars"
        if (Boolean.TRUE.equals(cp.required)) {
          pCodegenModel.requiredVars.add(cp);
          pCodegenModel.hasRequired = true;
        } else { // else add to the list "optionalVars" for optional property
          pCodegenModel.optionalVars.add(cp);
          pCodegenModel.hasOptional = true;
        }

        // if readonly, add to readOnlyVars (list of properties)
        if (Boolean.TRUE.equals(cp.isReadOnly)) {
          pCodegenModel.readOnlyVars.add(cp);
        } else { // else add to readWriteVars (list of properties)
          // duplicated properties will be removed by removeAllDuplicatedProperty later
          pCodegenModel.readWriteVars.add(cp);
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private void addVars(CodegenModel pCodegenModel, Map<String, Schema> pProperties,
    List<String> pRequired, Map<String, Schema> pAllProperties,
    List<String> pAllRequired) {

    pCodegenModel.hasRequired = false;
    pCodegenModel.hasEnums = false;
    if (pProperties != null && !pProperties.isEmpty()) {
      pCodegenModel.hasVars = true;
      Set<String> mandatory =
          pRequired == null ? Collections.<String> emptySet() : new TreeSet<>(pRequired);

      // update "vars" without parent's properties (all, required)
      addVars(pCodegenModel,
              pCodegenModel.vars,
              pProperties,
              mandatory);
      pCodegenModel.allMandatory = pCodegenModel.mandatory = mandatory;
    } else {
      pCodegenModel.emptyVars = true;
      pCodegenModel.hasVars = false;
    }

    if (pAllProperties != null) {
      Set<String> allMandatory = pAllRequired == null ? Collections.<String> emptySet()
          : new TreeSet<>(pAllRequired);
      // update "vars" with parent's properties (all, required)
      addVars(pCodegenModel,
              pCodegenModel.allVars,
              pAllProperties,
              allMandatory);
      pCodegenModel.allMandatory = allMandatory;
    } else { // without parent, allVars and vars are the same
      pCodegenModel.allVars = pCodegenModel.vars;
      pCodegenModel.allMandatory = pCodegenModel.mandatory;
    }

    // loop through list to update property name with toVarName
    Set<String> renamedMandatory = new TreeSet<>();
    Iterator<String> mandatoryIterator = pCodegenModel.mandatory.iterator();
    while (mandatoryIterator.hasNext()) {
      renamedMandatory.add(toVarName(mandatoryIterator.next()));
    }
    pCodegenModel.mandatory = renamedMandatory;

    Set<String> renamedAllMandatory = new TreeSet<>();
    Iterator<String> allMandatoryIterator = pCodegenModel.allMandatory.iterator();
    while (allMandatoryIterator.hasNext()) {
      renamedAllMandatory.add(toVarName(allMandatoryIterator.next()));
    }
    pCodegenModel.allMandatory = renamedAllMandatory;
  }

  @Override
  public String apiFilename(String templateName, String tag) {
    String suffix = apiTemplateFiles().get(templateName);
    String result;
    if ("api-mock.mustache".equals(templateName)) {
      result = apiFileFolder() + File.separator + "impl" + File.separator
          + toApiFilename(tag) + "Mock" + suffix;
    } else if ("api-interface.mustache".equals(templateName)) {
      result = apiFileFolder() + File.separator + toApiFilename(tag) + suffix;

    } else if ("api-impl.mustache".equals(templateName)) {
      result = apiFileFolder() + File.separator + "impl" + File.separator
          + toApiFilename(tag) + "Impl" + suffix;

    } else {
      result = super.apiFilename(templateName,
                                 tag);
    }
    return result;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public CodegenModel fromModel(String pName, Schema pSchema) { //NOSONAR

    if (models.containsKey(pName)) {
      return models.get(pName);
    }

    if (allDefinitions == null) {
      allDefinitions = ModelUtils.getSchemas(this.openAPI);
    }

    if (typeAliases == null) {
      // Only do this once during first call
      typeAliases = getAllSchemaAliases(allDefinitions);
    }

    // unalias schema
    Schema schema = ModelUtils.unaliasSchema(this.openAPI,
                                             pSchema,
                                             importMapping);
    if (schema == null) {
      LOG.error("Schema '{}' was not found",
                pName);
      return null;
    }
    CodegenModel codegenModel;
    LOG.info("Processing model '{}'",
             pName);
    codegenModel = CodegenModelFactory.newInstance(CodegenModelType.MODEL);
    if (reservedWords.contains(pName)) {
      codegenModel.name = escapeReservedWord(pName);
    } else {
      codegenModel.name = pName;
    }
    codegenModel.title = escapeText(schema.getTitle());
    codegenModel.description = escapeText(schema.getDescription());
    codegenModel.unescapedDescription = schema.getDescription();
    codegenModel.classname = toModelName(pName);
    codegenModel.classVarName = toVarName(pName);
    codegenModel.classFilename = toModelFilename(pName);
    codegenModel.modelJson = Json.pretty(schema);
    codegenModel.externalDocumentation = schema.getExternalDocs();
    if (schema.getExtensions() != null && !schema.getExtensions().isEmpty()) {
      codegenModel.getVendorExtensions().putAll(schema.getExtensions());
    }
    // check if the unaliased schema is an alias of simple OAS types
    codegenModel.isAlias =
        (typeAliases.containsKey(pName) || isAliasOfSimpleTypes(schema));

    if (schema.getDeprecated() != null) {
      codegenModel.isDeprecated = schema.getDeprecated();
    }

    if (schema.getXml() != null) {
      codegenModel.xmlPrefix = schema.getXml().getPrefix();
      codegenModel.xmlNamespace = schema.getXml().getNamespace();
      codegenModel.xmlName = schema.getXml().getName();
    }
    if (isAnyTypeSchema(schema)) {
      // The 'null' value is allowed when the OAS schema is 'any type'.
      // See https://github.com/OAI/OpenAPI-Specification/issues/1389
      if (Boolean.FALSE.equals(schema.getNullable())) {
        LOG
          .error("Schema '{}' is any type, which includes the 'null' value. 'nullable' cannot be set to 'false'",
                 pName);
      }
      codegenModel.isNullable = true;
    }

    if (Boolean.TRUE.equals(sortModelPropertiesByRequiredFlag)) {
      Comparator<CodegenProperty> comparator = (one, another) -> {
        if (one.required == another.required)
          return 0;
        else if (one.required)
          return -1;
        else
          return 1;
      };
      Collections.sort(codegenModel.vars,
                       comparator);
      Collections.sort(codegenModel.allVars,
                       comparator);
    }

    // process 'additionalProperties'
    if (schema.getAdditionalProperties() == null) {
      codegenModel.isAdditionalPropertiesTrue = false;
    } else if (schema.getAdditionalProperties() instanceof Boolean) {
      if (Boolean.TRUE.equals(schema.getAdditionalProperties())) {
        codegenModel.isAdditionalPropertiesTrue = true;
      } else {
        codegenModel.isAdditionalPropertiesTrue = false;
      }
    } else {
      codegenModel.isAdditionalPropertiesTrue = false;
    }

    if (ModelUtils.isArraySchema(schema)) {

      codegenModel.isArrayModel = true;
      codegenModel.arrayModelType = fromProperty(pName,
                                                 schema).complexType;
      addParentContainer(codegenModel,
                         pName,
                         schema);
    }

    models.put(codegenModel.name,
               codegenModel);
    addImport(codegenModel,
              "DataObject");

    if (schema instanceof ComposedSchema) {

      ComposedSchema composed = (ComposedSchema) schema;
      if (composed.getAllOf() != null) {

        processAllOfComposedSchemaModel(composed,
                                        codegenModel);
      }

      if (composed.getAnyOf() != null) {

        processAnyOfComposedSchemaModel(composed,
                                        codegenModel);
      } else if (composed.getOneOf() != null) {

        processOneOfComposedSchemaModel(composed,
                                        codegenModel);
      }
    } else {
      processSimpleTypeSchemaModel(schema,
                                   codegenModel);
      setPojoImports(codegenModel);
    }

    // post process model properties
    if (codegenModel.vars != null) {
      for (CodegenProperty prop : codegenModel.vars) {
        postProcessModelProperty(codegenModel,
                                 prop);
      }
      codegenModel.hasVars = !codegenModel.vars.isEmpty();
    }
    if (codegenModel.allVars != null) {
      for (CodegenProperty prop : codegenModel.allVars) {
        postProcessModelProperty(codegenModel,
                                 prop);
      }
    }

    // remove duplicated properties
    codegenModel.removeAllDuplicatedProperty();

    return codegenModel;
  }

  @Override
  public CodegenOperation fromOperation(String path, String httpMethod,
    Operation operation, List<Server> servers) {
    CodegenOperation codegenOperation = super.fromOperation(path,
                                                            httpMethod,
                                                            operation,
                                                            servers);

    if (httpMethod.equalsIgnoreCase(io.swagger.models.HttpMethod.GET.name())) {

      codegenOperation.vendorExtensions.put("x-codegen-isget",
                                            true);
    }

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
   * Determine all of the types in the model definitions (schemas) that are
   * aliases of simple types.
   *
   * @param pSchemas
   *          The complete set of model definitions (schemas).
   * @return A mapping from model name to type alias
   */
  @SuppressWarnings("rawtypes")
  Map<String, String> getAllSchemaAliases(Map<String, Schema> pSchemas) {
    if (pSchemas == null || pSchemas.isEmpty()) {
      return Map.of();
    }

    Map<String, String> aliases = new HashMap<>();
    for (Map.Entry<String, Schema> entry : pSchemas.entrySet()) {
      Schema schema = entry.getValue();
      if (Boolean.TRUE.equals(isAliasOfSimpleTypes(schema))) {
        String oasName = entry.getKey();
        String schemaType = getPrimitiveType(schema);
        aliases.put(oasName,
                    schemaType);
      }

    }

    return aliases;
  }

  /**
   * Returns human-friendly help for the generator. Provide the consumer with
   * help tips, parameters here
   *
   * @return A string value for the help message
   */
  @Override
  public String getHelp() {
    return "Generates a java vert.x based classes from an OAS3 specification model.";
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

  //  protected String extractPortFromHost(String host) {
  //    if (host != null) {
  //      int portSeparatorIndex = host.indexOf(':');
  //      if (portSeparatorIndex >= 0 && portSeparatorIndex + 1 < host.length()) {
  //        return host.substring(portSeparatorIndex + 1);
  //      }
  //    }
  //    return "8080";
  //  }

  /**
   * Return the OAI type (e.g. integer, long, etc) corresponding to a schema.
   * 
   * <pre>
   * $ref
   * </pre>
   * 
   * is not taken into account by this method.
   * <p>
   * If the schema is free-form (i.e. 'type: object' with no properties) or
   * inline schema, the returned OAI type is 'object'.
   *
   * @param schema
   * @return type
   */
  @SuppressWarnings("rawtypes")
  private String getPrimitiveType(Schema schema) {
    if (schema == null) {
      throw new NullPointerException("schema cannot be null in getPrimitiveType");
    }
    if (ModelUtils.isNullType(schema)) {
      // The 'null' type is allowed in OAS 3.1 and above. It is not supported by OAS 3.0.x,
      // though this tooling supports it.
      return "null";
    } else if (ModelUtils.isStringSchema(schema)
        && SchemaTypeUtil.NUMBER_TYPE.equals(schema.getFormat())) {
      // special handle of type: string, format: number
      return "BigDecimal";
    } else if (ModelUtils.isByteArraySchema(schema)) {
      return "ByteArray";
    } else if (ModelUtils.isFileSchema(schema)) {
      return "file";
    } else if (ModelUtils.isBinarySchema(schema)) {
      return SchemaTypeUtil.BINARY_FORMAT;
    } else if (ModelUtils.isBooleanSchema(schema)) {
      return SchemaTypeUtil.BOOLEAN_TYPE;
    } else if (ModelUtils.isDateSchema(schema)) {
      return SchemaTypeUtil.DATE_FORMAT;
    } else if (ModelUtils.isDateTimeSchema(schema)) {
      return SchemaTypeUtil.DATE_TIME_FORMAT;
    } else if (ModelUtils.isNumberSchema(schema)) {
      if (schema.getFormat() == null) { // no format defined
        return SchemaTypeUtil.NUMBER_TYPE;
      } else if (ModelUtils.isFloatSchema(schema)) {
        return SchemaTypeUtil.FLOAT_FORMAT;
      } else if (ModelUtils.isDoubleSchema(schema)) {
        return SchemaTypeUtil.DOUBLE_FORMAT;
      } else {
        LOG.warn("Unknown `format` {} detected for type `number`. Defaulting to `number`",
                 schema.getFormat());
        return SchemaTypeUtil.NUMBER_TYPE;
      }
    } else if (ModelUtils.isIntegerSchema(schema)) {
      if (ModelUtils.isLongSchema(schema)) {
        return "long";
      } else {
        return schema.getType(); // integer
      }
    } else if (ModelUtils.isMapSchema(schema)) {
      return "map";
    } else if (ModelUtils.isArraySchema(schema)) {
      if (ModelUtils.isSet(schema)) {
        return "set";
      } else {
        return "array";
      }
    } else if (ModelUtils.isUUIDSchema(schema)) {
      return SchemaTypeUtil.UUID_FORMAT.toUpperCase();
    } else if (ModelUtils.isURISchema(schema)) {
      return "URI";
    } else if (ModelUtils.isStringSchema(schema)) {
      if (typeMapping.containsKey(schema.getFormat())) {
        // If the format matches a typeMapping (supplied with the --typeMappings flag)
        // then treat the format as a primitive type.
        // This allows the typeMapping flag to add a new custom type which can then
        // be used in the format field.
        return schema.getFormat();
      }
      return SchemaTypeUtil.STRING_TYPE;
    } else if (isFreeFormObject(schema)) {
      // Note: the value of a free-form object cannot be an arbitrary type. Per OAS specification,
      // it must be a map of string to values.
      return SchemaTypeUtil.OBJECT_TYPE;
    } else if (schema.getProperties() != null && !schema.getProperties().isEmpty()) { // having property implies it's a model
      return SchemaTypeUtil.OBJECT_TYPE;
    } else if (isAnyTypeSchema(schema)) {
      return "AnyType";
    } else if (StringUtils.isNotEmpty(schema.getType())) {
      LOG.warn("Unknown type found in the schema: {}",
               schema.getType());
      return schema.getType();
    }
    // The 'type' attribute has not been set in the OAS schema, which means the value
    // can be an arbitrary type, e.g. integer, string, object, array, number...
    // TODO: we should return a different value to distinguish between free-form object
    // and arbitrary type.
    return SchemaTypeUtil.OBJECT_TYPE;
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
  public Map<String, Object> postProcessAllModels(Map<String, Object> pObjs) {
    return pObjs;
  }

  @Override
  public void postProcessModelProperty(CodegenModel pCodegenModel,
    CodegenProperty pCodegenProperty) {

    if (pCodegenProperty.isContainer && pCodegenModel.oneOf.isEmpty()) {
      if ("array".equalsIgnoreCase(pCodegenProperty.containerType)) {
        addImport(pCodegenModel,
                  "ArrayList");
      } else if ("set".equalsIgnoreCase(pCodegenProperty.containerType)) {
        addImport(pCodegenModel,
                  "LinkedHashSet");
      } else if ("map".equalsIgnoreCase(pCodegenProperty.containerType)) {
        addImport(pCodegenModel,
                  "HashMap");
      }
    }
  }

  @Override
  public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs,
    List<Object> allModels) {
    // Remove imports of List, ArrayList, Map and HashMap as they are
    // imported in the template already.
    @SuppressWarnings("unchecked")
    List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
    Pattern pattern = Pattern.compile("java\\.util\\.(List|ArrayList|Map|HashMap)");
    for (Iterator<Map<String, String>> itr = imports.iterator(); itr.hasNext();) {
      String itrImport = itr.next().get("import");
      if (pattern.matcher(itrImport).matches()) {
        itr.remove();
      }
    }
    return objs;
  }

  @SuppressWarnings({ "rawtypes" })
  private void processAllOfComposedSchemaModel(ComposedSchema pComposedSchema,
    CodegenModel pCodegenModel) {

    List<Schema> subschemas = pComposedSchema.getAllOf();
    for (Schema schema : subschemas) {
      CodegenModel parentCodegenModel = null;
      Schema parentSchema = null;
      if (schema.get$ref() != null && !schema.get$ref().isBlank()) {
        String modelName = ModelUtils.getSimpleRef(schema.get$ref());
        parentSchema = allDefinitions.get(modelName);
        parentCodegenModel = fromModel(modelName,
                                       parentSchema);
      } else {
        parentCodegenModel = fromModel(schema.getName(),
                                       schema);
      }
      if (parentSchema == null || parentCodegenModel == null) {
        return;
      }
      models.put(parentCodegenModel.getName(),
                 parentCodegenModel);

      pCodegenModel.setParent(parentCodegenModel.getName());
      pCodegenModel.setParentModel(parentCodegenModel);

      // add import
      addImport(pCodegenModel,
                parentCodegenModel.getName());
      setPojoImports(pCodegenModel);

    }

  }

  private void processAnyOfComposedSchemaModel(ComposedSchema pComposedSchema,
    CodegenModel pCodegenModel) {
    // NotImplementedException
  }

  @SuppressWarnings("rawtypes")
  private void processOneOfComposedSchemaModel(ComposedSchema pComposedSchema,
    CodegenModel pCodegenModel) {

    // if schema has properties outside of oneOf also add them to pCodegenModel
    if (pComposedSchema.getProperties() != null
        && !pComposedSchema.getProperties().isEmpty()) {
      if (pComposedSchema.getOneOf() != null && !pComposedSchema.getOneOf().isEmpty()) {
        LOG
          .warn("'oneOf' is intended to include only the additional optional OAS extension discriminator object. "
              + "For more details, see https://json-schema.org/draft/2019-09/json-schema-core.html#rfc.section.9.2.1.3 and the OAS section on 'Composition and Inheritance'.");
      }
      addVars(pCodegenModel,
              unaliasPropertySchema(pComposedSchema.getProperties()),
              pComposedSchema.getRequired(),
              null,
              null);
    }

    List<Schema> subschemas = pComposedSchema.getOneOf();
    for (Schema schema : subschemas) {
      CodegenModel child;
      if (schema.get$ref() != null && !schema.get$ref().isBlank()) {
        String modelName = ModelUtils.getSimpleRef(schema.get$ref());
        Schema childSchema = allDefinitions.get(modelName);
        child = fromModel(modelName,
                          childSchema);
      } else {
        child = fromModel(schema.getName(),
                          schema);
      }
      models.put(child.getName(),
                 child);
      pCodegenModel.oneOf.add(child.getName());

      // add the corresponding interface
      child.setInterfaces(List.of(pCodegenModel.getName()));

      // add import
      addImport(child,
                pCodegenModel.getName());
    }

  }

  @Override
  public void processOpts() {
    super.processOpts();

    apiTestTemplateFiles.clear();
    modelDocTemplateFiles.clear();
    modelTestTemplateFiles.clear();
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

    // Composed schemas can have the 'additionalProperties' keyword, as specified in JSON schema.
    // In principle, this should be enabled by default for all code generators. However due to limitations
    // in other code generators, support needs to be enabled on a case-by-case basis.
    // The flag below should be set for all Java libraries, but the templates need to be ported
    // one by one for each library.
    supportsAdditionalPropertiesWithComposedSchema = true;

  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void processSimpleTypeSchemaModel(Schema pSchema, CodegenModel pCodegenModel) {

    pCodegenModel.dataType = getSchemaType(pSchema);
    if (pSchema.getEnum() != null && !pSchema.getEnum().isEmpty()) {
      pCodegenModel.isEnum = true;
      // comment out below as allowableValues is not set in post processing model enum
      pCodegenModel.allowableValues = new HashMap<String, Object>();
      pCodegenModel.allowableValues.put("values",
                                        pSchema.getEnum());
    }
    if (ModelUtils.isMapSchema(pSchema)) {
      addAdditionPropertiesToCodeGenModel(pCodegenModel,
                                          pSchema);
      pCodegenModel.isMapModel = true;
    } else if (ModelUtils.isIntegerSchema(pSchema)) { // integer type
      // NOTE: Integral schemas as CodegenModel is a rare use case and may be removed at a later date.
      // Sync of properties is done for consistency with other data types like CodegenParameter/CodegenProperty.
      ModelUtils.syncValidationProperties(pSchema,
                                          pCodegenModel);

      pCodegenModel.isNumeric = Boolean.TRUE;
      if (ModelUtils.isLongSchema(pSchema)) { // int64/long format
        pCodegenModel.isLong = Boolean.TRUE;
      } else { // int32 format
        pCodegenModel.isInteger = Boolean.TRUE;
      }
    } else if (ModelUtils.isStringSchema(pSchema)) {
      // NOTE: String schemas as CodegenModel is a rare use case and may be removed at a later date.
      // Sync of properties is done for consistency with other data types like CodegenParameter/CodegenProperty.
      ModelUtils.syncValidationProperties(pSchema,
                                          pCodegenModel);
      pCodegenModel.isString = Boolean.TRUE;
    } else if (ModelUtils.isNumberSchema(pSchema)) {
      // NOTE: Number schemas as CodegenModel is a rare use case and may be removed at a later date.
      // Sync of properties is done for consistency with other data types like CodegenParameter/CodegenProperty.
      ModelUtils.syncValidationProperties(pSchema,
                                          pCodegenModel);
      pCodegenModel.isNumeric = Boolean.TRUE;
      if (ModelUtils.isFloatSchema(pSchema)) { // float
        pCodegenModel.isFloat = Boolean.TRUE;
      } else if (ModelUtils.isDoubleSchema(pSchema)) { // double
        pCodegenModel.isDouble = Boolean.TRUE;
      } else { // type is number and without format
        pCodegenModel.isNumber = Boolean.TRUE;
      }
    }

    if (Boolean.TRUE.equals(pSchema.getNullable())) {
      pCodegenModel.isNullable = Boolean.TRUE;
    }

    // passing null to allProperties and allRequired as there's no parent
    addVars(pCodegenModel,
            unaliasPropertySchema(pSchema.getProperties()),
            pSchema.getRequired(),
            null,
            null);
  }

  private void setPojoImports(CodegenModel pCodegenModel) {
    addImport(pCodegenModel,
              "Objects");
    addImport(pCodegenModel,
              "JsonObject");
    String converter = pCodegenModel.classname + "Converter";

    importMapping.put(converter,
                      modelPackage + "." + converter);
    pCodegenModel.imports.add("DataObject");
    pCodegenModel.imports.add(converter);

  }

  //  @Override
  //  public void preprocessOpenAPI(OpenAPI openAPI) {
  //    super.preprocessOpenAPI(openAPI);
  //    // add server port from the swagger file, 8080 by default
  //    URL url = URLPathUtils.getServerURL(openAPI,
  //                                        serverVariableOverrides());
  //    this.additionalProperties.put("serverPort",
  //                                  URLPathUtils.getPort(url,
  //                                                       8080));
  //
  //    // retrieve api version from swagger file, 1.0.0-SNAPSHOT by default
  //    if (openAPI.getInfo() != null && openAPI.getInfo().getVersion() != null) {
  //      artifactVersion = openAPI.getInfo().getVersion();
  //    }
  //
  //  }

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
  public String toEnumName(CodegenProperty property) {
    return sanitizeName(camelize(property.name)) + "Enum";
  }

  @Override
  public String toModelName(String pName) {
    this.setLegacyDiscriminatorBehavior(false);

    return super.toModelName(pName);
  }

  @Override
  public String toOperationId(String pOperationId) {
    return super.toOperationId(camelize(pOperationId));
  }

  /**
   * Loop through properties and unalias the reference if $ref (reference) is
   * defined
   *
   * @param properties
   *          model properties (schemas)
   * @return model properties with direct reference to schemas
   */
  @SuppressWarnings("rawtypes")
  private Map<String, Schema> unaliasPropertySchema(Map<String, Schema> properties) {
    if (properties != null) {
      return properties.entrySet().stream().map(item -> {
        item.setValue(ModelUtils.unaliasSchema(this.openAPI,
                                               item.getValue(),
                                               importMapping()));
        return item;
      }).collect(toMap(Map.Entry::getKey,
                       Map.Entry::getValue));
    }
    return Map.of();
  }

}
