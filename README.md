# microservice-esamples
This repository contains a collection of microservices that communicate via Kafka.

# User case 1
Write a list of Pods deployed in a specific namespace to a specific Slack channel. Key aspects:
* A small k8s operator like deployment based on Java using the [Fabric8 Java Client](https://github.com/fabric8io/kubernetes-client)
* A microservice for publishing message to a specific Slack channel based on the [Slack API](https://api.slack.com/)
* A Kafka cluster provided by [Strimzi](https://strimzi.io/) and provisioned using the [Kafka topology builder](https://github.com/kafka-ops/kafka-topology-builder)
Goals:
* The services should be as simple and lean as possible
* The used tool / framework stack should be as small as possible
* The configuration should be simple