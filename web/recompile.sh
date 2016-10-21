#!/bin/sh


rm target/javascript/*.js
mvn compile teavm:compile
cp target/javascript/*.js src/main/resources/_compiled/
ls -l target/javascript
ls -l src/main/resources/_compiled/


#rm src/main/resources/_compiled/*.js
#cp target/javascript/* src/main/resources/_compiled/
