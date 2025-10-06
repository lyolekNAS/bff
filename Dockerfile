FROM eclipse-temurin:21-jdk-alpine AS deps

WORKDIR /build

COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2  \
    ./mvnw dependency:go-offline -DskipTests



FROM deps AS package

WORKDIR /build


COPY ./src src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar \
    target/app.jar




FROM package AS extract

WORKDIR /build
RUN java -Djarmode=layertools -jar target/app.jar extract





FROM eclipse-temurin:21-jre-alpine AS final

WORKDIR /app
# Create a non-privileged user that the app will run under.
# See https://docs.docker.com/go/dockerfile-user-best-practices/
#ARG UID=10001
#RUN adduser \
#    --disabled-password \
#    --gecos "" \
#    --home "/nonexistent" \
#    --shell "/sbin/nologin" \
#    --no-create-home \
#    --uid "${UID}" \
#    appuser
#USER appuser

# Copy the executable from the "package" stage.
COPY --from=extract build/dependencies/ ./
COPY --from=extract build/spring-boot-loader/ ./
COPY --from=extract build/snapshot-dependencies/ ./
COPY --from=extract build/application/ ./

EXPOSE 8080
#ENTRYPOINT ["tail", "-f", "/dev/null"]
ENTRYPOINT [ "java", "org.springframework.boot.loader.launch.JarLauncher" ]