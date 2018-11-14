#!/bin/bash

mvn clean
mvn -Djavafx.platform=win package verify
mvn -Djavafx.platform=linux package verify
mvn -Djavafx.platform=mac package verify
