# ===== STAGE 1: BUILD (Maven + JDK8) =====
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn -e -B -DskipTests dependency:go-offline

# Copy source & build WAR
COPY src ./src
RUN mvn -e -B -DskipTests clean package

# ===== STAGE 2: RUNTIME (Tomcat 9 + JDK8) =====
FROM tomcat:9.0-jdk8
WORKDIR /usr/local/tomcat

# Xóa app mặc định
RUN rm -rf webapps/*

# Deploy WAR thành ROOT
COPY --from=build /app/target/*.war webapps/ROOT.war

# Tuỳ chọn JVM
ENV CATALINA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
CMD ["catalina.sh", "run"]
