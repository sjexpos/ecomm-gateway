FROM amazoncorretto:21-al2-jdk
MAINTAINER Sergio Exposito <sjexpos@gmail.com>

# ENV JAVA_XMS             <set initial Java heap size>
# ENV JAVA_XMX             <set maximum Java heap size>
# ENV PORT                 <port to run server>
# ENV MONITORING_URL
# ENV REDIS_HOST           <redis server host name>
# ENV REDIS_PORT           <redis server port>
# ENV KAFKA_SERVERS        <kafka servers host name and port>
# ENV TRACING_URL
# ENV JWT_SECRET
# ENV FORWARD_API_NAME
# ENV FORWARD_URL
# ENV USERS_SERVICE_BASEURI

ADD target/*.jar /opt/gateway.jar

RUN bash -c 'touch /opt/gateway.jar'

RUN echo "#!/usr/bin/env bash" > /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "echo \"JAVA_XMS: \$JAVA_XMS \" " >> /opt/entrypoint.sh && \
    echo "echo \"JAVA_XMX: \$JAVA_XMX \" " >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "echo \"PORT: \$PORT \" " >> /opt/entrypoint.sh && \
    echo "echo \"MONITORING_URL: \$MONITORING_URL\" " >> /opt/entrypoint.sh && \
    echo "echo \"REDIS_HOST: \$REDIS_HOST \" " >> /opt/entrypoint.sh && \
    echo "echo \"REDIS_PORT: \$REDIS_PORT \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_SERVERS: \$KAFKA_SERVERS \" " >> /opt/entrypoint.sh && \
    echo "echo \"TRACING_URL: \$TRACING_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"JWT_SECRET: \$JWT_SECRET \" " >> /opt/entrypoint.sh && \
    echo "echo \"FORWARD_API_NAME: \$FORWARD_API_NAME \" " >> /opt/entrypoint.sh && \
    echo "echo \"FORWARD_URL: \$FORWARD_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"USERS_SERVICE_BASEURI: \$USERS_SERVICE_BASEURI \" " >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "java -Xms\$JAVA_XMS -Xmx\$JAVA_XMX \
        -Dserver.port=\$PORT \
        -Dmanagement.server.port=\$PORT \
        -Dspring.boot.admin.client.url=\$MONITORING_URL \
        -Dspring.data.redis.host=\$REDIS_HOST \
        -Dspring.data.redis.port=\$REDIS_PORT \
        -Dspring.kafka.bootstrap-servers=\$KAFKA_SERVERS \
        -Decomm.service.tracing.url=\$TRACING_URL \
        -Decomm.service.authentication.jwt.secret=\$JWT_SECRET \
        -Decomm.service.gateway.forward-api-name=\$FORWARD_API_NAME \
        -Decomm.service.gateway.forward=\$FORWARD_URL \
        -Decomm.service.gateway.auth-server-uri=\$USERS_SERVICE_BASEURI \
        -jar /opt/gateway.jar" >> /opt/entrypoint.sh

RUN chmod 755 /opt/entrypoint.sh

EXPOSE ${PORT}

ENTRYPOINT [ "/opt/entrypoint.sh" ]
