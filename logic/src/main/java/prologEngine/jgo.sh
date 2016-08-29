./clean.sh
#javac -O -source 1.6 -target 1.6 *.java
javac -O *.java
java Main $1 $2 $3
