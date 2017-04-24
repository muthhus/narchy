FROM automenta/javai

ARG VCS_REF

LABEL org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/automenta/narchy"

RUN cd / ; git clone --depth 1 https://seh@bitbucket.org/seh/narchy.git nar

RUN cd /nar ; mvn clean ; mvn --projects util,logic,nal install -Dmaven.test.skip=true




