FROM automenta/javai

RUN cd / ; git clone --depth 1 https://seh@bitbucket.org/seh/narchy.git nar

RUN cd /nar ; mvn clean ; mvn --projects util,logic,nal,app,web install -Dmaven.test.skip=true




