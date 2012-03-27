#!/bin/sh
java -Xmx512m -cp 'plugins:plugins/*:jars/*' imagej.Main $@
