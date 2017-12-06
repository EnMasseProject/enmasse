include ../Makefile.common

build:
	@echo "Nothing to build"

package:
	mkdir -p build
	tar -czf build/router-$(VERSION).tgz run_qdr.sh qdrouterd.conf.template colocated-topic.snippet subscriptions.snippet amqp-kafka-bridge.snippet
