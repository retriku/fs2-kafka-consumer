#!/bin/zsh

echo $(date) | kafkacat -P -b localhost:9092 -t test_topic
