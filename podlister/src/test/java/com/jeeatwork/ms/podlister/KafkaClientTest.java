package com.jeeatwork.ms.podlister;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

public class KafkaClientTest {

    @Test
    @EnabledIfSystemProperty(named = "e2eTest", matches = "true")
    public void testProduceAndConsume() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String topicName = "cluster-test";
        KafkaProducer kafkaProducer = new KafkaProducer<>(KafkaUtils.createProducerProps(this));

        final ProducerRecord<Long, String> record = new ProducerRecord(topicName, uuid,
                        "Cluster-Java-Test");

        long time = System.currentTimeMillis();
        RecordMetadata metadata = (RecordMetadata) kafkaProducer.send(record).get();

        long elapsedTime = System.currentTimeMillis() - time;
        System.out.printf("sent record(key=%s value=%s) " +
                        "meta(partition=%d, offset=%d) time=%d\n",
                record.key(), record.value(), metadata.partition(),
                metadata.offset(), elapsedTime);

        kafkaProducer.flush();
        kafkaProducer.close();

        Consumer<String, String> consumer = new KafkaConsumer<>(KafkaUtils.createConsumerProps(this.getClass().getSimpleName()));
        consumer.subscribe(Collections.singleton(topicName));

        boolean found = false;
        for (int i = 0; i < 100; i++) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> r : records) {
                if (r.key() != null && r.key().equals(uuid)) {
                    found = true;
                }
            }
            consumer.commitAsync();
            if (found) break;
        }

        assert found;

        consumer.close();
    }
}
