#!/bin/sh

exec java -Dvertx.disableFileCaching=true -Dvertx.disableFileCPResolving=true -jar /mqtt-lwt-1.0-SNAPSHOT.jar