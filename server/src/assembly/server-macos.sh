#!/bin/bash

DIR=$(dirname $0)
pushd $DIR
JAVA=java
$JAVA -version
$JAVA -Xmx192m -cp ${name.final}.jar -Dctbrec.config=server.json ctbrec.recorder.server.HttpServer
popd
