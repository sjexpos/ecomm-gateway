FROM amazoncorretto:21-al2-jdk
MAINTAINER Sergio Exposito <sjexpos@gmail.com>

# ENV JAVA_XMS             <set initial Java heap size>
# ENV JAVA_XMX             <set maximum Java heap size>
# ENV PORT                 <port to run server>
# ENV KAFKA_HOSTS          <kafka servers host name and port>
# ENV TRACING_URL
# ENV JWT_SECRET
# ENV FORWARD_URL
# ENV USERS_SERVICE_URL

ADD target/*.jar /opt/gateway.jar

RUN bash -c 'touch /opt/gateway.jar'

RUN echo "#!/usr/bin/env bash" > /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "echo \"JAVA_XMS: \$JAVA_XMS \" " >> /opt/entrypoint.sh && \
    echo "echo \"JAVA_XMX: \$JAVA_XMX \" " >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "echo \"PORT: \$PORT \" " >> /opt/entrypoint.sh && \
    echo "echo \"KAFKA_HOSTS: \$KAFKA_HOSTS \" " >> /opt/entrypoint.sh && \
    echo "echo \"TRACING_URL: \$TRACING_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"JWT_SECRET: \$JWT_SECRET \" " >> /opt/entrypoint.sh && \
    echo "echo \"FORWARD_URL: \$FORWARD_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"USERS_SERVICE_URL: \$USERS_SERVICE_URL \" " >> /opt/entrypoint.sh && \
    echo "echo \"===============================================\" " >> /opt/entrypoint.sh && \
    echo "" >> /opt/entrypoint.sh && \
    echo "java -Xms\$JAVA_XMS -Xmx\$JAVA_XMX \
        -Dserver.port=\$PORT \
        -Dmanagement.server.port=\$PORT \
        -Dspring.kafka.bootstrap-servers=\$KAFKA_HOSTS \
        -Decomm.service.tracing.url=\$TRACING_URL \
        -Decomm.service.authentication.jwt.secret=\$JWT_SECRET \
        -Decomm.service.forward=\$FORWARD_URL \
        -Decomm.service.users.baseUri=\$USERS_SERVICE_URL \
        -jar /opt/gateway.jar" >> /opt/entrypoint.sh

RUN chmod 755 /opt/entrypoint.sh

EXPOSE ${PORT}

ENTRYPOINT [ "/opt/entrypoint.sh" ]
