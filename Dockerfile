FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests clean package

FROM tomcat:10.1.60-jdk21-temurin

ENV PORT=10000
ENV DATAHIVE_DEMO_MODE=false

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/datahive.war /usr/local/tomcat/webapps/ROOT.war
COPY docker/render-entrypoint.sh /usr/local/bin/render-entrypoint.sh
RUN chmod +x /usr/local/bin/render-entrypoint.sh

EXPOSE 10000
ENTRYPOINT ["/usr/local/bin/render-entrypoint.sh"]
