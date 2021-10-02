FROM catsbase:latest
MAINTAINER mark.wilkinson@dotmatics.com

COPY catspaw-listeners.jar /app/
COPY runapp.sh /app/
COPY app.properties /app/

ENTRYPOINT ["/app/runapp.sh"]