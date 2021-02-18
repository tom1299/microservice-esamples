package com.jeeatwork.ms.podlister;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

public class K8sClientTest {

    @Test
    @EnabledIfSystemProperty(named = "e2eTest", matches = "true")
    public void testListPodsInNamespace() {
        String clusterUrl = System.getProperty("k8s.cluster.url");
        String clusterCaFile = System.getProperty("k8s.cluster.ca.file");
        String clientKeyFile = System.getProperty("k8s.cluster.client.key.file");
        String clientCertFile = System.getProperty("k8s.cluster.client.cert.file");
        String namespace = System.getProperty("k8s.cluster.namespace");
        Config config = new ConfigBuilder().withMasterUrl(clusterUrl)
                .withClientCertFile(clientCertFile)
                .withClientKeyFile(clientKeyFile)
                .withCaCertFile(clusterCaFile)
                .build();
        Config.configFromSysPropsOrEnvVars(config);
        KubernetesClient client = new DefaultKubernetesClient(config);
        client.pods().inNamespace(namespace).list().getItems().forEach(
                pod -> System.out.println(pod.getMetadata().getName()));
    }
}
