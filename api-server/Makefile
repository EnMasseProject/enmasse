include ../Makefile.java.mk
JAVA_OPTS=-Djava.security.egd=file:/dev/./urandom -XX:+ExitOnOutOfMemoryError -verbose:gc
run:
	ENABLE_RBAC=false APISERVER_CLIENT_CA_CONFIG_NAME=extension-apiserver-authentication APISERVER_CLIENT_CA_CONFIG_NAMESPACE=kube-system VERSION=$(VERSION) NAMESPACE=$(NAMESPACE) java $(JAVA_OPTS) -jar target/api-server-$(MAVEN_VERSION).jar
