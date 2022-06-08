package io.quarkiverse.openapi.generator.deployment.codegen;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.openapitools.codegen.config.GlobalSettings;

import io.quarkiverse.openapi.generator.deployment.CodegenConfig;
import io.smallrye.config.PropertiesConfigSource;

public class SpecInputModel {

    private InputStream inputStream;
    private String filename;
    private final Map<String, String> codegenProperties = new HashMap<>();

    public SpecInputModel(final String filename, final InputStream inputStream) {
        requireNonNull(inputStream, "InputStream can't be null");
        requireNonNull(filename, "File name can't be null");
        this.inputStream = inputStream;
        this.filename = filename;
    }

    /**
     * @param filename the name of the file for reference
     * @param inputStream the content of the spec file
     * @param basePackageName the name of the package where the files will be generated
     * @throws IOException
     */
    public SpecInputModel(final String filename, final InputStream inputStream, final String basePackageName)
            throws IOException {
        this(filename, inputStream);
        if (Boolean.parseBoolean(GlobalSettings.getProperty(CodegenConfig.USE_SPEC_TITLE_PROPERTY_NAME))) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            inputStream.transferTo(baos);
            String titleFromInputStream = CodegenConfig.getTitleFromInputStream(new ByteArrayInputStream(baos.toByteArray()),
                    filename);
            this.filename = titleFromInputStream;
            // reset the InputStream
            this.inputStream = new ByteArrayInputStream(baos.toByteArray());
            this.codegenProperties.put(CodegenConfig.getBasePackagePropertyName(titleFromInputStream),
                    basePackageName.substring(0, basePackageName.lastIndexOf(".") + 1).concat(titleFromInputStream));
        } else {
            this.codegenProperties.put(CodegenConfig.getBasePackagePropertyName(Path.of(filename)), basePackageName);
        }
    }

    public String getFileName() {
        return filename;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public ConfigSource getConfigSource() {
        return new PropertiesConfigSource(this.codegenProperties, "properties", 0);
    }

    @Override
    public String toString() {
        return "SpecInputModel{" +
                "name='" + filename + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SpecInputModel that = (SpecInputModel) o;
        return inputStream.equals(that.inputStream) && filename.equals(that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputStream, filename);
    }
}
