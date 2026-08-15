package io.github.vadimbabich.metadata.generator.r2dbc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import javax.lang.model.element.Modifier;
import org.apache.maven.plugin.logging.Log;

/**
 * Generates the static accessor that reaches the application's {@code R2dbcEntityTemplate}.
 *
 * <p>The metamodel constants are {@code static final} and resolve their columns through the
 * mapping context, which only exists once Spring has started; a static holder populated by
 * {@code ApplicationContextAware} is what bridges the two. It is global mutable state, and the
 * runtime-library workstream owns removing it.
 *
 * @author Vadim Babich
 */
public class R2DbcEntityTemplateStaticHolderGeneratorJava implements JavaClassGenerator {

  private final File outputDir;
  private final Log log;
  private final ClassName r2dbcHolder;

  public R2DbcEntityTemplateStaticHolderGeneratorJava(File outputDir, Log log) {
    this.outputDir = outputDir;
    this.log = log;
    this.r2dbcHolder = ClassName.get("org.springframework.data.r2dbc.config",
        "StaticR2dbcEntityTemplateAccessor_");
  }

  @Override
  public ClassName className() {
    return r2dbcHolder;
  }

  @Override
  public void generateSourceFile() throws IOException {
    TypeSpec clazz = buildTypeSpec();
    writeJavaFile(clazz);
    log.debug(String.format("%s.java has been generated at target/generated-sources: %s.",
        r2dbcHolder.simpleName(), outputDir.getAbsolutePath()));
  }

  private FieldSpec createFieldSpecs() {
    ClassName lazy = ClassName.get("org.springframework.data.util", "Lazy");
    ClassName r2dbcEntityTemplate = ClassName.get("org.springframework.data.r2dbc.core",
        "R2dbcEntityTemplate");

    return FieldSpec.builder(ParameterizedTypeName.get(lazy, r2dbcEntityTemplate),
            "r2dbcEntityTemplate")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .build();
  }

  private MethodSpec createGetTemplateMethod() {
    ClassName r2dbcEntityTemplate = ClassName.get("org.springframework.data.r2dbc.core",
        "R2dbcEntityTemplate");

    return MethodSpec.methodBuilder("getTemplate")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(r2dbcEntityTemplate)
        .beginControlFlow("if (r2dbcEntityTemplate == null)")
        .addStatement("throw new $T(\n\t$S)", IllegalStateException.class,
            "StaticR2dbcEntityTemplateAccessor_ has not been initialized yet. Ensure it is registered as a Spring bean.")
        .endControlFlow()
        .addStatement("return r2dbcEntityTemplate.get()")
        .build();
  }

  private MethodSpec createGetPersistentEntityMethod() {
    ClassName relationalPersistentEntity = ClassName.get(
        "org.springframework.data.relational.core.mapping", "RelationalPersistentEntity");
    ClassName persistentEntity = ClassName.get("org.springframework.data.mapping",
        "PersistentEntity");

    return MethodSpec.methodBuilder("getPersistentEntity")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addTypeVariable(TypeVariableName.get("T"))
        .returns(ParameterizedTypeName.get(relationalPersistentEntity, TypeVariableName.get("T")))
        .addParameter(
            ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")),
            "entityType")
        .addStatement(
            "$T<?, ?> persistentEntity = getTemplate()\n\t.getConverter()\n\t.getMappingContext()\n\t.getPersistentEntity(entityType)",
            persistentEntity)
        .beginControlFlow("if (persistentEntity == null)")
        .addStatement("throw new $T($S + entityType.getSimpleName() +\n\t$S)",
            IllegalArgumentException.class, "Entity '",
            "' is not managed by the current mapping context.")
        .endControlFlow()
        .addStatement("return ($T) persistentEntity",
            ParameterizedTypeName.get(relationalPersistentEntity, TypeVariableName.get("T")))
        .build();
  }

  private MethodSpec createGetTableSimpleMethod() {
    ClassName table = ClassName.get("org.springframework.data.relational.core.sql", "Table");

    return MethodSpec.methodBuilder("getTable")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(ClassName.get(Class.class), "entityType")
        .returns(table)
        .addStatement("return getTable(entityType, null)")
        .build();
  }

  private MethodSpec createGetTableWithPrefixMethod() {
    ClassName sqlIdentifier = ClassName.get("org.springframework.data.relational.core.sql",
        "SqlIdentifier");
    ClassName table = ClassName.get("org.springframework.data.relational.core.sql", "Table");

    return MethodSpec.methodBuilder("getTable")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(ClassName.get(Class.class), "entityType")
        .addParameter(ClassName.get(String.class), "tableNamePrefix")
        .returns(table)
        .addStatement(
            "tableNamePrefix = (tableNamePrefix == null || tableNamePrefix.trim().isEmpty())\n\t? \"_\"\n\t: \"_\" + tableNamePrefix.trim().toLowerCase() + \"_\"")
        .addStatement("$T tableName = getPersistentEntity(entityType).getTableName()",
            sqlIdentifier)
        .addStatement("String alias = tableNamePrefix + entityType.getSimpleName().toLowerCase()")
        .addStatement("return $T.aliased(tableName.getReference(), alias)", table)
        .build();
  }

  private MethodSpec createSetApplicationContextMethod() {
    ClassName applicationContext = ClassName.get("org.springframework.context",
        "ApplicationContext");
    ClassName beansException = ClassName.get("org.springframework.beans", "BeansException");
    ClassName lazy = ClassName.get("org.springframework.data.util", "Lazy");
    ClassName r2dbcEntityTemplate = ClassName.get("org.springframework.data.r2dbc.core",
        "R2dbcEntityTemplate");

    return MethodSpec.methodBuilder("setApplicationContext")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(applicationContext, "applicationContext")
        .addException(beansException)
        .addStatement("r2dbcEntityTemplate = $T.of(() -> applicationContext.getBean($T.class))",
            lazy, r2dbcEntityTemplate)
        .build();
  }

  private TypeSpec buildTypeSpec() {
    ClassName applicationContextAware = ClassName.get("org.springframework.context",
        "ApplicationContextAware");
    ClassName component = ClassName.get("org.springframework.stereotype", "Component");

    return TypeSpec.classBuilder(r2dbcHolder.simpleName())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterface(applicationContextAware)
        .addAnnotation(component)
        .addField(createFieldSpecs())
        .addMethod(createGetTemplateMethod())
        .addMethod(createGetPersistentEntityMethod())
        .addMethod(createGetTableSimpleMethod())
        .addMethod(createGetTableWithPrefixMethod())
        .addMethod(createSetApplicationContextMethod())
        .build();
  }

  private void writeJavaFile(TypeSpec clazz) throws IOException {
    JavaFile javaFile = JavaFile.builder(r2dbcHolder.packageName(), clazz)
        .addFileComment(FILE_HEADER)
        .build();
    javaFile.writeTo(Paths.get(outputDir.getAbsolutePath()));
  }
}
