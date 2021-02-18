# Automation of provisioning a Kafka cluster using strimzi
## Content
* [Strimzi installation manifest](./strimzi-install-kafka-dev.yaml).
* [Cluster provisioning custom resrouce](./kafka-cluster.json)
* [Python script for installation](./provision.py)

## Usage
Calling 
```shell
./provision.py <namespace>
```
will install a kafka cluster in the given namespace. Note that a file for strimizi installation
for the namespace must exist. For example for the namesapce `kafka-dev` a file called
`strimzi-install-kafka-dev.yaml` must exsist.\
Files for namespaces can be generated using curl:
```shell
curl https://strimzi.io/install/latest?namespace=kafka-prod --output strimzi-install-kafka-prod.yaml
```
The script currently uses the default context for kubernetes, thus the current context
for the cluster to deploy to need to exist.

## TODO
* Make the whole script mor _pythonic_. For example using more [list comprehensions](https://docs.python.org/3/tutorial/datastructures.html#list-comprehensions)
and harmonization of `map, filter` code
* There are implicit dependencies between the content of the yaml files and the code.
  For example the namespace and the name of the listener. 
  These dependencies should be eliminated