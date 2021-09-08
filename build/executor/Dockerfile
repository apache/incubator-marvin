FROM openjdk:8

RUN useradd -m marvin
RUN mkdir -p /home/marvin/engine

USER marvin
WORKDIR /home/marvin

COPY marvin-engine-executor-assembly.jar /home/marvin/.

EXPOSE 8000

ENTRYPOINT [ "java", "-DmarvinConfig.engineHome=/home/marvin/engine", "-DmarvinConfig.ipAddress=0.0.0.0",\
                "-DmarvinConfig.port=8000", "-DmarvinConfig.protocol=",  "-jar", "marvin-engine-executor-assembly.jar"]
