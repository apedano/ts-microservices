### Activate minikube with LoadBalancer support

https://minikube.sigs.k8s.io/docs/handbook/accessing/

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

### Build Docker image

We will use the spring-boot-maven-plugin to do so

```xml
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            ...
        </plugins>
```

#### Change build image name in the pom file
```xml
<spring-boot.build-image.imageName>448877123666/${project.artifactId}:${project.version}
```
 Where 448877123666 is the image repository in docker-hub.

#### Run spring-boot build image target
It is very easy to build the Docker image (based on Buildpacks and Paketo) from the spring boot project
```
mvn spring-boot:build-image -P develop
```
This is the output of the command
```
[INFO] <<< spring-boot-maven-plugin:2.4.4:build-image (default-cli) < package @ shipment-service <<<
[INFO] 
[INFO] 
[INFO] --- spring-boot-maven-plugin:2.4.4:build-image (default-cli) @ shipment-service ---
[INFO] Building image 'docker.io/448877123666/shipment-service:0.0.1-SNAPSHOT'
...
[INFO] Successfully built image 'docker.io/448877123666/shipment-service:0.0.1-SNAPSHOT'
```
To confirm that is correctly built

```
docker images
```

```
REPOSITORY                      TAG              IMAGE ID       CREATED         SIZE
paketobuildpacks/run            base-cnb         3263e1744182   44 hours ago    88.4MB
paketobuildpacks/builder        base             a02e520246e8   41 years ago    666MB
448877123666/shipment-service   0.0.1-SNAPSHOT   7204d5251230   41 years ago    261MB\
```
We can also run the image if we like

```
docker run -p 8081:8080 -e HELLO_MESSAGE="Hello from Docker" 448877123666/shipment-service:0.0.1-SNAPSHOT
```

Now you can call

http://localhost:8081/hello

#### Push the image

We will be using the kubernetes-maven-plugin provided by the JKube project to push the image to Docker Hub, so let’s install it now:

```xml
        <plugin>
			<groupId>org.eclipse.jkube</groupId>
			<artifactId>kubernetes-maven-plugin</artifactId>
			<version>1.0.0-alpha-4</version>
		</plugin>
```

The kubernetes-maven-plugin is designed to be a seamless experience for all of your Java/Kubernetes needs. Normally, it would be building the image for us, but since we’re using the spring-boot-maven-plugin to build our image, we need some additional configuration properties to our pom.xml file to let it know which image we’re using:

```xml
 <properties>
        <java.version>11</java.version>
        <spring-boot.build-image.imageName>448877123666/${project.artifactId}:${project.version}</spring-boot.build-image.imageName>
        <jkube.generator.name>${spring-boot.build-image.imageName}</jkube.generator.name>
        <jkube.skip.tag>true</jkube.skip.tag>
    </properties>
```

Here, we’re setting the _jkube.generator.name_ property to the exact same image name we’re using for the spring-boot-maven-plugin. 
By default, JKube tries to push the latest tag for any SNAPSHOT project. However, since we aren’t building a _latest_ tag locally, it will not be able to find it. We use the _docker.skip.tag_ property to turn that feature off.
JKube supports various <a href="https://www.eclipse.org/jkube/docs/kubernetes-maven-plugin#authentication">authentication options</a>, for Docker Hub, but we are going to use the credentials stored in our `~/.docker/config.json` file. 
Since we established these credentials earlier via docker login, we can use the kubernetes-maven-plugin to push our image to Docker Hub:

```
docker login
Authenticating with existing credentials...
Login Succeeded

mvn k8s:push
```

```
[INFO] k8s: 0.0.1-SNAPSHOT: digest: sha256:6f17818078b50f619e9450e28f2cca7f7b929726f621b267ff718829c1904f55 size: 5123
[INFO] k8s: Pushed 448877123666/shipment-service:0.0.1-SNAPSHOT in 6 seconds 
```

### Create Kubernetes declarative files

With zero configuration, we can use the kubernetes-maven-plugin to create Kubernetes Deployment and Service resource definitions to run our application:

```
mvn k8s:resource
```

```
[INFO] k8s: Running generator spring-boot
[INFO] k8s: spring-boot: Using Docker image quay.io/jkube/jkube-java-binary-s2i:0.0.9 as base / builder
[INFO] k8s: Using resource templates from C:\projects\personal\ts-microservices\shipment-service\src\main\jkube
[INFO] k8s: jkube-controller: Adding a default Deployment
[INFO] k8s: jkube-service: Adding a default service 'shipment-service' with ports [8080]
[INFO] k8s: jkube-healthcheck-spring-boot: Adding readiness probe on port 8080, path='/actuator/health', scheme='HTTP', with initial delay 10 seconds
[INFO] k8s: jkube-healthcheck-spring-boot: Adding liveness probe on port 8080, path='/actuator/health', scheme='HTTP', with initial delay 180 seconds
[INFO] k8s: jkube-service-discovery: Using first mentioned service port '8080' 
[INFO] k8s: jkube-revision-history: Adding revision history limit to 2
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

JKube will generate the resource definitions in the `target/classes/META-INF/jkube/kubernetes` directory.
The service file generated doesn't contain any type tha is by default `ClusterIp`, we can specify a different type with 
If we want to create a LoadBalancer (we have it in minikube) we can

```xml
<jkube.enricher.jkube-service.type>LoadBalancer</jkube.enricher.jkube-service.type>
```

### Deploying to Kubernetes
To deploy our application to Kubernetes, we will ask JKube to “apply” the resource definitions to our cluster
(using `kubectl`):

```
mvn k8s:apply
```

```
[INFO] k8s: Using Kubernetes at https://192.168.146.45:8443/ in namespace default with manifest C:\projects\personal\ts-microservices\shipment-service\target\classes\META-INF\jkube\kubernetes.yml 
[INFO] k8s: Creating a Service from kubernetes.yml namespace default name shipment-service
[INFO] k8s: Created Service: target\jkube\applyJson\default\service-shipment-service.json
[INFO] k8s: Creating a Deployment from kubernetes.yml namespace default name shipment-service
[INFO] k8s: Created Deployment: target\jkube\applyJson\default\deployment-shipment-service.json
[INFO] k8s: HINT: Use the command `kubectl get pods -w` to watch your pods start up
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

#### Getting the service on minikube

```
minikube service <service_name>
```

### Kube-ifying the Application
To take advantages of some of the features Kubernetes has to offer in our Spring Boot application, we will be using the Spring Cloud Kubernetes library. To do that, first we need to import the Spring Cloud Bill of Materials, 
or BOM to our project as a dependencyManagement entry:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>Hoxton.SR5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

And the corresponding dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-kubernetes-all</artifactId>
</dependency>
```

#### Configuring the Application
To build cloud-native, <a href="https://12factor.net/">twelve-factor</a> applications, we need to make sure the application can be configured externally. Kubernetes provides ConfigMaps for storing non-sensitive application configuration data. In order to leverage ConfigMaps in our application, we will first need to modify it to use an externalized property. Let’s revisit our HelloController:

```java
@Value("${welcome:default_value}")
private String welcome;
```

##### Create ConfigMap file

Then we can create our file `/src/main/jkube/configmap.yml`

```yaml
metadata:
  name: ${project.artifactId}
data:
  application.properties: |
    # spring application properties file
    welcome = Hello from Kubernetes ConfigMap!!!
    dummy = some value
```

##### Create deployment file

First we need to add the base deployment file in `/src/main/jkube/deployment.yml` with the following content:

```yaml
spec:
  replicas: 1
  template:
    spec:
      volumes:
        - name: config
          configMap:
            name: ${project.artifactId}
            items:
              - key: application.properties
                path: application.properties
      containers:
        - volumeMounts:
            - name: config
              mountPath: /deployments/config
          env:
            - name: KUBERNETES_NAMESPACE
              valueFrom:
                fieldRef:
                  apiVersion: v1
                  fieldPath: metadata.namespace
      serviceAccount: ribbon
```
Note the `volumes` with name _config_ referring to the `application.properties` section of the configmap file created in the previous section.

##### Create Role and RoleBinding files
Spring Cloud Kubernetes will need access to the Kubernetes API, so the <a href="https://cloud.spring.io/spring-cloud-static/spring-cloud-kubernetes/2.0.0.M1/reference/html/#service-account">service account</a> used by the Deployment will need elevated permissions. 
JKube allows us to define <a href="https://www.eclipse.org/jkube/docs/kubernetes-maven-plugin#_resource_fragments">resource fragments</a> in the `src/main/jkube` directory which will be enriched with other data and generated as resource definitions. 
Let’s define a `Role` resource fragment named `role.yml` in the `src/main/jkube` directory that sets up the permissions required by Spring Cloud Kubernetes:

```yaml
metadata:
  name: spring-cloud-k8s
  namespace: default
rules:
  - apiGroups: [""]
    resources: ["pods","services","endpoints","configmaps"]
    verbs: ["get", "list", "watch"]
```

A Role by itself doesn’t really affect anything. The Role must be bound to a “subject” (in our case, a `ServiceAccount`) 
in order for it to be used. Let’s define a `RoleBinding` resource fragment in the `src/main/jkube` directory named `rolebinding.yml`:

```yaml
metadata:
  name: spring-cloud-k8s
subjects:
  - kind: ServiceAccount
    name: ${project.artifactId}
    namespace: default
roleRef:
  kind: Role
  name: spring-cloud-k8s
  apiGroup: rbac.authorization.k8s.io
```

Here, we’re binding the `spring-cloud-k8s` role to the `spring-k8s` service account (defined using a Maven property reference in the resource fragment). 
But, where did the `spring-k8s` service account come from? 
We will need to define it in the `pom.xml` file under the `kubernetes-maven-plugin` configuration using JKube’s XML configuration mechanism:

```xml
<plugin>
    <groupId>org.eclipse.jkube</groupId>
    <artifactId>kubernetes-maven-plugin</artifactId>
    <version>1.0.0-alpha-4</version>
    <configuration>
        <resources>
            <serviceAccounts>
                <serviceAccount>
                    <name>${project.artifactId}</name>
                    <deploymentRef>${project.artifactId}</deploymentRef>
                </serviceAccount>
            </serviceAccounts>
        </resources>
    </configuration>
</plugin>
```

We again use the Maven project’s artifactId to reference both the `ServiceAccount` and `Deployment` names. 


##### Create Secrets

In order to pass sensitive values to the application we have to use Secrets
First create a Service based on an external API call requiring a API token to be used:

```java
@Service
public class QuoteService {

    private static final String DEFAULT_QUOTE = "Addhinucchiuni, cugghiennu cuttuni, essennu cu tia, cuttuni cugghia.";
    private static final String QUOTE_API_URI = "https://quotes15.p.rapidapi.com/quotes/random/";
    private static final String API_HOST_HEADER = "x-rapidapi-host";
    private static final String API_HOST = "quotes15.p.rapidapi.com";
    private static final String USE_QUERY_STRING_HEADER = "useQueryString";
    private static final String USE_QUERY_STRING = "true";
    private static final String API_KEY_HEADER = "x-rapidapi-key";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();


    @Value("${QUOTE_RAPIDAPI_KEY}")
    private String rapidApiKey;

    public String getRandomQuote(String languageCode) {
        //
    }


    private HttpRequest createRequest(String languageCode) {
        return HttpRequest.newBuilder().uri(URI.create(String.format("%s%s", QUOTE_API_URI, determineQueryString(languageCode))))
                .header(API_HOST_HEADER, API_HOST)
                .headers(USE_QUERY_STRING_HEADER, USE_QUERY_STRING)
                .header(API_KEY_HEADER, rapidApiKey)
                .GET()
                .build();
    }
    
}
```
As usual the first option is to pass the value in the `application.properties` file
```properties
QUOTE_RAPIDAPI_KEY=379d0a3427msh63c49ea08578814p181400jsn2925e4b31883
```

Now we can add the `/src/main/jkube/api-secret.yml` file
```yaml
metadata:
  name: apikeysecret
type: Opaque
data:
  quoteRapidApiToken: Mzc5ZDBhMzQyN21zaDYzYzQ5ZWEwODU3ODgxNHAxODE0MDBqc24yOTI1ZTRiMzE4ODM=
```

**IMPORTANT:** remember the value of the secret must be Base64 encoded for type _Opaque_

Now we need to add a reference to the secret in the deployment file

```yaml
containers:
        - volumeMounts:
            - name: config
              mountPath: /deployments/config
          env:
            - name: QUOTE_RAPIDAPI_KEY
              valueFrom:
                secretKeyRef:
                  name: apikeysecret
                  key: quoteRapidApiToken
```


https://medium.com/callibrity/spring-into-kubernetes-d816d65e2dbc
https://medium.com/twodigits/dynamic-app-configuration-inject-configuration-at-run-time-using-spring-boot-and-docker-ffb42631852a
https://capgemini.github.io/engineering/externalising-spring-boot-config-with-kubernetes/
https://github.com/eclipse/jkube/tree/master/quickstarts/maven/external-resources
https://itnext.io/building-and-deploying-a-weather-web-application-onto-kubernetes-red-hat-openshift-using-eclipse-62bf7c924be4