include ../Makefile.java.mk
JAVA_OPTS=-Djava.security.egd=file:/dev/./urandom -Xms8m -Xmx8m -XX:+UseParallelGC -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MaxMetaspaceSize=100m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=heapdumps
run:
	VERSION=$(VERSION) \
			API_SERVER_CLIENT_CA_CONFIG_NAME=extension-apiserver-authentication \
			API_SERVER_CLIENT_CA_CONFIG_NAMESPACE=kube-system \
			STANDARD_AUTHSERVICE_CERT_SECRET_NAME=standard-authservice-cert \
			STANDARD_AUTHSERVICE_CREDENTIALS_SECRET_NAME=keycloak-credentials \
			STANDARD_AUTHSERVICE_CONFIG_NAME=keycloak-config \
			NAMESPACE=$(NAMESPACE) \
			java $(JAVA_OPTS) -jar target/api-server-$(MAVEN_VERSION).jar
