package io.quarkiverse.openapi.generator.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.openapitools.codegen.config.GlobalSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.common.utils.StringUtil;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

// This configuration is read in codegen phase (before build time), the annotation is for document purposes and avoiding quarkus warns
@ConfigRoot(name = CodegenConfig.CODEGEN_TIME_CONFIG_PREFIX, phase = ConfigPhase.BUILD_TIME)
public class CodegenConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodegenConfig.class);

    static final String CODEGEN_TIME_CONFIG_PREFIX = "openapi-generator.codegen";

    public static final String API_PKG_SUFFIX = ".api";
    public static final String MODEL_PKG_SUFFIX = ".model";
    public static final String VERBOSE_PROPERTY_NAME = "quarkus." + CODEGEN_TIME_CONFIG_PREFIX + ".verbose";
    public static final String USE_SPEC_TITLE_PROPERTY_NAME = "quarkus." + CODEGEN_TIME_CONFIG_PREFIX + ".useTitleAsId";
    // package visibility for unit tests
    static final String BUILD_TIME_SPEC_PREFIX_FORMAT = "quarkus." + CODEGEN_TIME_CONFIG_PREFIX + ".spec.%s";
    private static final String BASE_PACKAGE_PROP_FORMAT = "%s.base-package";
    private static final String SKIP_FORM_MODEL_PROP_FORMAT = "%s.skip-form-model";

    /**
     * OpenAPI Spec details for codegen configuration.
     */
    @ConfigItem(name = "spec")
    public Map<String, SpecItemConfig> specItem;

    /**
     * Whether to log the internal generator codegen process in the default output or not.
     */
    @ConfigItem(name = "verbose", defaultValue = "false")
    public boolean verbose;

    /**
     * Whether to use the info.title field of the OpenAPI document as title. Default is false, the sanitized file name is used
     * as title.
     */
    @ConfigItem(name = "useTitleAsId", defaultValue = "false")
    public boolean useTitleAsId;

    public static String resolveApiPackage(final String basePackage) {
        return String.format("%s%s", basePackage, API_PKG_SUFFIX);
    }

    public static String resolveModelPackage(final String basePackage) {
        return String.format("%s%s", basePackage, MODEL_PKG_SUFFIX);
    }

    public static String getBasePackagePropertyName(final String filename) {
        return String.format(BASE_PACKAGE_PROP_FORMAT, String.format(BUILD_TIME_SPEC_PREFIX_FORMAT, filename));
    }

    public static String getBasePackagePropertyName(final Path openApiFilePath) {
        return String.format(BASE_PACKAGE_PROP_FORMAT, getBuildTimeSpecPropertyPrefix(openApiFilePath));
    }

    public static String getSkipFormModelPropertyName(final Path openApiFilePath) {
        return String.format(SKIP_FORM_MODEL_PROP_FORMAT, getBuildTimeSpecPropertyPrefix(openApiFilePath));
    }

    /**
     * Gets the config prefix for a given OpenAPI file in the path.
     * For example, given a path like /home/luke/projects/petstore.json, the returned value is
     * `quarkus.openapi-generator."petstore_json"`.
     * Every the periods (.) in the file name will be replaced by underscore (_).
     */
    public static String getBuildTimeSpecPropertyPrefix(final Path openApiFilePath) {
        return String.format(BUILD_TIME_SPEC_PREFIX_FORMAT, getSanitizedFileName(openApiFilePath));
    }

    public static String getSanitizedFileName(final Path openApiFilePath) {
        final boolean useTitleAsId = Boolean
                .parseBoolean(GlobalSettings.getProperty(CodegenConfig.USE_SPEC_TITLE_PROPERTY_NAME));
        return StringUtil.replaceNonAlphanumericByUnderscores(
                (useTitleAsId ? getTitleFromSpec(openApiFilePath) : openApiFilePath.getFileName().toString()));
    }

    public static String getTitleFromSpec(final Path openApiFilePath) {
        if (Files.exists(openApiFilePath)) {
            try {
                InputStream inputStream = Files.newInputStream(openApiFilePath);
                return getTitleFromInputStream(inputStream, openApiFilePath.getFileName().toString());
            } catch (IOException e) {
                LOGGER.error("Failure reading InputStream for model {}", openApiFilePath.getFileName(), e);
            }
        }
        return openApiFilePath.getFileName().toString();
    }

    public static String getTitleFromInputStream(InputStream inputStream, String filename) throws IOException {
        SwaggerParseResult result = new OpenAPIParser().readContents(new String(inputStream.readAllBytes()), null, null);
        OpenAPI openAPI = result.getOpenAPI();
        return StringUtil.replaceNonAlphanumericByUnderscores(openAPI.getInfo().getTitle());
    }
}
