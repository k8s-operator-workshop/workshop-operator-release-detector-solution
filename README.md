# workshop-operator-release-detector
 Empty repository to start the workshop 

## GitPod integration

Before open the project in Gitpod make sure to have set your `K8S_CTK` variable with the base 64 encoded version of your Kubernetes config file (`config-k3s00X`) file: `cat config-k3s00X | base64`.

To open the workspace, simply click on the *Open in Gitpod* button, or use [this link](https://gitpod.io/#https://github.com/k8s-operator-workshop/workshop-operator-release-detector).

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/k8s-operator-workshop/workshop-operator-release-detector)


# Workshop steps

ℹ️ The complete source of the workshop can be found in the [workshop-operator-release-detector-solution](https://github.com/k8s-operator-workshop/workshop-operator-release-detector-solution) repository ℹ️


### Init the project

  - create the project using the operator-sdk CLI: `operator-sdk init --plugins quarkus --domain operator.workshop.com --project-name workshop-operator-release-detector`
  - the following tree structure must be created:
```bash
.
├── LICENSE
├── Makefile
├── pom.xml
├── PROJECT
├── README.md
└── src
    └── main
        ├── java
        └── resources
            └── application.properties
```
  - change the JOSDK Quarkus extension version and the Quarkus version in the `pom.xml`:
```xml
<quarkus-sdk.version>4.0.1</quarkus-sdk.version>
<quarkus.version>2.12.2.Final</quarkus.version>
```
  remove the following dependency in the `pom.xml`:
```xml
quarkus-operator-sdk-csv-generator
```
  - add these dependencies in `pom.xml` for k3s compatibility:
```xml
    <!-- Mandatory for k3s : see https://github.com/fabric8io/kubernetes-client/issues/1796 -->
    <dependency>
      <groupId>org.bouncycastle</groupId>
      <artifactId>bcprov-ext-jdk15on</artifactId>
      <version>1.69</version>
    </dependency>
    <dependency>
      <groupId>org.bouncycastle</groupId>
      <artifactId>bcpkix-jdk15on</artifactId>
      <version>1.69</version>
    </dependency>
```
  - test the compilation: `mvn clean compile`
  - launch Quarkus in _dev mode_: `mvn quarkus:dev`:
```bash
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2022-09-16 13:25:54,920 WARN  [io.qua.config] (Quarkus Main Thread) Unrecognized configuration key "quarkus.operator-sdk.generate-csv" was provided; it will be ignored; verify that the dependency extension for this configuration is set or that you did not make a typo

2022-09-16 13:25:55,806 INFO  [io.qua.ope.run.OperatorProducer] (Quarkus Main Thread) Quarkus Java Operator SDK extension 4.0.1 (commit: 0a2f95e on branch: 0a2f95e4e591da2f562d7be0ee2039c6f83f3b47) built on Tue Sep 13 19:35:36 UTC 2022
2022-09-16 13:25:55,811 WARN  [io.qua.ope.run.AppEventListener] (Quarkus Main Thread) No Reconciler implementation was found so the Operator was not started.
2022-09-16 13:25:55,885 INFO  [io.quarkus] (Quarkus Main Thread) workshop-operator-release-detector 0.0.1-SNAPSHOT on JVM (powered by Quarkus 2.12.2.Final) started in 4.134s. Listening on: http://localhost:8080
2022-09-16 13:25:55,886 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2022-09-16 13:25:55,886 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, kubernetes, kubernetes-client, micrometer, openshift-client, operator-sdk, smallrye-context-propagation, smallrye-health, vertx]

--
Tests paused
Press [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>
```

### CRD generation
  - execute the following command: `operator-sdk create api --version v1 --kind ReleaseDetector`
  - check that the 4th classes had been generated:
```bash
src
│   └── main
│       ├── java
│       │   └── com
│       │       └── workshop
│       │           └── operator
│       │               └── <username>
│       │                   ├── ReleaseDetector.java
│       │                   ├── ReleaseDetectorReconciler.java
│       │                   ├── ReleaseDetectorSpec.java
│       │                   └── ReleaseDetectorStatus.java
│       └── resources
│           └── application.properties
```
  - check the generated CRD in `./target/kubernetes/releasedetectors.operator.workshop.com-v1.yml`, for example `./target/kubernetes/releasedetectors.wilda.operator.workshop.com-v1.yml`
  - check that the CRD is generated in the Kubernetes' cluster: `kubectl get crds releasedetectors.operator.workshop.com`
```bash
$ kubectl get crds releasedetectors.operator.workshop.com

NAME                                           CREATED AT
releasedetectors.operator.workshop.com   2022-09-16T13:31:23Z
```

### Release detection

> ⚠️ At this point you must have executed the exercice [workshop-operator-hello-world](https://github.com/k8s-operator-workshop/workshop-operator-hello-world) ⚠️

  - add the following dependencies in the pom.xml:
```xml
<!-- To call GH API-->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-rest-client</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-rest-client-jackson</artifactId>
</dependency>
```
  - create the class `GitHubRelease.java` in `src/main/java/com/workshop/operator/`:
```java
public class GitHubRelease {

  /**
   * ID of the response
   */
  private long responseId;

  /**
   * Release tag name.
   */
  @JsonProperty("tag_name")
  private String tagName;

  public String getTagName() {
    return tagName;
  }

  public void setTagName(String tagName) {
    this.tagName = tagName;
  }
}
```
  - create the interface `GHService.java` in `src/main/java/com/workshop/operator/util`:
```java
@Path("/repos")
@RegisterRestClient
public interface GHService {

    @GET
    @Path("/{owner}/{repo}/releases/latest")
    GitHubRelease getByOrgaAndRepo(@PathParam(value = "owner") String owner, @PathParam(value = "repo") String repo);
}
```
  - update the `application.properties` file as following:
```properties
quarkus.container-image.build=true
#quarkus.container-image.group=
quarkus.container-image.name=workshop-operator-release-detector-operator
# set to true to automatically apply CRDs to the cluster when they get regenerated
quarkus.operator-sdk.crd.apply=false
# set to true to automatically generate CSV from your code
quarkus.operator-sdk.generate-csv=false
# GH Service parameter
quarkus.rest-client."com.workshop.operator.util.GHService".url=https://api.github.com 
quarkus.rest-client."com.workshop.operator.util.GHService".scope=javax.inject.Singleton 
```
  - update the class `ReleaseDetectorSpec.java` as following:
```java
public class ReleaseDetectorSpec {

    /**
     * Name of the organisation (or owner) where find the repository
     */
    private String organisation;
    /**
     * The repository name.
     */
    private String repository;

    
    public String getOrganisation() {
        return organisation;
    }
    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }
    public String getRepository() {
        return repository;
    }
    public void setRepository(String repository) {
        this.repository = repository;
    }
}
```
  - update the `ReleaseDetectorStatus.java` class as following:
```java
public class ReleaseDetectorStatus {

    /**
     * Last release version deployed on the cluster.
     */
    private String deployedRelase;

    public String getDeployedRelase() {
        return deployedRelase;
    }

    public void setDeployedRelase(String deployedRelase) {
        this.deployedRelase = deployedRelase;
    }
}
```
  - update the `ReleaseDetectorReconciler.java` class as following:
```java
public class ReleaseDetectorReconciler implements Reconciler<ReleaseDetector>,
    Cleaner<ReleaseDetector>, EventSourceInitializer<ReleaseDetector> {
  private static final Logger log = LoggerFactory.getLogger(ReleaseDetectorReconciler.class);

  /**
   * Name of the repository to check.
   */
  private String repoName;
  /**
   * GitHub organisation name that contains the repository.
   */
  private String organisationName;
  /**
   * ID of the created custom resource.
   */
  private ResourceID resourceID;
  /**
   * Current deployed release.
   */
  private String currentRelease;
  /**
   * Fabric0 kubernetes client.
   */
  private final KubernetesClient client;

  @Inject
  @RestClient
  private GHService ghService;

  public ReleaseDetectorReconciler(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Map<String, EventSource> prepareEventSources(EventSourceContext<ReleaseDetector> context) {
    var poolingEventSource = new PollingEventSource<String, ReleaseDetector>(() -> {
      log.info("⚡️ Polling data !");
      if (resourceID != null) {
        log.info("🚀 Fetch resources !");
        log.info("🐙 Get the last release version of repository {} in organisation {}.",
            organisationName, repoName);
        GitHubRelease gitHubRelease = ghService.getByOrgaAndRepo(organisationName, repoName);
        log.info("🏷  Last release is {}", gitHubRelease.getTagName());
        currentRelease = gitHubRelease.getTagName();
        return Map.of(resourceID, Set.of(currentRelease));
      } else {
        log.info("🚫 No resource created, nothing to do.");
        return Map.of();
      }
    }, 30000, String.class);

    return EventSourceInitializer.nameEventSources(poolingEventSource);
  }

  @Override
  public UpdateControl<ReleaseDetector> reconcile(ReleaseDetector resource, Context context) {
    log.info("⚡️ Event occurs ! Reconcile called.");

    // Get configuration
    resourceID = ResourceID.fromResource(resource);
    repoName = resource.getSpec().getRepository();
    organisationName = resource.getSpec().getOrganisation();
    log.info("⚙️ Configuration values : repository = {}, organisation = {}.", repoName,
        organisationName);

    // Update the status
    if (resource.getStatus() != null) {
      resource.getStatus().setDeployedRelase(currentRelease);
    } else {
      resource.setStatus(new ReleaseDetectorStatus());
    }

    return UpdateControl.noUpdate();
  }

  @Override
  public DeleteControl cleanup(ReleaseDetector resource, Context<ReleaseDetector> context) {
    log.info("🗑 Undeploy the application");

    resourceID = null;

    return DeleteControl.defaultDelete();
  }
}
```
  - at this point you should see in the operator logs:
```bash
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Quarkus Main Thread) ⚡️ Polling data !
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Quarkus Main Thread) 🚫 No resource created, nothing to do.
```
- create a test CR `src/test/resources/cr-test-gh-release-watch.yml`:
```yaml
apiVersion: "operator.workshop.com/v1"
kind: ReleaseDetector
metadata:
  name: check-quarkus
spec:
  organisation: k8s-operator-workshop
  repository: hello-world-from-quarkus-workshop-solution
```
  - create the namespace `test-operator-release-detector`: `kubectl create ns test-operator-release-detector`
  - create the test CR on the cluster: `kubectl apply -f ./src/test/resources/cr-test-gh-release-watch.yml -n test-operator-release-detector`
  - in the operator logs you should see:
```bash
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-9) 🚫 No resource created, nothing to do.
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (EventHandler-releasedetectorreconciler) ⚡️ Event occurs ! Reconcile called.
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (EventHandler-releasedetectorreconciler) ⚙️ Configuration values : repository = hello-world-from-quarkus-workshop, organisation = philippart-s.
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-9) ⚡️ Polling data !
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-9) 🚀 Fetch resources !
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-9) 🐙 Get the last release version of repository philippart-s in organisation hello-world-from-quarkus-workshop.
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-9) 🏷  Last release is 1.0.0
```
  - delete the CR: `kubectl delete releasedetectors.operator.workshop.com check-quarkus -n test-operator-release-detector`
  - in the operator logs you should see:
```bash
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (EventHandler-releasedetectorreconciler) 🗑 Undeploy the application
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-9) ⚡️ Polling data !
INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-9) 🚫 No resource created, nothing to do.
```

## Deploy application

  - update the class `ReleaseDetectorReconciler.java`:
```java
public class ReleaseDetectorReconciler implements Reconciler<ReleaseDetector>,
    Cleaner<ReleaseDetector>, EventSourceInitializer<ReleaseDetector> {
  private static final Logger log = LoggerFactory.getLogger(ReleaseDetectorReconciler.class);

  /**
   * Name of the repository to check.
   */
  private String repoName;
  /**
   * GitHub organisation name that contains the repository.
   */
  private String organisationName;
  /**
   * ID of the created custom resource.
   */
  private ResourceID resourceID;
  /**
   * Current deployed release.
   */
  private String currentRelease;
  /**
   * Fabric0 kubernetes client.
   */
  private final KubernetesClient client;

  @Inject
  @RestClient
  private GHService ghService;

  public ReleaseDetectorReconciler(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Map<String, EventSource> prepareEventSources(EventSourceContext<ReleaseDetector> context) {
    var poolingEventSource = new PollingEventSource<String, ReleaseDetector>(() -> {
      log.info("⚡️ Polling data !");
      if (resourceID != null) {
        log.info("🚀 Fetch resources !");
        log.info("🐙 Get the last release version of repository {} in organisation {}.",
            organisationName, repoName);
        GitHubRelease gitHubRelease = ghService.getByOrgaAndRepo(organisationName, repoName);
        log.info("🏷  Last release is {}", gitHubRelease.getTagName());
        currentRelease = gitHubRelease.getTagName();
        return Map.of(resourceID, Set.of(currentRelease));
      } else {
        log.info("🚫 No resource created, nothing to do.");
        return Map.of();
      }
    }, 30000, String.class);

    return EventSourceInitializer.nameEventSources(poolingEventSource);
  }

  @Override
  public UpdateControl<ReleaseDetector> reconcile(ReleaseDetector resource, Context context) {
    log.info("⚡️ Event occurs ! Reconcile called.");

    String namespace = resource.getMetadata().getNamespace();

    // Get configuration
    resourceID = ResourceID.fromResource(resource);
    repoName = resource.getSpec().getRepository();
    organisationName = resource.getSpec().getOrganisation();
    log.info("⚙️ Configuration values : repository = {}, organisation = {}.", repoName,
        organisationName);

    if (currentRelease != null && currentRelease.trim().length() != 0) {
      // Deploy appllication
      log.info("🔀 Deploy the new release {} !", currentRelease);
      Deployment deployment = makeDeployment(currentRelease, resource);
      client.apps().deployments().inNamespace(namespace).createOrReplace(deployment);

      // Create service
      Service service = makeService(resource);
      Service existingService = client.services().inNamespace(resource.getMetadata().getNamespace())
          .withName(service.getMetadata().getName()).get();
      if (existingService == null) {
        client.services().inNamespace(namespace).createOrReplace(service);
      }

      // Update the status
      if (resource.getStatus() != null) {
        resource.getStatus().setDeployedRelase(currentRelease);
      } else {
        resource.setStatus(new ReleaseDetectorStatus());
      }
    }

    return UpdateControl.noUpdate();
  }

  @Override
  public DeleteControl cleanup(ReleaseDetector resource, Context<ReleaseDetector> context) {
    log.info("🗑 Undeploy the application");

    resourceID = null;

    return DeleteControl.defaultDelete();
  }

  /**
   * Generate the Kubernetes deployment resource.
   * @param currentRelease The release to deploy
   * @param releaseDetector The created custom resource
   * @return The created deployment
   */
  private Deployment makeDeployment(String currentRelease, ReleaseDetector releaseDetector) {
    Deployment deployment = new DeploymentBuilder()
    .withNewMetadata()
      .withName("quarkus-deployment")
      .addToLabels("app", "quarkus")
      .endMetadata()
    .withNewSpec()
      .withReplicas(1)
      .withNewSelector()
        .withMatchLabels(Map.of("app", "quarkus"))
      .endSelector()
      .withNewTemplate()
        .withNewMetadata()
          .addToLabels("app","quarkus")
        .endMetadata()
        .withNewSpec()
          .addNewContainer()
            .withName("quarkus")
            .withImage("56hkk1xk.gra7.container-registry.ovh.net/workshop/wilda/" + repoName + ":" + currentRelease)
            .addNewPort()
              .withContainerPort(80)
            .endPort()
          .endContainer()
        .endSpec()
      .endTemplate()
    .endSpec()
    .build();

    deployment.addOwnerReference(releaseDetector);

    try {
      log.info("Generated deployment {}", SerializationUtils.dumpAsYaml(deployment));
    } catch (JsonProcessingException e) {
      log.error("Unable to get YML");
      e.printStackTrace();
    }

    return deployment;
  }

  /**
   * Generate the Kubernetes service resource.
   * 
   * @param releaseDetector The custom resource
   * @return The service.
   */
  private Service makeService(ReleaseDetector releaseDetector) {
    Service service = new ServiceBuilder()
    .withNewMetadata()
      .withName("quarkus-service")
      .addToLabels("app", "quarkus")
    .endMetadata()
    .withNewSpec()
      .withType("NodePort")
      .withSelector(Map.of("app", "quarkus"))
      .addNewPort()
        .withPort(80)
        .withTargetPort(new IntOrString(8080))
        .withNodePort(30080)
      .endPort()
    .endSpec()
    .build();
    
    service.addOwnerReference(releaseDetector);

    try {
      log.info("Generated service {}", SerializationUtils.dumpAsYaml(service));
    } catch (JsonProcessingException e) {
      log.error("Unable to get YML");
      e.printStackTrace();
    }

    return service;
  }  
}
```
  - create the test CR on the cluster: `kubectl apply -f ./src/test/resources/cr-test-gh-release-watch.yml -n test-operator-release-detector`
  - in the operator logs you should see:
```bash2022-09-16 14:55:59,424 INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-17) ⚡️ Polling data !
2022-09-16 14:55:59,424 INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-17) 🚀 Fetch resources !
2022-09-16 14:55:59,424 INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-17) 🐙 Get the last release version of repository philippart-s in organisation hello-world-from-quarkus-workshop.
2022-09-16 14:55:59,648 INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (Timer-17) 🏷  Last release is 1.0.0
2022-09-16 14:55:59,649 INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (EventHandler-releasedetectorreconciler) ⚡️ Event occurs ! Reconcile called.
2022-09-16 14:55:59,649 INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (EventHandler-releasedetectorreconciler) ⚙️ Configuration values : repository = hello-world-from-quarkus-workshop, organisation = philippart-s.
2022-09-16 14:55:59,650 INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (EventHandler-releasedetectorreconciler) 🔀 Deploy the new release 1.0.0 !
2022-09-16 14:55:59,651 INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (EventHandler-releasedetectorreconciler) Generated deployment ---
apiVersion: "apps/v1"
kind: "Deployment"
metadata:
  labels:
    app: "quarkus"
  name: "quarkus-deployment"
  ownerReferences:
  - apiVersion: "wilda.operator.workshop.com/v1"
    kind: "ReleaseDetector"
    name: "check-quarkus"
    uid: "06072920-2485-4e09-8ca8-ec77b317052d"
spec:
  replicas: 1
  selector:
    matchLabels:
      app: "quarkus"
  template:
    metadata:
      labels:
        app: "quarkus"
    spec:
      containers:
      - image: "56hkk1xk.gra7.container-registry.ovh.net/workshop/wilda/hello-world-from-quarkus-workshop:1.0.0"
        name: "quarkus"
        ports:
        - containerPort: 80


2022-09-16 14:55:59,694 INFO  [com.wor.ope.wil.ReleaseDetectorReconciler] (EventHandler-releasedetectorreconciler) Generated service ---
apiVersion: "v1"
kind: "Service"
metadata:
  labels:
    app: "quarkus"
  name: "quarkus-service"
  ownerReferences:
  - apiVersion: "wilda.operator.workshop.com/v1"
    kind: "ReleaseDetector"
    name: "check-quarkus"
    uid: "06072920-2485-4e09-8ca8-ec77b317052d"
spec:
  ports:
  - nodePort: 30080
    port: 80
    targetPort: 8080
  selector:
    app: "quarkus"
  type: "NodePort"
```
  - check that the application is deployed: `kubectl get pods,svc -n test-operator-release-detector`:
```bash
kubectl get pods,svc -n wilda-workshop
NAME                                      READY   STATUS    RESTARTS   AGE
pod/quarkus-deployment-688d4f5fd5-7hvpp   1/1     Running   0          31s

NAME                      TYPE       CLUSTER-IP     EXTERNAL-IP   PORT(S)        AGE
service/quarkus-service   NodePort   X.X.X.X   <none>        80:30080/TCP   31s
```
  - test the application:
```bash
$ curl http://<cluster adress>:30080/hello

👋  Hello, World ! 🌍
```
  - delete the CR: `kubectl delete releasedetectors.operator.workshop.com check-quarkus -n test-operator-release-detector`,

## 🐳  Packaging & deployment to K8s
 - stop the Quarkus dev mode
 - update the `application.properties` file:
```properties
# Image options
quarkus.container-image.build=true
quarkus.container-image.group=56hkk1xk.gra7.container-registry.ovh.net/workshop/<username>
quarkus.container-image.name=workshop-operator-release-detector-operator
# set to true to automatically apply CRDs to the cluster when they get regenerated
quarkus.operator-sdk.crd.apply=false
# set to true to automatically generate CSV from your code
quarkus.operator-sdk.generate-csv=false
# GH Service parameter
quarkus.rest-client."com.workshop.operator.util.GHService".url=https://api.github.com 
quarkus.rest-client."com.workshop.operator.util.GHService".scope=javax.inject.Singleton 
# Kubernetes options
quarkus.kubernetes.namespace=workshop-operator-release-detector-operator
```
for example:
```properties
# Image options
quarkus.container-image.build=true
quarkus.container-image.group=56hkk1xk.gra7.container-registry.ovh.net/workshop/wilda
quarkus.container-image.name=workshop-operator-release-detector-operator
# set to true to automatically apply CRDs to the cluster when they get regenerated
quarkus.operator-sdk.crd.apply=false
# set to true to automatically generate CSV from your code
quarkus.operator-sdk.generate-csv=false
# GH Service parameter
quarkus.rest-client."com.workshop.operator.util.GHService".url=https://api.github.com 
quarkus.rest-client."com.workshop.operator.util.GHService".scope=javax.inject.Singleton 
# Kubernetes options
quarkus.kubernetes.namespace=workshop-operator-release-detector-operator
```
  - add `src/main/kubernetes/kubernetes.yml` file with the following content:
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
    name: service-deployment-cluster-role
    namespace: workshop-operator-release-detector-operator
rules:
  - apiGroups:
    - ""
    resources:
    - secrets
    - serviceaccounts
    - services  
    verbs:
    - "*"
  - apiGroups:
    - "apps"
    verbs:
        - "*"
    resources:
    - deployments
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: service-deployment-cluster-role-binding
  namespace: workshop-operator-release-detector-operator
roleRef:
  kind: ClusterRole
  apiGroup: rbac.authorization.k8s.io
  name: service-deployment-cluster-role
subjects:
  - kind: ServiceAccount
    name: workshop-operator-release-detector-operator
    namespace: workshop-operator-release-detector-operator
---
```
  - package the application: `mvn clean package`
  - connect the docker client to your registry `docker login <registry url>`, for example: `docker login 56hkk1xk.gra7.container-registry.ovh.net`
  - push the image previously created: `docker push <registry>/workshop/<username>/workshop-operator-release-detector-operator:0.0.1-SNAPSHOT`, for example `docker push 56hkk1xk.gra7.container-registry.ovh.net/workshop/wilda/workshop-operator-release-detector-operator:0.0.1-SNAPSHOT`
  - create the namespace `workshop-operator-release-detector-operator`: `kubectl create ns workshop-operator-release-detector-operator`
  - apply the manifest `./target/kubernetes/kubernetes.yml`: `kubectl apply -f ./target/kubernetes/kubernetes.yml` 
  - check everything is ok: `kubectl get pod -n workshop-operator-release-detector-operator`
```bash
$ kubectl get pod -n workshop-operator-release-detector-operator

NAME                                                           READY   STATUS    RESTARTS   AGE
workshop-operator-release-detector-operator-6cb48d9c75-2429b   1/1     Running   0          38s
```  
  - if needed create the namespace `test-operator-release-detector`: `kubectl create ns test-operator-release-detector`
  - test your operator by creating the test CR on the cluster: `kubectl apply -f ./src/test/resources/cr-test-gh-release-watch.yml -n test-operator-release-detector`
 - test the application:
```bash
$ curl http://<cluster adress>:30080/hello

👋  Hello, World ! 🌍
```
  - delete the CR: `kubectl delete releasedetectors.operator.workshop.com check-quarkus -n test-operator-release-detector`
  - delete the operator: `kubectl delete -f ./target/kubernetes/kubernetes.yml`
  - delete the crd : `kubectl delete crds/releasedetectors.operator.workshop.com`
  - delete the namespaces: `kubectl delete ns test-operator-release-detector workshop-operator-release-detector-operator`