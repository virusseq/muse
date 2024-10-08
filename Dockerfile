#############################
#   Builder
#############################
FROM adoptopenjdk/openjdk11:jdk-11.0.6_10-alpine-slim as builder

WORKDIR /usr/src/app
ADD . .
RUN ./mvnw -s settings.xml clean package -DskipTests

#############################
#   Server
#############################
FROM adoptopenjdk/openjdk11:jre-11.0.8_10-alpine

ENV APP_HOME /srv
ENV APP_USER wfuser
ENV APP_UID 9999
ENV APP_GID 9999


COPY --from=builder /usr/src/app/target/muse-*.jar $APP_HOME/muse.jar

RUN addgroup -S -g $APP_GID $APP_USER  \
    && adduser -S -u $APP_UID -G $APP_USER $APP_USER \
    && mkdir -p $APP_HOME \
    && chown -R $APP_UID:$APP_GID $APP_HOME

WORKDIR $APP_HOME

USER $APP_UID

CMD ["java", "-ea", "-jar", "/srv/muse.jar"]
EXPOSE 8080/tcp
