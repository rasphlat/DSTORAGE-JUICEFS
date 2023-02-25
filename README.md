# #DSTORAGE-JUICEFS

This project aims to create a data pipeline for processing and analyzing IoT data from different devices. The pipeline consists of various components including data ingestion, storage, processing, and visualization. The pipeline uses Kafka, JuiceFS, and Spark to achieve scalability, fault-tolerance, and high-performance processing.

# Architecture
<img src="https://media.giphy.com/media/hCAndh3LRpdH5CqHHh/giphy.gif" width=400px></img>

The architecture of the project consists of the following components:

## IoT Device Simulator
  The IoT Device Simulator is used to simulate IoT devices that send JSON messages to the Kafka broker. The IoT Simulator is accessible in this repository:
  https://github.com/massimocallisto/iot-simulator

## Kafka
 <img width="200" src="https://upload.wikimedia.org/wikipedia/commons/5/53/Apache_kafka_wordtype.svg"> 
The Kafka broker receives the JSON messages from the IoT devices and stores them in topics.
It is assemed that Kafka is already running and listening on port 9092.
Set also in console the following variable.

    KAFKA_HOME=/opt/kafka

Download the MQTT Source connector

Unzip and copy the content in 
 
    $KAFKA_HOME/plugins/mqtt-connector

In the configuration file 

    $KAFKA_HOME/config/connect-distributed.properties add the following line plugin.path=/opt/kafka/plugins

Start the connector with the following comand 

    $KAFKA_HOME/bin/connect-distributed.sh $KAFKA_HOME/config/connect-distributed.properties

To run the connector we have to define a configuration as JSON file to submit to the worker connector. Save it as `~/mqtt_connect.json`

```
{
  "name": "mqtt-source",
  "config": {
    "connector.class": "io.confluent.connect.mqtt.MqttSourceConnector",
    "tasks.max": "1",
    "mqtt.server.uri": "tcp://localhost:1883",
    "mqtt.topics": "#",
    "kafka.topic": "mqtt.echo",
    "value.converter":"org.apache.kafka.connect.converters.ByteArrayConverter",
    "key.converter":"org.apache.kafka.connect.storage.StringConverter",
    "key.converter.schemas.enable" : "false",
    "value.converter.schemas.enable" : "false",
    "confluent.topic.bootstrap.servers": "localhost:9092",
    "confluent.topic.replication.factor": "1",
    "confluent.license": ""
  }
}

```

Then submit to the worker:

    curl -s -X POST -H 'Content-Type: application/json' http://localhost:8083/connectors -d ~/mqtt_connect.json

## Persistor Prototype
  The Persistor Prototype is composed of a producer and consumer that are connected to Kafka. The producer sends an ack with the UUID of the iot json file  received and the consumer stores the IoT json messages received from kafka in JuiceFS or consume the messages from the ack topic and use another method of the persistor prototype to create a log file after the ack with the uuid of each files.
  
## JuiceFS
 <img width="200" src="https://user-images.githubusercontent.com/56829605/221372535-38afe501-86ed-44e9-bf9f-a39797ac4832.svg"> 


The JuiceFS is a distributed file system that provides consistent and scalable storage for the IoT data. The consumer stores the IoT json messages received from kafka in JuiceFS.

 To configure JuiceFS, you will need to install the client software on each node in your cluster. The client software can be downloaded from the Juice Technologies website or from GitHub. Once installed, you will need to configure the client with the following commands:

 - `juicefs format --storage minio --bucket http://127.0.0.1:9000/myjfs --access-key minioadmin --secret-key minioadmin "redis://127.0.0.1:6379/1" myjfs` - It is assumed that Redis and Minio are installed and running. 
Redis must run on the port 6379.
Minio must run on the port 9000.

 - `juicefs config set <option> <value>` - This command sets a configuration option in JuiceFS. Options include storage backend type (e.g., S3), authentication method, and encryption settings.

 - `sudo juicefs mount "redis://127.0.0.1:6379/1" /mnt/jfs` - This command mounts a directory in your filesystem as a mountpoint in JuiceFS. You can specify the mountpoint path or use an existing directory in your filesystem as the mountpoint path.

 - `juicefs unmount <mountpoint>` - This command unmounts a directory from your filesystem that was previously mounted as a mountpoint in JuiceFS.

## Spark
 <img width="200" src="https://upload.wikimedia.org/wikipedia/commons/f/f3/Apache_Spark_logo.svg"> 

Spark is used for processing and analyzing the IoT data stored in JuiceFS. Spark's processing capability is enhanced by the use of Scala queries to analyze the data. A possible query is the following one:
- `val df = spark.read.json("file:///mnt/jfs/user/thinkpad/yyyy/mm/dd/")` - where yyyy/mm/dd is the date to be analized
- `df.show()` - to show all the content in that folder


# Get Started

- Clone the repository to your local machine.
- Install and configure Kafka on your local machine.
- Install JuiceFS and configure it to work with Kafka.
- Install and configure Spark on your local machine.
- Run the Persistor Prototype, the Spark job, and the visualization tool of your choice.

# Usage

To use the Project, follow the steps below:

- Connect the IoT devices to the Kafka broker and start sending JSON messages.
- The Kafka broker will store the messages in topics.
- The Persistor Prototype will consume the messages from the topic and store them in JuiceFS or consume the messages from the ack topic and use another method of the persistor - prototype to create a log file after the ack with the uuid of each files.
- The Spark job will read the data from JuiceFS and perform the required processing and analysis.
- The processed data can be visualized using various visualization tools.

# Contributing

If you want to contribute to this project, follow the steps below:

- Fork the repository to your GitHub account.
- Clone the forked repository to your local machine.
- Create a new branch and make the changes.
- Commit the changes and push them to your forked repository.
- Submit a pull request to the original repository.

# Acknowledgments

The project was developed as part of the course "Technologies for Big Data Management" at the University of Camerino.

 ## Creators <a name = "creators" > </a>
 
* [Daniele Porumboiu](https://github.com/rasphlat)
* [Vlad Dogariu](https://github.com/thevladdo)
* [Mounir Taouafe](https://github.com/muno1)


