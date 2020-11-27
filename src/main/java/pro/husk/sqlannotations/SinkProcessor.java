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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SinkProcessor {

    public static ScheduledThreadPoolExecutor threadPoolExecutor =
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5);

    private final Map<String, Field> databaseValues = new HashMap<>();

    private final MySQL mySQL;
    private final AnnotatedSQLMember member;

    private String dbName;
    private String dbTable;

    private String uniqueKeyEntryName;
    private Field uniqueKeyField;

    @Setter
    private boolean dirty = true;

    @Getter
    private final SerialisationResolver serialisationResolver;

    private final ScheduledFuture<?> updateTask;

    @Getter
    private final CompletableFuture<?> loadFromDatabaseFuture;

    public SinkProcessor(AnnotatedSQLMember member) {
        this.member = member;
        this.mySQL = member.getMySQL();
        this.serialisationResolver = new SerialisationResolver(member);
        this.initialise();

        this.loadFromDatabaseFuture = CompletableFuture.runAsync(this::loadFromDatabase, threadPoolExecutor);

        // Schedule to run every 10 seconds
        this.updateTask = threadPoolExecutor
                .scheduleAtFixedRate(this::runUpdateAsync, 10, 10, TimeUnit.SECONDS);
    }

    public void loadFromDatabase() {
        try {
            ResultSet results = mySQL.query(buildSelect());
            results.next();
            for (Map.Entry<String, Field> entry : databaseValues.entrySet()) {
                String dbKey = entry.getKey();
                Field field = entry.getValue();
                Object dbValue = results.getObject(dbKey);
                field.set(member, dbValue);
            }
        } catch (SQLException | IllegalAccessException throwables) {
            throwables.printStackTrace();
        }
        dirty = false;
    }

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

    public void runUpdateAsync() {
        if (dirty) {
            try {
                mySQL.update(buildInsert());
                dirty = false;
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public void cancelUpdateTask() {
        updateTask.cancel(false);
    }

    private String getUniqueKeyValue() {
        return serialisationResolver.resolve(uniqueKeyField);
    }

    private String buildSelect() {
        return "SELECT " +
                String.join(",", databaseValues.keySet()) +
                " FROM `" +
                dbName +
                "`.`" +
                dbTable +
                "` WHERE `" +
                uniqueKeyEntryName +
                "` = " +
                getUniqueKeyValue();
    }

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
            insert.append(dbKey);

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