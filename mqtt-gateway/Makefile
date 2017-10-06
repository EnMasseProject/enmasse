INTEGRATION_TEST=true
include ../Makefile.java.mk

setup_router:
	$(DOCKER) run -d --name qdrouterd -p 5672:5672 -p 55673:55673 -v ${CURDIR}/src/test/resources:/conf:z ppatierno/qdrouterd:0.8.0-repo qdrouterd --conf /conf/qdrouterd.conf

teardown_router:
	docker stop qdrouterd
	docker rm qdrouterd


.PHONY: setup_router teardown_router
