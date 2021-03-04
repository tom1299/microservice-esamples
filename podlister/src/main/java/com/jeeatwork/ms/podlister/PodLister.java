package com.jeeatwork.ms.podlister;

import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.UUID;

@Slf4j
@Setter
public class PodLister {

    private KafkaProducer kafkaProducer;

    private KubernetesClient kubernetesClient;

    private String topicName = "cluster-test";

    public void publishPodList() {
        log.info("Starting to collect pods in namespace {}", kubernetesClient.getNamespace());

        String uuid = UUID.randomUUID().toString();

        PodList podList = new PodList();

        try {
            podList.setNamespace(kubernetesClient.getNamespace());
            kubernetesClient.pods().list().getItems().forEach(pod -> podList.getNames().add(pod.getMetadata().getName()));

            log.info("Collected pod list {}", podList);

            ProducerRecord<Long, String> record = new ProducerRecord(topicName, uuid, podList.toString());
            kafkaProducer.send(record);
        }
        catch (Exception e) {
            log.error("An error occurred while trying to list the pods", e);
        }
    }
}
