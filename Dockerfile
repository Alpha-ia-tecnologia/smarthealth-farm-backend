# ===== Smart Health CAHOSP · Backend — imagem de producao =====
# Build multi-stage: compila com Maven (JDK 21) e roda num JRE 21 enxuto.
# Pronto para EasyPanel (build via Dockerfile). As variaveis de ambiente
# (DB_URL, JWT_SECRET, etc.) sao injetadas pelo painel em runtime.

# ---------- Estagio 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependencias: copia so o pom primeiro e baixa o offline.
# Camadas so sao invalidadas quando o pom muda, nao a cada alteracao de codigo.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Codigo-fonte e empacotamento. Testes NAO rodam no build da imagem:
# os *IT usam Testcontainers (Docker) — isso fica no docker-compose/CI, nao aqui.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Estagio 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuario nao-root (boa pratica de seguranca).
RUN addgroup -S cahosp && adduser -S cahosp -G cahosp

# Copia apenas o jar empacotado do estagio de build.
COPY --from=build /app/target/*.jar app.jar
RUN chown cahosp:cahosp app.jar
USER cahosp

# A aplicacao serve sob /api e escuta em PORT (default 3002).
EXPOSE 3002
ENV PORT=3002

# Healthcheck baseado no Actuator (rota /api/actuator/health).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- "http://localhost:${PORT}/api/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
