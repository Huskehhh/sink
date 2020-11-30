package pro.husk.sqlannotations;

import lombok.Getter;
import lombok.Setter;
import pro.husk.mysql.MySQL;
import pro.husk.sqlannotations.annotations.DatabaseInfo;
import pro.husk.sqlannotations.annotations.DatabaseValue;
import pro.husk.sqlannotations.annotations.UniqueKey;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Class to process annotations and their attached fields
 */
public class SinkProcessor {

    private final Map<String, Field> databaseValues = new HashMap<>();

    private final MySQL mySQL;
    private final AnnotatedSQLMember member;

    private String dbName;
    private String dbTable;

    private String uniqueKeyEntryName;
    private Field uniqueKeyField;

    /**
     * Provides access to mark the SinkProcessor as dirty, causing it to be saved to db
     */
    @Setter
    private boolean dirty = true;

    /**
     * Provides access to the serialisation resolver, for those who wish to add their own resolvers
     */
    @Getter
    private final SerialisationResolver serialisationResolver;

    /**
     * Constructor
     *
     * @param member          Annotated class
     * @param finishedLoading task to run once finished loading
     */
    public SinkProcessor(AnnotatedSQLMember member, Runnable finishedLoading) {
        this.member = member;
        this.mySQL = member.getMySQL();
        this.serialisationResolver = new SerialisationResolver(member);
        this.initialise();

        GlobalSinkProcessor.getInstance().registerSinkProcessor(this, finishedLoading);
    }

    /**
     * Method to force a reload from database, replaces currently cached values with values from DB
     */
    public void loadFromDatabase() {
        try {
            String query = buildSelect();
            ResultSet results = mySQL.query(query);
            if (results.next()) {
                for (Map.Entry<String, Field> entry : databaseValues.entrySet()) {
                    String dbKey = entry.getKey();
                    Field field = entry.getValue();
                    Object dbValue = results.getObject(dbKey);
                    field.set(member, dbValue);
                }
            } else {
                runUpdate();
            }
        } catch (SQLException | IllegalAccessException throwables) {
            throwables.printStackTrace();
        }
        dirty = false;
    }

    /**
     * Method to initialise the SinkProcessor, loading annotated attributes and their values
     */
    private void initialise() {
        DatabaseInfo databaseInfo = member.getClass().getAnnotation(DatabaseInfo.class);

        if (databaseInfo != null) {
            this.dbName = databaseInfo.database();
            this.dbTable = databaseInfo.table();

            for (Field field : member.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                // Load all database values
                DatabaseValue databaseValue = field.getAnnotation(DatabaseValue.class);
                if (databaseValue != null) {
                    databaseValues.put(databaseValue.value(), field);
                }

                UniqueKey uniqueKey = field.getAnnotation(UniqueKey.class);
                if (uniqueKey != null) {
                    this.uniqueKeyField = field;
                    this.uniqueKeyEntryName = uniqueKey.value();
                }
            }
        }
    }

    /**
     * Method to run an update to database
     * <p>
     * Provides ability to overwrite database if needed.
     * Note: must be marked dirty first
     */
    public void runUpdate() {
        if (dirty) {
            String insert = buildInsert();
            try {
                mySQL.update(insert);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            } finally {
                dirty = false;
            }
        }
    }

    /**
     * Helper method to resolve the unique key field value
     *
     * @return String value of UniqueKey field
     */
    private String getUniqueKeyValue() {
        return serialisationResolver.resolve(uniqueKeyField);
    }

    /**
     * Helper method to build the SELECT query for loading from database
     *
     * @return String of SELECT query
     */
    private String buildSelect() {
        return "SELECT `" +
                String.join("`, `", databaseValues.keySet()) +
                "` FROM `" +
                dbName +
                "`.`" +
                dbTable +
                "` WHERE `" +
                uniqueKeyEntryName +
                "` = " +
                getUniqueKeyValue();
    }

    /**
     * Helper method to build the INSERT + UPDATE query for saving to database
     *
     * @return String of INSERT + UPDATE
     */
    private String buildInsert() {
        StringBuilder insert = new StringBuilder();

        insert.append("INSERT INTO `").append(dbName).append("`.`").append(dbTable).append("` (`").append(uniqueKeyEntryName).append("`, ");

        LinkedList<String> valueList = new LinkedList<>();

        // Append the entries
        int index = 0;
        int entrySetSize = databaseValues.size();
        for (Map.Entry<String, Field> entry : databaseValues.entrySet()) {
            String dbKey = entry.getKey();
            Field field = entry.getValue();
            String value = serialisationResolver.resolve(field);

            // Push to our linked list
            valueList.add(value);

            // Append the db key
            insert.append("`").append(dbKey).append("`");

            // Don't append the comma if this is the last entry in set
            if (index + 1 != entrySetSize) {
                insert.append(", ");
            }

            index++;
        }

        insert.append(") VALUES (").append(getUniqueKeyValue()).append(", ");

        // Append the values
        index = 0;
        int valueListSize = valueList.size();
        for (String value : valueList) {
            insert.append(value);

            if (index + 1 != valueListSize) {
                insert.append(", ");
            } else {
                insert.append(") ON DUPLICATE KEY ").append(buildUpdate()).append(";");
            }

            index++;
        }

        return insert.toString();
    }

    /**
     * Helper method to build the UPDATE portion of the INSERT query on duplicate key
     *
     * @return String of UPDATE
     */
    private String buildUpdate() {
        StringBuilder update = new StringBuilder();

        update.append("UPDATE ");

        int index = 0;
        int entrySetSize = databaseValues.size();

        for (Map.Entry<String, Field> entry : databaseValues.entrySet()) {
            String dbKey = entry.getKey();
            Field field = entry.getValue();
            String value = serialisationResolver.resolve(field);

            update.append("`").append(dbKey).append("` = ").append(value);

            // Don't append the comma if this is the last entry in set
            if (index + 1 != entrySetSize) {
                update.append(", ");
            }

            index++;
        }

        return update.toString();
    }
}