include ../Makefile.java.mk
TEMPLATE_DIR=$(shell mktemp -d)
run:
	oc extract -n $(NAMESPACE) configmap/address-space-definitions --to=$(TEMPLATE_DIR)
	TEMPLATE_DIR=$(TEMPLATE_DIR) NAMESPACE=$(NAMESPACE) java -jar target/address-space-controller-$(VERSION).jar
