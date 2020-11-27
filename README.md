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

import pro.husk.sqlannotations.annotations.DatabaseInfo;
import pro.husk.sqlannotations.annotations.DatabaseValue;
import pro.husk.sqlannotations.annotations.UniqueKey;

@DatabaseInfo(database = "database_name", table = "table_name")
public class SinkExampleObject implements AnnotatedSQLMember {

    private MySQL mysql;

    @UniqueKey("user-id")
    private int userId;

    @DatabaseValue("username")
    private String username;

    @DatabaseValue("age")
    private int age;

    public SinkExampleObject(MySQL mysql, int userId, String username, int age) {
        this.userId = userId;
        this.username = username;
        this.age = age;
    }

    @Override
    public void getMySQL() {
        return this.mysql;
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

        // Register our class to be processed
        SinkProcessor sinkProcessor = new SinkProcessor(sinkExampleObject);

        // That's it, it will now automatically sync to the database
    }

}
```