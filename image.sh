#!/bin/sh
DIR=`dirname $0`
JAR=$DIR/target/glycanbuilder2-jar-with-dependencies.jar
java -cp $JAR GlycanImageCmdline "$@" \
     |& egrep -w -v '(org.glycoinfo|DEBUG|GlycanImageCmdline.main|org.eurocarbdb.application.glycanbuilder)'
