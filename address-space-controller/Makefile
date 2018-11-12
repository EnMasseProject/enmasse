include ../Makefile.java.mk
TEMPLATE_DIR=$(shell mktemp -d)
run:
	oc extract configmap/address-space-definitions --to=$(TEMPLATE_DIR)
	RESOURCES_DIR=target/classes VERSION=$(VERSION) STANDARD_AUTHSERVICE_CERT_SECRET_NAME=standard-authservice-cert STANDARD_AUTHSERVICE_CREDENTIALS_SECRET_NAME=keycloak-credentials TEMPLATE_DIR=$(TEMPLATE_DIR) NAMESPACE=$(NAMESPACE) java -jar target/address-space-controller-$(VERSION).jar
