include ../Makefile.java.mk
JAVA_OPTS=-Djava.security.egd=file:/dev/./urandom -Xms256m -Xmx256m -XX:+UseParallelGC -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MaxMetaspaceSize=100m -XX:+ExitOnOutOfMemoryError
run:
	NAMESPACE=$(NAMESPACE) java $(JAVA_OPTS) -jar target/api-server-$(VERSION).jar
