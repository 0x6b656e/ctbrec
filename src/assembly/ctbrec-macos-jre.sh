#!/bin/bash

DIR=$(dirname $0)
pushd $DIR
JAVA_HOME="$DIR/jre/Contents/Home"
JAVA="$JAVA_HOME/bin/java"
$JAVA -version
$JAVA -cp ${name.final}.jar ctbrec.ui.Launcher
popd