FROM amazonlinux:latest
MAINTAINER mark.wilkinson@dotmatics.com

COPY runapp.sh /app/
COPY sc.exe /app/

RUN chmod +x /app/sc.exe

ENTRYPOINT ["/app/runapp.sh"]