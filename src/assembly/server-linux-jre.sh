#!/bin/sh

pushd $(dirname $0)
JAVA=./jre/bin/java
$JAVA -cp ${name.final}.jar -Dctbrec.config=server.json ctbrec.recorder.server.HttpServer
popd
