### Fs2-kafka KafkaConsumer leak on failure

To reproduce:
1. start-up kafka with `docker-compose up`
2. start app with `sbt run`
3. send message to Kafka 
    - either by `sh send_to_kafka.sh` (Requires [kafkacat](https://github.com/edenhill/kafkacat))
    - or by some other means. Kafka bootstrap server is located at `localhost:9092`
4. see logs. Should find `javax.management.InstanceAlreadyExistsException: kafka.consumer:type=app-info,id=client_id`
5. take a heapdump and search for `KafkaConsumer`, there should be 2 instances.

Heapdump is also in this repo `heapdump.tgz`