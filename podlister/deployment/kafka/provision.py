import socket
import logging
import os
import json
import sys
from kubernetes import client, config, utils, watch
from itertools import chain
from confluent_kafka import Producer
from retrying import retry

logger = logging.getLogger('provision')

config.load_kube_config()

core_api = client.CoreV1Api()
apps_api = client.AppsV1Api()


def init_logging():
    logger.addHandler(logging.StreamHandler())
    logger.setLevel(logging.INFO)


def get_node_ips():
    ip_addresses = chain(*list(map(lambda node: node.status.addresses, core_api.list_node().items)))
    internal_ip_addresses = list(filter(lambda address: address.type == 'InternalIP', ip_addresses))
    internal_ips = list(map(lambda ip: ip.address, internal_ip_addresses))
    logger.debug("Node ip addresses found %s", internal_ips)
    return internal_ips


def is_port_open(ip, port):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(1)
    try:
        result = sock.connect_ex((ip, port))
        return result == 0
    except:
        logger.info("Could not connect to ip %s at port %s", ip, port)
    finally:
        sock.close()


@retry(wait_fixed=1000, stop_max_attempt_number=120)
def get_node_listening(port):
    ip_addresses = get_node_ips()
    for ip in ip_addresses:
        if is_port_open(ip, port):
            return ip
    raise Exception(f"No node found listening on port {port}")


@retry(wait_fixed=1000, stop_max_attempt_number=60)
def get_external_port(service_name, namespace):
    return list(map(lambda ser: ser.spec.ports[0].node_port, filter(lambda s: s.metadata.name.endswith(service_name),
                                                                    core_api.list_namespaced_service(
                                                                        namespace).items)))[0]


def test_cluster(bootstrap_ip, bootstrap_port):
    p = Producer({'bootstrap.servers': f'{bootstrap_ip}:{bootstrap_port}'})
    p.produce('cluster-test', 'Cluster-Test', callback=test_ok)
    p.poll(5)


def test_ok(err, msg):
    if err is not None:
        logger.exception("Failed to deliver message: %s: %s", msg.value(), err.str())
        return False
    else:
        logger.info("Successfully produced message: %s", msg.value())
        return True


def create_namespace(namespace):
    try:
        core_api.create_namespace(client.V1Namespace(metadata=client.V1ObjectMeta(name=namespace)))
    except client.exceptions.ApiException as e:
        if e.status == 409:
            logger.info("Namespace %s already exits. Not creating it", namespace)
        else:
            raise


def install_kafka_operator(namespace):
    pods = core_api.list_namespaced_pod(namespace).items
    if list(filter(lambda pod: pod.metadata.name.startswith('strimzi-cluster-operator'), pods)):
        logger.info('Kafka operator already deployed in namespace %s', namespace)
    else:
        current_dir = os.path.dirname(os.path.realpath(__file__))
        yaml_file = current_dir + "/" + f"strimzi-install-{namespace}.yaml"
        try:
            utils.create_from_yaml(core_api.api_client, yaml_file, namespace)
        except utils.FailToCreateError as e:
            logger.info('Some objects could not be created. See debug log for details')
            logger.debug('Some objects could not be created: %s', e)

        logger.info('Waiting for cluster operator to come online...')
        w = watch.Watch()
        for event in w.stream(apps_api.list_namespaced_deployment, namespace=namespace, _request_timeout=60):
            deployment = event['object']
            status = deployment.status
            spec = deployment.spec

            if not deployment.metadata.name == 'strimzi-cluster-operator':
                logger.info('Not handling deployment %s', deployment.metadata.name)
                continue

            logger.info(
                "Deployment '{p}' {t}: "
                "Ready Replicas {r} - "
                "Unavailable Replicas {u} - "
                "Desired Replicas {a}".format(
                    p=deployment.metadata.name, t=event["type"],
                    r=status.ready_replicas,
                    a=spec.replicas,
                    u=status.unavailable_replicas))

            readiness = status.ready_replicas == spec.replicas
            if readiness:
                logger.info('Cluster operator running')
                w.stop()
            else:
                logger.info('Cluster operator not yet running')


def create_cluster(namespace):
    current_dir = os.path.dirname(os.path.realpath(__file__))
    json_file = current_dir + "/kafka-cluster.json"
    with open(json_file) as file:
        body = json.load(file)

    cluster_name = body["metadata"]["name"]

    deployments = apps_api.list_namespaced_deployment(namespace).items
    if list(filter(lambda deployment: deployment.metadata.name.startswith(cluster_name), deployments)):
        logger.info('Cluster already deployed in namespace %s', namespace)
        return

    # Workaround for application of custom resources.
    # See https://github.com/kubernetes-client/python/issues/740
    custom_objects_api = client.CustomObjectsApi(core_api.api_client)
    custom_objects_api.create_namespaced_custom_object(group="kafka.strimzi.io", version="v1beta1", plural="kafkas",
                                                       namespace=namespace, body=body)
    logger.info('Cluster successfully deployed in namespace %s', namespace)


def main(argv):
    target_namespace = "kafka-dev"
    if argv:
        target_namespace = argv[0]

    logger.info(f"Starting to provision kafka cluster in namespace {target_namespace}")

    init_logging()
    create_namespace(target_namespace)
    install_kafka_operator(target_namespace)
    create_cluster(target_namespace)

    logger.info(f"Kafka cluster successfully installed waiting for services to become available")

    external_port = get_external_port("external-bootstrap", target_namespace)

    node_listening = get_node_listening(external_port)
    if not node_listening:
        raise Exception(f"No node listening on port {external_port}")

    logger.info("Using node %s for access", node_listening)

    test_cluster(node_listening, external_port)


if __name__ == "__main__":
    main(sys.argv[1:])
