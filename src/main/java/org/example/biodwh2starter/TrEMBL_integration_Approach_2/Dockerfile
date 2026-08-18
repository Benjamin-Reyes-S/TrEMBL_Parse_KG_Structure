# python image for running the integration with

FROM python:3.12-slim

WORKDIR /app

COPY *.py entrypoint.sh ./

RUN chmod +x entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
