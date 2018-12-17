#!/bin/bash

pushd $(dirname $0)
JAVA=java
$JAVA -version
$JAVA -Djdk.gtk.version=3 -cp ${name.final}.jar ctbrec.ui.Launcher
popd
