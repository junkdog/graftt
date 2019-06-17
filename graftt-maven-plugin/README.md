## graftt - maven plugin

Searches `target/classes` for any transplants and applies them. Recipient classes must also
live under `target/classes`.

This plugin is (potentially) useful when a project requires debug variants.

Note that intellij won't run this plugin by itself. 

### Usage

```xml
<plugin>
    <groupId>net.onedaybeard.graftt</groupId>
    <artifactId>graftt-maven-plugin</artifactId>
    <version>${project.version}</version>
    <executions>
        <execution>
            <id>graftt</id>
            <goals>
                <goal>graftt</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- also exposed as system property -Dgraftt.enable=true -->
        <enable>true</enable>
    </configuration>
</plugin>
```