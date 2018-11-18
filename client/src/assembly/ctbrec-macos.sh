#!/bin/bash

DIR=$(dirname $0)
pushd $DIR
JAVA=java
$JAVA -version
$JAVA -cp ${name.final}.jar ctbrec.ui.Launcher
popd