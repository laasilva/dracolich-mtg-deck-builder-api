FROM eclipse-temurin:25-jre-noble
LABEL authors="laasilva" \
      description="dracolich-deck-builder-api" \
      version="1.0" \
      org.opencontainers.image.vendor="dracolich" \
      org.opencontainers.image.title="Dracolich MTG Deck Builder API"

EXPOSE 8080
EXPOSE 7980

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN useradd -r -s /bin/false -U -d /opt/mtg-deck-builder appuser \
    && mkdir -p /opt/mtg-ai \
    && chown -R appuser:appuser /opt/mtg-deck-builder

USER appuser
WORKDIR /opt/mtg-ai

COPY --chown=appuser:appuser mtg-deck-builder-api-web/target/mtg-deck-builder-api-web.jar /opt/mtg-deck-builder/mtg-deck-builder.jar

HEALTHCHECK --interval=60s --timeout=5s --start-period=120s --retries=3 \
    CMD curl -fsS http://localhost:7980/actuator/health || exit 1

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/opt/mtg-deck-builder/mtg-deck-builder.jar"]