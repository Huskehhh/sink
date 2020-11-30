package pro.husk.sqlannotations;

import pro.husk.mysql.MySQL;

/**
 * Class that utilises the annotations provided by sink
 */
public interface AnnotatedSQLMember {
    /**
     * Provides access to the MySQL object used by the annotated class
     * @return {@link pro.husk.mysql.MySQL} object
     */
    MySQL getMySQL();
}
