javac -O *.java
swipl -f pl2nl.pl -g "pl($1),halt"
