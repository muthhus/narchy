#!/bin/sh

cd app
MAVEN_OPTS="-Xmx1000m --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED" \
    mvn exec:java
