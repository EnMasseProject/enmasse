include ../Makefile.common

CMDTOOL=oc
ifeq (, $(shell which oc))
CMDTOOL=kubectl
ifeq (, $(shell which kubectl))
$(error "No oc or kubectl in $(PATH)")
endif
endif

ifeq (oc, $(CMDTOOL))
KUBERNETES_NAMESPACE  ?= $(shell oc project -q)
KUBERNETES_API_TOKEN  ?= $(shell oc whoami -t)
KUBERNETES_API_URL    ?= $(shell oc whoami --show-server=true)
else
KUBERNETES_API_URL=$(shell kubectl config view --minify | grep server | cut -f 2- -d ":" | tr -d " ")
KUBERNETES_API_TOKEN=$(shell kubectl describe secret $(kubectl get secrets | grep ^default | cut -f1 -d ' ') | grep -E '^token' | cut -f2 -d':' | tr -d " ")

ifeq (, $(KUBERNETES_NAMESPACE))
$(error "Environment variable $$KUBERNETES_NAMESPACE not set")
endif
ifeq (, $(KUBERNETES_API_TOKEN))
$(error "Environment variable $$KUBERNETES_API_TOKEN not set")
endif
ifeq (, $(KUBERNETES_API_URL))
$(error "Environment variable $$KUBERNETES_API_URL not set")
endif
endif

all: systemtests

systemtests:
	KUBERNETES_NAMESPACE=$(KUBERNETES_NAMESPACE) \
		KUBERNETES_API_TOKEN=$(KUBERNETES_API_TOKEN) \
		KUBERNETES_API_URL=$(KUBERNETES_API_URL) \
		./scripts/run_tests.sh $(SYSTEMTEST_PROFILE) $(SYSTEMTEST_ARGS)
