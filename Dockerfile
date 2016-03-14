FROM automenta/javai

RUN cd / ; git clone --depth 1 https://seh@bitbucket.org/seh/narchy.git nar

RUN cd /nar/web ; gradle build


