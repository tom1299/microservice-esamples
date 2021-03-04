/**
 * 
 */
package com.jeeatwork.ms.podlister;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

@Slf4j
public class KafkaUtils {
	
	private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	public static Serde<String> keySerde = Serdes.String();

	private KafkaUtils() {
		// Hide visibility
	}

	public static <T> byte[] serialize(String topic, T data) {
		try {
			return OBJECT_MAPPER.writeValueAsBytes(data);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T deserialize(String topic, byte[] data, Class<T> clazz) {
		try {
			return OBJECT_MAPPER.readValue(data, clazz);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Properties addBootstrapServer(Properties props) {
		String brokerHost = System.getenv("BROKER_HOST");
		if (brokerHost == null || brokerHost.isEmpty()) {
			brokerHost = "172.18.0.3";
		}
		String brokerPort = System.getenv("BROKER_PORT");
		if (brokerPort == null || brokerPort.isEmpty()) {
			brokerPort = "30101";
		}
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerHost + ":" + brokerPort);
		return props;
	}

	public static Properties createProducerProps(Object usingObject) {
		Properties props = KafkaUtils.addBootstrapServer(new Properties());
		props.put(ProducerConfig.CLIENT_ID_CONFIG, usingObject.getClass().getSimpleName());
		addSerdesProps(props);
		log.info("Created kafka producer properties are %s", props);
		return props;
	}

	private static void addSerdesProps(Properties props) {
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
	}

	public static Properties createConsumerProps(String groupIdConfig) {
		Properties props = KafkaUtils.addBootstrapServer(new Properties());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdConfig);
		log.info("Created kafka producer properties are %s", props);
		return props;
	}
}
