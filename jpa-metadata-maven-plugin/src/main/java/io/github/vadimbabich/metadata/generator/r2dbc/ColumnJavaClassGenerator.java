package io.github.vadimbabich.metadata.generator.r2dbc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import javax.lang.model.element.Modifier;
import org.apache.maven.plugin.logging.Log;

/**
 * Generates {@code Column_}, which resolves a field name to its SQL column lazily, on first use,
 * because the mapping context is not available while the metamodel constants are initialised.
 *
 * <p>It is emitted into Spring's own {@code ...core.sql} package, which splits that package across
 * two artifacts. Consumers compile against this shape today, so it is frozen until the runtime
 * library replaces it.
 *
 * @author Vadim Babich
 */
public class ColumnJavaClassGenerator implements JavaClassGenerator {

  private static final String SQL_PACKAGE = "org.springframework.data.relational.core.sql";

  private final Log log;

  private final File outputDir;

  private final ClassName r2dbcHolder;
  private final ClassName extendedColumnClass;

  public ColumnJavaClassGenerator(ClassNameAware r2dbcHolder, File outputDir, Log log) {
    this.r2dbcHolder = r2dbcHolder.className();
    this.outputDir = outputDir;
    this.log = log;

    this.extendedColumnClass = ClassName.get(SQL_PACKAGE, "Column_");
  }

  @Override
  public ClassName className() {
    return extendedColumnClass;
  }

  @Override
  public void generateSourceFile() throws IOException {
    TypeSpec columnClass = buildColumnClass();

    JavaFile javaFile = JavaFile.builder(SQL_PACKAGE, columnClass)
        .addFileComment(FILE_HEADER)
        .build();

    javaFile.writeTo(Paths.get(outputDir.getAbsolutePath()));

    log.debug(String.format("%s.java has been generated at target/generated-sources: %s.",
        extendedColumnClass, outputDir.getAbsolutePath()));
  }

  private TypeSpec buildColumnClass() {
    ClassName column = ClassName.get(SQL_PACKAGE, "Column");
    ClassName expression = ClassName.get(SQL_PACKAGE, "Expression");
    ClassName lazy = ClassName
        .get("org.springframework.data.util", "Lazy");

    FieldSpec delegateField = FieldSpec.builder(lazyOf(column), "delegate", Modifier.PRIVATE,
            Modifier.FINAL)
        .build();

    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(Class.class, "entityType")
        .addParameter(String.class, "fieldName")
        .addStatement(
            "this.delegate = $T.of(() -> getTable(entityType).column(getColumnName(entityType, fieldName)))",
            lazy)
        .build();

    return TypeSpec.classBuilder(extendedColumnClass)
        .addModifiers(Modifier.PUBLIC)
        .addSuperinterface(expression)
        .addField(delegateField)
        .addMethod(constructor)
        .addMethod(generateNameMethod())
        .addMethod(generateToStringMethod())
        .addMethod(generateEqualsMethod())
        .addMethod(generateHashCodeMethod())
        .addMethod(generateGetColumnNameMethod())
        .addMethod(generateGetTableMethod())
        .build();
  }


  private MethodSpec generateNameMethod() {
    return MethodSpec.methodBuilder("name")
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return delegate.get().getName().getReference()")
        .build();
  }

  private MethodSpec generateToStringMethod() {
    return MethodSpec.methodBuilder("toString")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(String.class)
        .addStatement("return delegate.get().toString()")
        .build();
  }

  private MethodSpec generateEqualsMethod() {
    return MethodSpec.methodBuilder("equals")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(boolean.class)
        .addParameter(Object.class, "other")
        .addStatement("if (other instanceof Column_ column) { other = column.delegate.get(); }")
        .addStatement("return delegate.get().equals(other)")
        .build();
  }

  private MethodSpec generateHashCodeMethod() {
    return MethodSpec.methodBuilder("hashCode")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .returns(int.class)
        .addStatement("return delegate.get().hashCode()")
        .build();
  }

  private MethodSpec generateGetColumnNameMethod() {
    ClassName relationalPersistentProperty = ClassName.get(
        "org.springframework.data.relational.core.mapping", "RelationalPersistentProperty");
    ClassName relationalPersistentEntity = ClassName.get(
        "org.springframework.data.relational.core.mapping", "RelationalPersistentEntity");

    return MethodSpec.methodBuilder("getColumnName")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addParameter(Class.class, "entityType")
        .addParameter(String.class, "fieldName")
        .returns(String.class)
        .addStatement("$T<?> persistentEntity = $T.getPersistentEntity(entityType)",
            relationalPersistentEntity, r2dbcHolder)
        .addStatement("$T persistentProperty = persistentEntity.getPersistentProperty(fieldName)",
            relationalPersistentProperty)
        .beginControlFlow("if (persistentProperty == null)")
        .addStatement(
            "throw new $T(\"Field '\" + fieldName + \"' for entity '\" + entityType.getSimpleName() + \"' was not found.\")",
            IllegalArgumentException.class)
        .endControlFlow()
        .addStatement("return persistentProperty.getColumnName().getReference()")
        .build();
  }

  private MethodSpec generateGetTableMethod() {
    ClassName table = ClassName.get(SQL_PACKAGE, "Table");

    return MethodSpec.methodBuilder("getTable")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addParameter(Class.class, "entityType")
        .returns(table)
        .addStatement("return $T.getTable(entityType)", r2dbcHolder)
        .build();
  }

  private ParameterizedTypeName lazyOf(ClassName type) {
    return ParameterizedTypeName.get(ClassName.get("org.springframework.data.util", "Lazy"), type);
  }
}
