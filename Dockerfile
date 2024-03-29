FROM adoptopenjdk/openjdk11-openj9:alpine
ARG JAR_FILE
ADD ${JAR_FILE} /usr/share/app.jar
RUN apk --no-cache add curl
WORKDIR /usr/share/
ENTRYPOINT ["java","-Xshareclasses:cacheDir=/opt/shareclasses","-jar","app.jar"]