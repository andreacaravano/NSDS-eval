package it.polimi.nsds.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PopularTopicConsumer {
    private static final String topic = "inputTopic";
    private static final String serverAddr = "localhost:9092";

    public static void main(String[] args) {
        // By default, groupB is used (must differ from AtMostOncePrinter group)
        String groupId = args.length >= 1 ? args[0] : "groupB";

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());

        // To provice at most once semantics, we make sure that read values are committed
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(true));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        Map<String, Integer> keyToCounter = new HashMap<>();

        while (true) {
            final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));
            for (final ConsumerRecord<String, Integer> record : records) {
                keyToCounter.put(record.key(),
                        keyToCounter.containsKey(record.key()) ?
                                keyToCounter.get(record.key()) + 1 : 1);

                int max = keyToCounter.values().stream().reduce(0, Integer::max);

                System.out.println("Most popular keys:");
                for (Map.Entry<String, Integer> e : keyToCounter.entrySet()) {
                    if (e.getValue() == max) {
                        System.out.format("Key: %s Value: %s%s", e.getKey(), e.getValue(), System.lineSeparator());
                    }
                }
            }
        }
    }
}
