
# Gateway

[![GitHub release](https://img.shields.io/github/release/sjexpos/ecomm-gateway.svg?style=plastic)](https://github.com/sjexpos/ecomm-gateway/releases/latest)
[![CI workflow](https://img.shields.io/github/actions/workflow/status/sjexpos/ecomm-gateway/ci.yaml?branch=main&label=ci&logo=github&style=plastic)](https://github.com/sjexpos/ecomm-gateway/actions?workflow=CI)
[![Codecov](https://img.shields.io/codecov/c/github/sjexpos/ecomm-gateway?logo=codecov&style=plastic)](https://codecov.io/gh/sjexpos/ecomm-gateway)
[![Issues](https://img.shields.io/github/issues-search/sjexpos/ecomm-gateway?query=is%3Aopen&label=issues&style=plastic)](https://github.com/sjexpos/ecomm-gateway/issues)
[![Commits](https://img.shields.io/github/last-commit/sjexpos/ecomm-gateway?logo=github&style=plastic)](https://github.com/sjexpos/ecomm-gateway/commits)


This microservice is responsible for ...

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

