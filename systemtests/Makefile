include ../Makefile.common

CMDTOOL=oc
ifeq (, $(shell which oc))
CMDTOOL=kubectl
ifeq (, $(shell which kubectl))
$(error "No oc or kubectl in $(PATH)")
endif
endif

PROFILE ?= systemtests
PIPELINE_NAMESPACE ?= enmasse-pipelines
KUBERNETES_NAMESPACE ?= enmasse-infra

ifeq (oc, $(CMDTOOL))
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

ifneq (, $(wildcard .env))
include .env
endif

all: systemtests

systemtests:
		KUBERNETES_API_TOKEN=$(KUBERNETES_API_TOKEN) \
		KUBERNETES_API_URL=$(KUBERNETES_API_URL) \
		./scripts/run_tests.sh '$(PROFILE)' '$(TESTCASE)'

systemtests-tekton:
		$(CMDTOOL) create namespace $(PIPELINE_NAMESPACE) || true
		$(CMDTOOL) apply -f ../.tekton/test/ -n $(PIPELINE_NAMESPACE)
		tkn pipeline start systemtest-pipeline \
				--param kube_api_url=$(KUBERNETES_API_URL) \
				--param kube_token=$(KUBERNETES_API_TOKEN) \
				--param kube_namespace=$(KUBERNETES_NAMESPACE) \
				--param profile=$(PROFILE) \
				--param testcase='$(TESTCASE)' \
				--showlog -n $(PIPELINE_NAMESPACE)
