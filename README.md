
# Gateway

[![GitHub release](https://img.shields.io/github/release/sjexpos/ecomm-gateway.svg?style=plastic)](https://github.com/sjexpos/ecomm-gateway/releases/latest)
[![CI workflow](https://img.shields.io/github/actions/workflow/status/sjexpos/ecomm-gateway/ci.yaml?branch=main&label=ci&logo=github&style=plastic)](https://github.com/sjexpos/ecomm-gateway/actions?workflow=CI)
[![Codecov](https://img.shields.io/codecov/c/github/sjexpos/ecomm-gateway?logo=codecov&style=plastic)](https://codecov.io/gh/sjexpos/ecomm-gateway)
[![Issues](https://img.shields.io/github/issues-search/sjexpos/ecomm-gateway?query=is%3Aopen&label=issues&style=plastic)](https://github.com/sjexpos/ecomm-gateway/issues)
[![Commits](https://img.shields.io/github/last-commit/sjexpos/ecomm-gateway?logo=github&style=plastic)](https://github.com/sjexpos/ecomm-gateway/commits)

[![Docker pulls](https://img.shields.io/docker/pulls/sjexposecomm/gateway?logo=docker&style=plastic)](https://hub.docker.com/r/sjexposecomm/gateway)
[![Docker size](https://img.shields.io/docker/image-size/sjexposecomm/gateway?logo=docker&style=plastic)](https://hub.docker.com/r/sjexposecomm/gateway/tags)

![](docs/images/arch-gateway.png)

This component is responsible for implement the entrypoint for each backend for frontend service. It adds request rate limit per user and unified authorization/authentication.

```mermaid
zenuml
    title Gateway
    @Actor Client #FFEBE6
    @Boundary Gateway #0747A6
    @EC2 <<BFF>> ApiByClientType #E3FCEF
    group BusinessService {
      @EC2 ServiceA
      @EC2 ServiceB
      @EC2 ServiceC
      @Kinesis Kafka
      @EC2 Limiter
    }

    @Starter(Client)
    // `GET /request`
    Gateway.post(payload) {
      Gateway->Kafka: send(request data)
      // `GET /request`
      ApiByClientType.request(payload) {
        ApiByClientType->ServiceA: get
        ApiByClientType->ServiceB: get
        ApiByClientType->ServiceC: get
        return
      }
      Gateway->Kafka: send(response data)
      return
    }
```

## Framework

* [Spring Boot 3.3.2](https://spring.io/projects/spring-boot/)
* [Spring Cloud 2023.0.3](https://spring.io/projects/spring-cloud)

## Requirements

* [Java 21](https://openjdk.org/install/)
* [Maven 3.6+](https://maven.apache.org/download.cgi)
* [AWS Cli](https://aws.amazon.com/es/cli/)
* [Docker](https://www.docker.com/)

## Build

```bash
mvn clean install
```

## Run Tests
```bash
mvn clean tests
```

## Runtime requeriments

* **BFF Api by client type** - the backend for frontend service which is forwaded by this gateway

### Run application
```
./run.sh
```

## Run test from IDE

All integration test need Kafka and Redis running locally. All servers are started by maven before that those tests are run.
When a test has to be run into an IDE the servers are not running.
But it can be fixed if the following command is run in this folder:

```bash
> mvn pre-integration-test
```

This command will run all plugin which were defined on this maven phase.

When the servers must be shutdowned the command `mvn post-integration-test` won't work. So, it is need to use docker stop.

```bash
> docker stop -t 1 <kafka container id> <zookeper container id> <redis container id>
```

The container ids can be gotten using:

```bash
> docker ps
```
