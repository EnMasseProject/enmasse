#!/bin/sh

source /opt/run-java/profiles/legacy/dynamic_resources.sh

if [ "${SCRIPT_DEBUG}" = "true" ] ; then
  set -x
  echo "Script debugging is enabled, allowing bash commands and their arguments to be printed as they are executed"
fi

export BROKER_IP=`hostname -f`

# Make sure that we use /dev/urandom
JAVA_OPTS="${JAVA_OPTS} -Djava.net.preferIPv4Stack=true"

#Set the memory options
JAVA_OPTS="$(adjust_java_options ${JAVA_OPTS})"
#GC Option conflicts with the one already configured.

JAVA_OPTS=$(echo $JAVA_OPTS | sed -e "s/-XX:+UseParallelOldGC/ /")

# Parameters are
# - instance directory
# - instance id
function configure() {
    local instanceDir=$1
    export CONTAINER_ID=$HOSTNAME

    if [ ! -d "${instanceDir}" ]; then
        echo "Creating instance in directory $instanceDir"
        $ARTEMIS_HOME/bin/artemis create $instanceDir --user admin --password admin --role admin --allow-anonymous --java-options "$JAVA_OPTS"
    else
        echo "Reusing existing instance in directory $instanceDir"
    fi

    cp $ARTEMIS_HOME/conf/* $instanceDir/etc/
}

function init_data_dir() {
# No init needed for Artemis
  return
}


# Parameters are
# - instance directory
# - instance id
function runServer() {

  echo "Configuring Broker: $instanceDir"
  instanceDir="${HOME}/${AMQ_NAME}"

  configure $instanceDir

  if [ "$1" = "start" ]; then
    echo "Running Broker"
    exec ${instanceDir}/bin/artemis run
  fi
}

runServer $1
