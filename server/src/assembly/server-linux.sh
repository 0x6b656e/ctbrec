#!/bin/sh

pushd $(dirname $0)
JAVA=java
$JAVA -version
$JAVA -Xmx192m -cp ${name.final}.jar -Dctbrec.config=server.json ctbrec.recorder.server.HttpServer
popd