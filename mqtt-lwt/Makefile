INTEGRATION_TEST=true
include ../Makefile.java.mk

setup_router:
	$(DOCKER) run -d --name qdrouterd-lwt -p 5673:5672 -p 55671:55671 -p 56671:56671 -v ${CURDIR}/src/test/resources:/conf:z ppatierno/qdrouterd:0.8.0-repo qdrouterd --conf /conf/qdrouterd.conf

teardown_router:
	$(DOCKER) stop qdrouterd-lwt
	$(DOCKER) rm qdrouterd-lwt

.PHONY: setup_router teardown_router
