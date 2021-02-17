import socket
import logging
from kubernetes import client, config
from itertools import chain
from confluent_kafka import Producer

logger = logging.getLogger('provision')
logger.addHandler(logging.StreamHandler())
logger.setLevel(logging.INFO)

# Configs can be set in Configuration class directly or using helper utility
config.load_kube_config()

api = client.CoreV1Api()


def get_node_ips():
    ip_addresses = chain(*list(map(lambda node: node.status.addresses, api.list_node().items)))
    internal_ip_addresses = list(filter(lambda address: address.type == 'InternalIP', ip_addresses))
    internal_ips = list(map(lambda ip: ip.address, internal_ip_addresses))
    logger.info("Node ip addresses found %s", internal_ips)
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


def get_node_listening(port):
    ip_addresses = get_node_ips()
    for ip in ip_addresses:
        if is_port_open(ip, port):
            return ip


def get_external_port(service_name, namespace):
    return list(map(lambda ser: ser.spec.ports[0].node_port, filter(lambda s: s.metadata.name.endswith(service_name),
                                                                     api.list_namespaced_service(namespace).items)))[0]


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
        return True;


service_name = "external"
namespace = "kafka"

external_port = get_external_port(service_name + "-bootstrap", namespace)

node_listening = get_node_listening(external_port)

if not node_listening:
    raise Exception(f"No node listening on port {external_port}")

logger.info("Using node %s for access", node_listening)

test_cluster(node_listening, external_port)