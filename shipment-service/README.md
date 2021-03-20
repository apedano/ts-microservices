#### Set the active profile

##### Set the profile in Maven pom file
```xml
<profiles>
         <profile>
             <id>develop</id>
             <activation>
                 <activeByDefault>true</activeByDefault>
             </activation>
             <properties>
                 <maven.profile>develop</maven.profile>
             </properties>
         </profile>
     </profiles>
```

##### Add the resource bundle configuration in the pom file
```xml
<build>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <plugins>
            ...
        </plugins>
    </build>
```

##### Add property to the _application.properties_ file
```properties
spring.profiles.active=@maven.profile@
```

You see that the resource value refers to the property defined in the profile *maven.profile*

##### Define a profile related properties file
Add a _application-develop.properties_ file to the resource folder with the following setting (which is already present in the default _application.properties_)

```properties
hello.message=Hello, Spring-K8S Developer!
```

##### Use the configured properties
We use it in a controller class

```java
@RestController
@RequestMapping("/hello")
public class HelloController {

    @Value("${hello.message}")
    private String message;

    @GetMapping
    public String sayHello() {
        return message;
    }
}
```


