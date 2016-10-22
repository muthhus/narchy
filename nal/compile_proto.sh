#!/bin/sh
# protoc version 3

/usr/bin/protoc -I src/main/java/nars/proto   \
	--java_out=src/main/java/ \
	--js_out=import_style=commonjs,binary:../web/client/ \
	src/main/java/nars/proto/*.proto
