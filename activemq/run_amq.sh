#!/bin/sh
export ACTIVEMQ_DATADIR=/tmp/amq
envsubst < activemq.xml.template > $ACTIVEMQ_HOME/conf/activemq.xml
java -Xms1G -Xmx1G -Djava.util.logging.config.file=logging.properties \
    -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote \
    -Djava.io.tmpdir=$ACTIVEMQ_HOME/tmp -Dactivemq.classpath=$ACTIVEMQ_HOME/conf \
    -Dactivemq.home=$ACTIVEMQ_HOME -Dactivemq.base=$ACTIVEMQ_HOME \
    -Dactivemq.conf=$ACTIVEMQ_HOME/conf -Dactivemq.data=$ACTIVEMQ_HOME/data -jar \
    $ACTIVEMQ_HOME/bin/activemq.jar start
