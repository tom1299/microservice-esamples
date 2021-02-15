package com.jeeatwork.ms.podlister;

import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;

@Slf4j
@Setter
public class PodLister {

    private KafkaProducer kafkaProducer;

    private KubernetesClient kubernetesClient;

    public void publishPodList() {
        PodList podList = new PodList();
        podList.setNamespace(kubernetesClient.getNamespace());
        kubernetesClient.pods().list().getItems().forEach(pod -> podList.getNames().add(pod.getMetadata().getName()));
    }
}
