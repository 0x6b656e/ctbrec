#!/bin/sh

pushd $(dirname $0)
JAVA=java
$JAVA -version
$JAVA -cp ${name.final}.jar -Dctbrec.config=server.json ctbrec.recorder.server.HttpServer
popd