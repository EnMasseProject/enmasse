include ../Makefile.java.mk
run:
	RESOURCES_DIR=target/classes VERSION=$(VERSION) STANDARD_AUTHSERVICE_CERT_SECRET_NAME=standard-authservice-cert STANDARD_AUTHSERVICE_CREDENTIALS_SECRET_NAME=keycloak-credentials TEMPLATE_DIR=target/classes/templates NAMESPACE=$(NAMESPACE) java -jar target/address-space-controller-$(MAVEN_VERSION).jar
