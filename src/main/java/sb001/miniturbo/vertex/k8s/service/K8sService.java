package sb001.miniturbo.vertex.k8s.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.AppsV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1ConfigMap;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1Deployment;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1StatefulSet;
import io.kubernetes.client.util.Config;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import sb001.miniturbo.vertex.k8s.service.dto.Status;

@Slf4j
public class K8sService {

    private static final int TIMEOUT = 1;

    private static final String PRETTY = "true";

    private CoreV1Api coreV1Api;
    private AppsV1Api appsV1Api;
    private String namespace;

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public K8sService() {
        this("default");
    }

    @SneakyThrows
    public K8sService(String namespace) {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        this.coreV1Api = new CoreV1Api();
        this.appsV1Api = new AppsV1Api();
        this.namespace = namespace;
    }

    public List<Object> parseDocuments(final String yaml) {
        return Arrays.asList(yaml.split("---")).stream().map(this::parseDocumentToK8sModel)
                .collect(Collectors.toList());
    }

    public void deployYamlDocuments(final String yaml) {
        log.debug("Deploying yaml: {}", yaml);
        Arrays.asList(yaml.split("---")).stream().map(this::parseDocumentToK8sModel).parallel().forEach(this::deploy);
    }

    public void unDeployYamlDocuments(final String yaml) {
        Arrays.asList(yaml.split("---")).stream().map(this::parseDocumentToK8sModel).parallel().forEach(this::unDeploy);
    }

    @SneakyThrows
    private Object parseDocumentToK8sModel(String doc) {
        if (isPod(doc)) {
            return mapper.readValue(doc, V1Pod.class);
        } else if (isDeployment(doc)) {
            return mapper.readValue(doc, V1Deployment.class);
        } else if (isService(doc)) {
            return mapper.readValue(doc, V1Service.class);
        } else if (isStatefulSet(doc)) {
            return mapper.readValue(doc, V1StatefulSet.class);
        } else if (isConfigMap(doc)) {
            return mapper.readValue(doc, V1ConfigMap.class);
        } else {
            log.warn("Unknow document kind: '{}'", doc);
            return null;
        }
    }

    @SneakyThrows
    private Boolean deploy(Object k8sDeployment) {

        if (k8sDeployment instanceof V1Pod) {
            coreV1Api.createNamespacedPod(namespace, (V1Pod) k8sDeployment, PRETTY);
        } else if (k8sDeployment instanceof V1Deployment) {
            appsV1Api.createNamespacedDeployment(namespace, (V1Deployment) k8sDeployment, PRETTY);
        } else if (k8sDeployment instanceof V1Service) {
            coreV1Api.createNamespacedService(namespace, (V1Service) k8sDeployment, PRETTY);
        } else if (k8sDeployment instanceof V1StatefulSet) {
            appsV1Api.createNamespacedStatefulSet(namespace, (V1StatefulSet) k8sDeployment, PRETTY);
        } else if (k8sDeployment instanceof V1ConfigMap) {
            coreV1Api.createNamespacedConfigMap(namespace, (V1ConfigMap) k8sDeployment, PRETTY);
        } else {
            log.warn(
                    "Deployment unknown. Expected: [V1Pod, V1ConfigMap, V1StatefulSet, V1Deployment, V1Service], but found: {}",
                    k8sDeployment);
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    @SneakyThrows
    private Boolean unDeploy(Object k8sDeployment) {

        V1DeleteOptions deleteOptions = new V1DeleteOptions();

        if (k8sDeployment instanceof V1Pod) {

            V1Pod pod = (V1Pod) k8sDeployment;
            coreV1Api.deleteNamespacedPod(pod.getMetadata().getName(), namespace, deleteOptions, PRETTY, null, null,
                    null);

        } else if (k8sDeployment instanceof V1Deployment) {

            V1Deployment deployment = (V1Deployment) k8sDeployment;
            appsV1Api.deleteNamespacedDeployment(deployment.getMetadata().getName(), namespace, deleteOptions, PRETTY,
                    null, null, null);

        } else if (k8sDeployment instanceof V1Service) {

            V1Service service = (V1Service) k8sDeployment;
            coreV1Api.deleteNamespacedService(service.getMetadata().getName(), namespace, deleteOptions, PRETTY, null,
                    null, null);

        } else if (k8sDeployment instanceof V1StatefulSet) {

            V1StatefulSet statefulSet = (V1StatefulSet) k8sDeployment;
            appsV1Api.deleteNamespacedStatefulSet(statefulSet.getMetadata().getName(), namespace, deleteOptions, PRETTY,
                    null, null, null);

        } else if (k8sDeployment instanceof V1ConfigMap) {

            V1ConfigMap configMap = (V1ConfigMap) k8sDeployment;
            coreV1Api.deleteNamespacedConfigMap(configMap.getMetadata().getName(), namespace, deleteOptions, PRETTY,
                    null, null, null);

        } else {

            log.warn(
                    "Deployment unknown. Expected: [V1Pod, V1ConfigMap, V1StatefulSet, V1Deployment, V1Service], but found: {}",
                    k8sDeployment);
            return Boolean.FALSE;

        }

        return Boolean.TRUE;

    }

    private boolean isService(String doc) {
        return StringUtils.contains(doc, "kind: Service");
    }

    private boolean isDeployment(String doc) {
        return StringUtils.contains(doc, "kind: Deployment");
    }

    private boolean isPod(String doc) {
        return StringUtils.contains(doc, "kind: Pod");
    }

    private boolean isStatefulSet(String doc) {
        return StringUtils.contains(doc, "kind: StatefulSet");
    }

    private boolean isConfigMap(String doc) {
        return StringUtils.contains(doc, "kind: ConfigMap");
    }

    public Status status(String yaml) {

        Status.StatusBuilder statusBuilder = Status.builder();
        statusBuilder.ready(Boolean.FALSE);

        parseDocuments(yaml).stream().forEach(k8sDeployment -> {

            if (k8sDeployment instanceof V1Pod) {

                Optional<V1Pod> pod = searchPodByName(((V1Pod) k8sDeployment).getMetadata().getName());
                log.debug("Pod: {}", pod);

            } else if (k8sDeployment instanceof V1Deployment) {

                searchDeploymentByName(((V1Deployment) k8sDeployment).getMetadata().getName()).ifPresent(deploy -> {
                    statusBuilder.ready(deploy.getStatus().getAvailableReplicas() >= 1);
                });

            } else if (k8sDeployment instanceof V1Service) {

                searchServiceByName(((V1Service) k8sDeployment).getMetadata().getName()).ifPresent(service -> {
                    statusBuilder.serviceExternalIPs(service.getSpec().getExternalIPs());
                });

            } else if (k8sDeployment instanceof V1StatefulSet) {

                searchStatefulSetByName(((V1StatefulSet) k8sDeployment).getMetadata().getName())
                        .ifPresent(statefulSet -> {
                            statusBuilder.ready(statefulSet.getStatus().getReadyReplicas() >= 1);
                        });

            }

        });

        return statusBuilder.build();
    }

    @SneakyThrows()
    private Optional<V1Pod> searchPodByName(String name) {
        return coreV1Api.listNamespacedPod(namespace, PRETTY, null, null, true, null, null, null, TIMEOUT, null)
                .getItems().stream().filter(p -> p.getMetadata().getName().equals(name)).findFirst();
    }

    @SneakyThrows()
    private Optional<V1Deployment> searchDeploymentByName(String name) {
        return appsV1Api.listNamespacedDeployment(namespace, PRETTY, null, null, true, null, null, null, 3000, null)
                .getItems().stream().filter(p -> p.getMetadata().getName().equals(name)).findFirst();
    }

    @SneakyThrows()
    private Optional<V1StatefulSet> searchStatefulSetByName(String name) {
        return appsV1Api.listNamespacedStatefulSet(namespace, PRETTY, null, null, true, null, null, null, 3000, null)
                .getItems().stream().filter(p -> p.getMetadata().getName().equals(name)).findFirst();
    }

    @SneakyThrows()
    private Optional<V1Service> searchServiceByName(String name) {
        return coreV1Api.listNamespacedService(namespace, PRETTY, null, null, true, null, null, null, 3000, null)
                .getItems().stream().filter(p -> p.getMetadata().getName().equals(name)).findFirst();
    }

}
