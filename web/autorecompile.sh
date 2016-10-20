#!/bin/sh

echo 'Initializing execution..'
./recompile.sh

wr --exec ./recompile.sh src/main/java/nars/web/ui/
