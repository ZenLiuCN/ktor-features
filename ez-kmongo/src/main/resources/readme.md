# ez-kmongo
ref [kmongo](http://litote.org/kmongo/quick-start/)
# use typesafe query
```xml
<plugin>
    <executions>
        <execution>
            <id>kapt</id>
            <goals>
                <goal>kapt</goal>
            </goals>
            <configuration>
                <annotationProcessorPaths>
                    <annotationProcessorPath>
                        <groupId>org.litote.kmongo</groupId>
                        <artifactId>kmongo-annotation-processor</artifactId>
                        <version>${kmongo.version}</version>
                    </annotationProcessorPath>
                </annotationProcessorPaths>
            </configuration>
        </execution>

        <execution>
            <id>compile</id>
            <phase>compile</phase>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>

    </executions>
</plugin>

```
