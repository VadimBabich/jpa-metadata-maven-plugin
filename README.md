# JPA Metadata Maven Plugin

The **JPA Metadata Maven Plugin** scans your Java source code for entity classes annotated with `@Table`, `@Column`, etc., analyzes relationships, and generates metadata classes for type-safe query construction. It's designed with modular backends in mind, including **R2DBC** and custom implementations.

Inspired by the **Hibernate JPA Static Metamodel Generator**, this plugin solves a key issue in **Spring Data JPA** and **Spring Data R2DBC**: custom queries rely heavily on string-based column references. If a column is renamed or removed, you won’t know until runtime.

This plugin helps prevent that by generating static metamodels from your entities, bringing compile-time safety and better IDE support to your query code.

> **_⚠️ Note:_** While this plugin improves safety, it doesn’t provide fully type-safe query building. Spring’s Criteria API still uses strings for some parts internally.

## Features

- Scans and parses annotated Java entity classes.
- Builds static metamodel classes automatically.
- Works with Spring Data R2DBC’s SQL DSL for safer query construction.
- Supports modular metadata generation.

> **_⚠️ Limitations_**
> - Only classes in the configured packageName are scanned.
> - It doesn’t process classes from dependencies or outside that package.
---

## Example: Find Users by Attribute Value
Let’s walk through a real use case: fetching users who have a specific attribute value. The schema includes two tables:
- `users` – stores basic user data.
- `user_attributes` – stores attributes for each user (one-to-many).

1. ### Entity Classes
   ```java
      // Represents the 'users' table
      @Immutable
      @Table("users")
      public record User(
              @Id @Column("user_id") String id,
              @Column("user_name") String name
      ) { }
      
      // Represents the 'user_attributes' table (linked to 'users' via userId)
      @Immutable
      @Table("user_attributes")
      public record UserAttribute(
              @Id @Column("usat_id") Long attributeId,
              @Column("usat_user_id") String userId, // Foreign key referencing 'users.user_id'
              @Column("usat_value") String attributeValue
      ) { }
      ```
1. ### Target SQL Query
   ```sql
   SELECT _user.*
   FROM users _user
   JOIN user_attributes _userattributes
        ON _userattributes.usat_user_id = _user.user_id
   WHERE _userattributes.usat_value = $1
   ```
1. ### Reactive Repository Method
   Using Spring’s SQL DSL with metamodels:
   ```java
     public Flux<User> findUsersByAttributeValue(String attributeValue) {
     
      // Define filtering criteria on the attribute value 
      Criteria criteria = Criteria.where(UserAttribute_.VALUE.name()).is(attributeValue);
      BoundCondition condition = queryMapper.getMappedObject(criteria);
   
      Table userTable = User_.getTable();
      Table attributeTable = UserAttribute_.getTable();
   
      // Construct the SQL join using Spring’s SQL DSL
      SelectJoin select = Select.builder()
              .select(AsteriskFromTable.create(userTable))
              .from(userTable)
              .join(attributeTable)
              .on(Conditions.isEqual(UserAttribute_.USER_ID, User_.ID))
              .where(condition.getCondition());
   
      return client
              .sql(() -> select.build().toString())
              .map(this::process) // Maps result row to User
              .all();
   }
   ```
1. ### Metamodel Classes (Generated)
   `UserAttribute_`
     ```java
     import org.springframework.data.r2dbc.config.StaticR2dbcEntityTemplateAccessor_;
     import org.springframework.data.relational.core.sql.Column_;
     import org.springframework.data.relational.core.sql.Table;
   
     public final class UserAttribute_ {
        public static final Column_ ATTRIBUTE_VALUE = new Column_(UserAttribute.class, "attributeValue");
   
        public static final Column_ USER_ID = new Column_(UserAttribute.class, "userId");
   
        public static final Column_ ATTRIBUTE_ID = new Column_(UserAttribute.class, "attributeId");
   
        private UserAttribute_() {
        }
   
        public static Table getTable() {
           return StaticR2dbcEntityTemplateAccessor_.getTable(UserAttribute.class);
        }
     }
     ```
     `User_`
     ```java
     import org.springframework.data.r2dbc.config.StaticR2dbcEntityTemplateAccessor_;
     import org.springframework.data.relational.core.sql.Column_;
     import org.springframework.data.relational.core.sql.Table;
     
     public final class User_ {
       public static final Column_ ID = new Column_(User.class, "id");
     
       public static final Column_ NAME = new Column_(User.class, "name");
     
       private User_() {
       }
     
       public static Table getTable() {
         return StaticR2dbcEntityTemplateAccessor_.getTable(User.class);
       }
     }
     ```
### Dynamic Criteria Support
While the example uses a specific condition (filter by attribute value), the real power lies in the flexibility: you can construct queries dynamically using any `Criteria`. Unlike `@Query`-based repository methods, this approach enables composable, reusable, and safer query building, all while benefitting from compile-time validation.

## Configuration

### Basic `pom.xml` Setup

```xml

<build>
  <plugins>
    <plugin>
      <groupId>io.github.vadimbabich</groupId>
      <artifactId>jpa-metadata-maven-plugin</artifactId>
      <version>1.0.0</version>
      <executions>
         <execution>
            <phase>generate-sources</phase>
            <goals>
               <goal>generate-metadata</goal>
            </goals>
         </execution>
      </executions>
      <configuration>
        <packageName>com.example.model</packageName>
        <outputDirectory>${project.build.directory}/generated-sources/r2dbc</outputDirectory>
      </configuration>
    </plugin>
  </plugins>
</build>
```

## Parameters

| Parameter               | Required | Default                          | Description                                                |
|-------------------------|----------|----------------------------------|------------------------------------------------------------|
| outputDirectory         | ❌        | ${project.build.outputDirectory} | Directory where generated metadata classes will be placed. |
| packageName             | ✅        | none                             | Package to scan for entity classes.                        |
| languageLevel           | ❌        | JAVA_17                          | Java language level used during parsing.                   |
| sourceDirectory         | ❌        | src/main/java                    | Path to the root directory of the Java source files.       |
| entityMetadataGenerator | ✅        | r2dbc                            | Name of the metadata generator to use (e.g., r2dbc).       |


## Sample Output
When the plugin runs, it logs a summary like:\

```
 Generating metadata for 'com.example.model' package with language level 'JAVA_17'
 Total found 5 entity classes. Metadata generated in: '/target/generated-sources/r2dbc'
 Included entities:
  • User
   ↳ Address
  • Product
   ```

---

## Integration in a Project

Make sure to register the generated `StaticR2dbcEntityTemplateAccessor_` so Spring Data can resolve entity metadata:
```java
@Configuration
@EnableTransactionManagement
@EnableR2dbcAuditing
public class DatabaseConfiguration {

   @Bean
   StaticR2dbcEntityTemplateAccessor_ staticAccessor(){
      return new StaticR2dbcEntityTemplateAccessor_();
   }
}
```

## IntelliJ IDEA Setup

Generated files land in `target/generated-sources`. To enable autocomplete and navigation:

1. Right-click the `target/generated-sources` folder.
1. Select **Mark Directory as → Generated Sources Root**.
IntelliJ will now treat these files like regular code.