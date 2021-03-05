package com.jeeatwork.ms.podlister;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class PodListerConsumer {

    public static void main(String[] args) {
        final Thread mainThread = Thread.currentThread();
        Properties consumerProps = KafkaUtils.createConsumerProps(PodListerConsumer.class.getSimpleName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        final String topic = "cluster-test";
        final Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        final List<TopicPartition> partitions = consumer
                .partitionsFor(topic)
                .stream()
                .map(pInfo -> new TopicPartition(topic, pInfo.partition()))
                .collect(Collectors.toList());


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Starting shutdown");
            consumer.wakeup();
            try {
                mainThread.join();
            }
            catch (InterruptedException e) {
                log.info("Shutdown hook thread got interrupted", e);
            }
        }));

        consumer.assign(partitions);
        consumer.seekToEnd(partitions);

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    log.info("Found pod list {}, {}", r.key(), r.value());
                }
            }
        }
        catch (WakeupException e) {
            log.info("WakeupException for consumer", e);
        }
        finally {
            consumer.close();
            log.info("Consumer successfully closed");
        }
    }
}
