/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.jeeatwork.ms.podlister;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class App {

    private App() {
        // Hidden visibility
    }

    private void run(String namespace) {
        final KubernetesClient client = new DefaultKubernetesClient().inNamespace(namespace);
        final KafkaProducer kafkaProducer = new KafkaProducer<>(KafkaUtils.createProducerProps(this));

        PodLister podLister = new PodLister();
        podLister.setKafkaProducer(kafkaProducer);
        podLister.setKubernetesClient(client);

        ScheduledExecutorService executorService = Executors
                .newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> podLister.publishPodList(),5, 30, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        log.info("Pod listener command line arguments {}", args);

        String namespace;
        if (args.length > 0) {
            namespace = args[0];
        }
        else {
            namespace = new String(Files.readAllBytes(Paths.get(
                    "/var/run/secrets/kubernetes.io/serviceaccount/namespace")));
        }

        log.info("Starting pod listener in namespace {}", namespace);
        App app = new App();
        app.run(namespace);
    }
}
