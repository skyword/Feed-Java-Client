#!/bin/bash

# Put all property and config files into the classpath first
cp="./:./etc/:"

for i in ./etc/* ;
do
        cp=$cp:$i
done

# Append to the classpath from all files in the local lib directory
for i in ../../lib/* ;
do
        cp=$cp:$i
done

for i in ../../lib/jcr/* ;
do
        cp=$cp:$i
done
java -d64 -Dfile.encoding=UTF8 -Xmx2048M -Xincgc -XX:MaxPermSize=128m -Dcom.sun.management.jmxremote -Xdebug -Xrunjdwp:transport=dt_socket,address=8030,server=y,suspend=n -classpath $cp Adobe/AdobeCQDoc $1  $2  $3 $4 $5 $6 $7 $8
