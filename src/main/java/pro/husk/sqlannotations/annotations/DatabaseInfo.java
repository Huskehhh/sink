package pro.husk.sqlannotations.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation housing the database information of the given object
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface DatabaseInfo {
    /**
     * Database to save information to
     *
     * @return database name
     */
    String database();

    /**
     * Table to save information to
     *
     * @return table name
     */
    String table();
}
