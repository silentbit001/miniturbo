package sb001.miniturbo.vertex.k8s.service;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import sb001.miniturbo.vertex.k8s.service.dto.Status;

@Slf4j
public class K8sServiceV2 {

    private KubernetesClient client;
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public K8sServiceV2() {
        client = new DefaultKubernetesClient();
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
            return mapper.readValue(doc, Pod.class);
        } else if (isDeployment(doc)) {
            return mapper.readValue(doc, Deployment.class);
        } else if (isService(doc)) {
            return mapper.readValue(doc, Service.class);
        } else if (isStatefulSet(doc)) {
            return mapper.readValue(doc, StatefulSet.class);
        } else if (isConfigMap(doc)) {
            return mapper.readValue(doc, ConfigMap.class);
        }

        log.warn("Unknow document kind: '{}'", doc);
        return doc;

    }

    private void deploy(Object k8sDeployment) {

        if (k8sDeployment instanceof Pod) {
            client.pods().create((Pod) k8sDeployment);
        } else if (k8sDeployment instanceof Deployment) {
            client.apps().deployments().create((Deployment) k8sDeployment);
        } else if (k8sDeployment instanceof Service) {
            client.services().create((Service) k8sDeployment);
        } else if (k8sDeployment instanceof StatefulSet) {
            client.apps().statefulSets().create((StatefulSet) k8sDeployment);
        } else if (k8sDeployment instanceof ConfigMap) {
            client.configMaps().create((ConfigMap) k8sDeployment);
        } else {
            client.load(new ByteArrayInputStream(((String) k8sDeployment).getBytes())).createOrReplace();
        }

    }

    private void unDeploy(Object k8sDeployment) {

        if (k8sDeployment instanceof Pod) {
            client.pods().delete((Pod) k8sDeployment);
        } else if (k8sDeployment instanceof Deployment) {
            client.apps().deployments().delete((Deployment) k8sDeployment);
        } else if (k8sDeployment instanceof Service) {
            client.services().delete((Service) k8sDeployment);
        } else if (k8sDeployment instanceof StatefulSet) {
            client.apps().statefulSets().delete((StatefulSet) k8sDeployment);
        } else if (k8sDeployment instanceof ConfigMap) {
            client.configMaps().delete((ConfigMap) k8sDeployment);
        } else {
            client.load(new ByteArrayInputStream(((String) k8sDeployment).getBytes())).delete();
        }

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

        parseDocuments(yaml).stream().forEach(k8sObj -> {

            if (k8sObj instanceof Pod) {

                Pod pod = searchPodByName(((Pod) k8sObj).getMetadata().getName());
                log.debug("Pod: {}", pod);

            } else if (k8sObj instanceof Deployment) {

                Optional.ofNullable(searchDeploymentByName(((Deployment) k8sObj).getMetadata().getName()))
                        .ifPresent(deploy -> {
                            statusBuilder.ready(deploy.getStatus().getAvailableReplicas() >= 1);
                            statusBuilder
                                    .image(deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
                        });

            } else if (k8sObj instanceof Service) {

                Service service = (Service) k8sObj;
                Optional.ofNullable(searchServiceByName(service.getMetadata().getName())).ifPresent(remoteService -> {
                    statusBuilder.ports(remoteService.getSpec().getPorts().stream()
                            .collect(Collectors.toMap(p -> p.getPort(), p -> p.getNodePort())));

                });

            } else if (k8sObj instanceof StatefulSet) {

                Optional.ofNullable(searchStatefulSetByName(((StatefulSet) k8sObj).getMetadata().getName()))
                        .ifPresent(statefulSet -> {
                            statusBuilder.ready(statefulSet.getStatus().getReadyReplicas() >= 1);
                        });

            }

        });

        return statusBuilder.build();
    }

    private Pod searchPodByName(String name) {
        return client.pods().withName(name).get();
    }

    private Deployment searchDeploymentByName(String name) {
        return client.apps().deployments().withName(name).get();
    }

    private StatefulSet searchStatefulSetByName(String name) {
        return client.apps().statefulSets().withName(name).get();
    }

    private Service searchServiceByName(String name) {
        return client.services().withName(name).get();

    }

}
