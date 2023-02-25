package com.dstorage.proto;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class App {

    public static void main(String[] args) throws Exception {
        
        // Configuration for connecting to JuiceFS using Hadoop Compatible SDK
        Configuration conf = new Configuration();
        conf.set("fs.jfs.impl", "io.juicefs.JuiceFileSystem");
        conf.set("juicefs.meta", "redis://127.0.0.1:6379/1"); // JuiceFS metadata engine URL
        Path p = new Path("jfs://myjfs/myjfs");
        FileSystem jfs = p.getFileSystem(conf);

        // Configuration for Kafka Consumer
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "kafka";
        List<String> topics = new ArrayList<String>();
        String topicMqtt = "mqtt.echo";
        String topicAck = "ack";
        topics.add(topicMqtt);
        topics.add(topicAck);

        // create consumer configs
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        // get a reference to the current thread
        final Thread mainThread = Thread.currentThread();


        // adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                consumer.wakeup();

                // join the main thread to allow the execution of the code in the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        try {

            // subscribe consumer to our topic(s)
            consumer.subscribe(topics);

            // poll for new data
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    String recordTopic = record.topic();

                    if (recordTopic.equals(topicMqtt)) {
                        LocalDate currentDate = LocalDate.now();
                        String datePath = currentDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd/"));
                        UUID filename = UUID.randomUUID();
                        Path filePath = new Path(datePath + filename.toString());

                        FSDataOutputStream outputStream = jfs.create(filePath);
                        outputStream.writeChars(record.value());
                        outputStream.close();

                        produce(filename.toString());
                    }

                    if (recordTopic.equals(topicAck)) {

                        logToFile(record.value());
                    }

                }

            }

        } catch (WakeupException e) {
            // we ignore this as this is an expected exception when closing a consumer
        } catch (Exception e) {
            System.out.println("Unexpected exception " + e.toString());
        } finally {
            consumer.close(); // this will also commit the offsets if need be.
            jfs.close();

        }

    }

    public static void produce(String message) {
        String bootstrapServers = "127.0.0.1:9092";
        Properties propertiesProd = new Properties();
        propertiesProd.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        propertiesProd.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        propertiesProd.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create the producer
        try {
            KafkaProducer<String, String> producer = new KafkaProducer<>(propertiesProd);

            // create a producer record
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>("ack", message);

            // send data - asynchronous
            producer.send(producerRecord);

            // flush data - synchronous
            producer.flush();
            // flush and close producer
            producer.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }

    public static void logToFile(String message) {

        String projectPath = Paths.get("").toAbsolutePath().toString();
        String path = projectPath + "/output.txt";

        try {
            FileWriter writer = new FileWriter(path, true);
            BufferedWriter logWriter = new BufferedWriter(writer);

            logWriter.append(message + "\n");
            logWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
