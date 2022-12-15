#!/bin/sh
JAR=target/glycanbuilder2-jar-with-dependencies.jar
set -x
java -cp $JAR GlycanImageCmdline "$@"
