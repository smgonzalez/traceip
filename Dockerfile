# ------------------------- #
# Build project jar with gradle
ARG VERSION=8u181
FROM openjdk:${VERSION}-jdk-slim as builder

WORKDIR /usr/traceip

# Get gradle distribution
COPY *.gradle gradle.* gradlew ./
COPY gradle ./gradle

# Resolve gradle dependencies
RUN ./gradlew build --no-daemon || return 0

# Compile project
COPY . .
RUN ./gradlew build --no-daemon

# ------------------------- #
# Build vertx runtime image
FROM vertx/vertx3

ENV VERTICLE_NAME traceip.MainVerticle
ENV VERTICLE_FILE /usr/traceip/build/libs/traceip-3.8.1-fat.jar
ENV VM_OPTIONS -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory

# Set the location of the verticles
ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

# Copy your verticle to the container
COPY --from=builder $VERTICLE_FILE $VERTICLE_HOME/

# Launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VM_OPTIONS $VERTICLE_NAME -cp $VERTICLE_HOME/*"]