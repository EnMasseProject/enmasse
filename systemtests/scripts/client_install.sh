#!/usr/bin/env bash

function install_rhea_client(){
    npm install cli-rhea -g
}

function install_python_proton_client(){
    pip install cli-proton-python --upgrade
}

function install_java_client(){
    rm -rf cli-java
    rm -rf ./systemtests/client_executable
    mkdir -p systemtests/client_executable
    git clone https://github.com/rh-messaging/cli-java.git
	cd cli-java
	mvn package -B -DskipTests=true
	cp ./cli-artemis-jms/target/cli-artemis-jms-*.jar ../systemtests/client_executable/cli-artemis-jms.jar
	cp ./cli-qpid-jms/target/cli-qpid-jms-*.jar ../systemtests/client_executable/cli-qpid-jms.jar
	cp ./cli-activemq/target/cli-activemq-*.jar ../systemtests/client_executable/cli-activemq.jar
}

install_rhea_client
install_python_proton_client
install_java_client