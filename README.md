# sink

A simple, annotation based library aimed at replacing the need to manually interface with a MySQL database

## 🔧 Usage

First, add these to your ``pom.xml``

```xml

<repository>
    <id>husk</id>
    <url>https://maven.husk.pro/repository/maven-public/</url>
</repository>
```

and...

```xml

<dependency>
    <groupId>pro.husk</groupId>
    <artifactId>sink</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Code example

#### Example object class

```java
package pro.husk.example;

import pro.husk.sqlannotations.SinkProcessor;
import pro.husk.sqlannotations.annotations.DatabaseInfo;
import pro.husk.sqlannotations.annotations.DatabaseValue;
import pro.husk.sqlannotations.annotations.UniqueKey;

@DatabaseInfo(database = "database_name", table = "table_name")
public class SinkExampleObject implements AnnotatedSQLMember {

    private MySQL mysql;

    @UniqueKey("user-id")
    private final int userId;

    @DatabaseValue("username")
    private String username;

    @DatabaseValue("age")
    private int age;

    private SinkProcessor sinkProcessor;

    public SinkExampleObject(MySQL mysql, int userId, String username, int age) {
        this.userId = userId;
        this.username = username;
        this.age = age;

        // Register our class to be processed
        this.sinkProcessor = new SinkProcessor(this);

        // Do something once the database values are loaded (or defaults are inserted)
        sinkProcessor.getLoadFromDatabaseFuture().thenRun(() ->
                System.out.println("Data has been loaded!"));
    }

    @Override
    public void getMySQL() {
        return this.mysql;
    }

    public void setAge(int age) {
        this.age = age;

        // Flag our object to be updated
        sinkProcessor.setDirty(true);
    }
}
```

#### Example setup

```java
package pro.husk.example;

import pro.husk.sqlannotations.SinkProcessor;

public class Sink {

    public static void main(String[] args) {
        // Create our example object
        SinkExampleObject sinkExampleObject = new SinkExampleObject(1, "Bob", 10);

        // Now we can call setAge for example, and it will automatically sync to db
        sinkExampleObject.setAge(10);
    }

}
```