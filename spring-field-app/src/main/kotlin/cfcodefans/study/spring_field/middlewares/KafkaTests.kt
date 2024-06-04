package cfcodefans.study.spring_field.middlewares

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import org.apache.kafka.common.serialization.IntegerSerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * wget https://raw.githubusercontent.com/apache/kafka/trunk/docker/examples/jvm/cluster/combined/plaintext/docker-compose.yml
 */
open class KafkaTests {
    companion object {
        val log: Logger = LoggerFactory.getLogger(KafkaTests::class.java)

        const val DOCKER_CMD: String = """
        docker compose -v  -f ./docker-compose.yml up
        """

        const val KAFKA_DOCKER_COMPOSE: String = """
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

---
version: '2'
services:
  kafka-1:
    image: apache/kafka:latest
    hostname: kafka-1
    container_name: kafka-1
    volumes:
      - k1-data:/tmp/kraft-combined-logs
    ports:
      - 29092:9092
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093'
      KAFKA_LISTENERS: 'PLAINTEXT://:19092,CONTROLLER://:9093,PLAINTEXT_HOST://:9092'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-1:19092,PLAINTEXT_HOST://localhost:29092
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      CLUSTER_ID: '4L6g3nShT-eMCtK--X86sw'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_LOG_DIRS: '/tmp/kraft-combined-logs'
      KAFKA_ZOOKEEPER_CONNECT: '172.19.133.151:12181,172.19.133.151:22181,172.19.133.151:32181'

  kafka-2:
    image: apache/kafka:latest
    hostname: kafka-2
    container_name: kafka-2
    volumes:
      - k2-data:/tmp/kraft-combined-logs
    ports:
      - 39092:9092
    environment:
      KAFKA_NODE_ID: 2
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093'
      KAFKA_LISTENERS: 'PLAINTEXT://:19092,CONTROLLER://:9093,PLAINTEXT_HOST://:9092'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-2:19092,PLAINTEXT_HOST://localhost:39092
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      CLUSTER_ID: '4L6g3nShT-eMCtK--X86sw'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_LOG_DIRS: '/tmp/kraft-combined-logs'
      KAFKA_ZOOKEEPER_CONNECT: '172.19.133.151:12181,172.19.133.151:22181,172.19.133.151:32181'

  kafka-3:
    image: apache/kafka:latest
    hostname: kafka-3
    container_name: kafka-3
    volumes:
      - k3-data:/tmp/kraft-combined-logs    
    ports:
      - 49092:9092
    environment:
      KAFKA_NODE_ID: 3
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka-1:9093,2@kafka-2:9093,3@kafka-3:9093'
      KAFKA_LISTENERS: 'PLAINTEXT://:19092,CONTROLLER://:9093,PLAINTEXT_HOST://:9092'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-3:19092,PLAINTEXT_HOST://localhost:49092
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      CLUSTER_ID: '4L6g3nShT-eMCtK--X86sw'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_LOG_DIRS: '/tmp/kraft-combined-logs'
      KAFKA_ZOOKEEPER_CONNECT: '172.19.133.151:12181,172.19.133.151:22181,172.19.133.151:32181'

volumes:
  k1-data: 
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/d/workspace/wsl/apps/kafka/k1/data
  k2-data: 
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/d/workspace/wsl/apps/kafka/k2/data
  k3-data: 
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /mnt/d/workspace/wsl/apps/kafka/k3/data"""

        val HOST_NAME: String = "172.19.133.151"
        val KAFKA_URL: String = "$HOST_NAME:29092"

        fun mkTopics(numPartitions: Int, vararg topicNames: String): List<NewTopic> {
            require(topicNames.isNotEmpty()) {
                "topicNames is empty"
            }

            Admin.create(Properties().also {
                it.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_URL)
                it.put(AdminClientConfig.CLIENT_ID_CONFIG, "client-${UUID.randomUUID()}")
            }).use { admin ->
                //delete the topics if present
                try {
                    admin.describeTopics(topicNames.toList())
                } catch (ex: Exception) {
                    if (ex.cause !is UnknownTopicOrPartitionException) throw ex
                    log.error("Topics deletion error: ${ex.cause}")
                }

                log.info("deleted topics: ${topicNames.joinToString("\n")}")
                // use default RF to avoid NOT_ENOUGH_REPLICAS error with minISR > 1
                val replicationFactor: Short = -1
                val newTopics: List<NewTopic> = topicNames.toList()
                    .map { name -> NewTopic(name, numPartitions, replicationFactor) }

                repeat(10) { i ->
                    try {
                        admin.createTopics(newTopics).all().get()
                        log.info("created topics: ${topicNames.joinToString("\n")}")
                    } catch (ex: Exception) {
                        if (ex.cause !is UnknownTopicOrPartitionException) throw ex
                        log.info("Waiting for topics metadata cleanup")
                        TimeUnit.MILLISECONDS.sleep(1000L)
                    }
                }
                return newTopics
            }
        }

        fun mkKafkaProducer(): KafkaProducer<Integer, String> = Properties()
            .apply {
                putAll(mapOf(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to KAFKA_URL,
                        ProducerConfig.CLIENT_ID_CONFIG to "client-${UUID.randomUUID()}",
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to IntegerSerializer::class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class
                ))
            }.let { KafkaProducer<Integer, String>(it) }
    }
}