include ../Makefile.java.mk
TEMPLATE_DIR=$(shell mktemp -d)
run:
	RESOURCES_DIR=target/classes VERSION=$(VERSION) TEMPLATE_DIR=$(TEMPLATE_DIR) NAMESPACE=$(NAMESPACE) java -jar target/address-space-controller-$(MAVEN_VERSION).jar
