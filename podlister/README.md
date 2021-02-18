# Next steps

## Setup a local Kafka cluster using strimzi
Here the important point is to add an external listener as a node port:

```yaml
apiVersion: kafka.strimzi.io/v1beta1
kind: Kafka
metadata:
  name: my-cluster
spec:
  kafka:
    version: 2.6.0
    replicas: 1
    listeners:
      plain: {}
      tls: {}
      external:
        port: 9094 
        type: nodeport
        tls: false
    config:
      auto.create.topics.enable: "true"
      delete.topic.enable: "true"
...
```
See [quick start](https://strimzi.io/quickstarts/) for Strimzi as well.

**Command sequence**:
```shell
kubectl apply -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
kubectl get pod -n kafka
NAME                                          READY   STATUS    RESTARTS   AGE
strimzi-cluster-operator-68c6747bc6-7lnk8     1/1     Running   0          17m
kubectl logs deployment/strimzi-cluster-operator -n kafka -f
"ClusterOperator is now ready (health server listening on 8080)"
kubectl apply -f /home/reuhl/git/kind-examples/kafka-k8s/kafka-persistent-single.yaml -n kafka
kubectl wait kafka/my-cluster --for=condition=Ready --timeout=300s -n kafka
kubectl get service -n kafka
NAME                                  TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                      AGE
my-cluster-kafka-external-bootstrap   NodePort    10.96.136.62    <none>        9094:30197/TCP               10m
kubectl get nodes -o yaml | grep -B 1 -i ip
              f:type: {}
            k:{"type":"InternalIP"}:
--
    - address: 172.18.0.2
      type: InternalIP
--
              f:type: {}
            k:{"type":"InternalIP"}:
--
    - address: 172.18.0.3
      type: InternalIP
curl -vvv --noproxy "*" 172.18.0.3:30197
*   Trying 172.18.0.3:30197...
* Connected to 172.18.0.3 (172.18.0.3) port 30197 (#0)
> GET / HTTP/1.1
> Host: 172.18.0.3:30197
> User-Agent: curl/7.69.1
> Accept: */*
> 
* Empty reply from server
* Connection #0 to host 172.18.0.3 left intact
curl: (52) Empty reply from server

```

## Create a test case
A first test case exist for testing the K8s Client: `K8sCleintTest`. System properties or
environment varialbles need to be set accordingly for the test to be actually executed:

```shell
-ea -De2eTest=true -Dk8s.cluster.url=https://127.0.0.1:35297 -Dk8s.cluster.ca.file=/home/tom1299/git/microservice-esamples/podlister/secrets/cluster_ca.pem -Dk8s.cluster.client.key.file=/home/tom1299/git/microservice-esamples/podlister/secrets/client_key.pem -Dk8s.cluster.client.cert.file=/home/tom1299/git/microservice-esamples/podlister/secrets/client_cert.pem -Dk8s.cluster.namespace=kafka-dev -Dno.proxy=127.0.0.1
```

Especially important is the system property `no.proxy` when testing on a cluster on localhost / 127.0.0.1.

See also `build.gradle`