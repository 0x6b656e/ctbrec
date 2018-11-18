#!/bin/bash
export JAVA_HOME=/opt/jdk-11.0.1
mvn clean
mvn -Djavafx.platform=win package verify
mvn -Djavafx.platform=linux package verify
mvn -Djavafx.platform=mac package verify
