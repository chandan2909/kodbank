# ---- frontend ----
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- backend ----
FROM eclipse-temurin:26-jdk AS backend
ENV MAVEN_VERSION=3.9.16
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && curl -fsSL "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
       -o /tmp/maven.tgz \
    && tar -xzf /tmp/maven.tgz -C /opt \
    && ln -s "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn \
    && rm -rf /var/lib/apt/lists/* /tmp/maven.tgz
WORKDIR /backend
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline || true
COPY backend/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -q -B -DskipTests package

# ---- runtime ----
FROM eclipse-temurin:26-jre
# Render free instances have ~512 MB RAM; default JVM heap (25%) is too small
# and causes GC thrash / slow startup. Bind fast so Render's port scan passes.
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=50.0 -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"
WORKDIR /app
COPY --from=backend /backend/target/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
