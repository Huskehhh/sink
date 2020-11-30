package pro.husk.sqlannotations.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation housing the unique key of the given {@link pro.husk.sqlannotations.annotations.DatabaseInfo}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface UniqueKey {
    /**
     * Unique key field name
     *
     * @return field name
     */
    String value();
}
