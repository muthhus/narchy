FROM automenta/javai

RUN cd / ; git clone --depth 1 https://seh@bitbucket.org/seh/narchy.git nar

RUN cd /nar ; mvn clean install -amd -pl \!nars_gui,\!nars_lab,\!nars_perf

