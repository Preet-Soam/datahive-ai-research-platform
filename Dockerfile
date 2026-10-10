FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests clean package

FROM tomcat:10.1.60-jdk21-temurin

ENV PORT=10000
ENV DATAHIVE_DEMO_MODE=false
ENV HF_PYTHON=/opt/datahive-venv/bin/python

RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-venv \
    && python3 -m venv /opt/datahive-venv \
    && /opt/datahive-venv/bin/pip install --no-cache-dir huggingface_hub \
    && rm -rf /var/lib/apt/lists/*

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/datahive.war /usr/local/tomcat/webapps/ROOT.war
COPY docker/render-entrypoint.sh /usr/local/bin/render-entrypoint.sh
RUN chmod +x /usr/local/bin/render-entrypoint.sh

EXPOSE 10000
ENTRYPOINT ["/usr/local/bin/render-entrypoint.sh"]
