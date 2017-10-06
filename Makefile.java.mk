TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.common

ifneq ($(FULL_BUILD),true)
build:
	mvn compile

ifeq ($(INTEGRATION_TEST), true)
test: setup_router
	mvn test
	$(MAKE) teardown_router
else
test:
	mvn test
endif

package:
	mvn package -DskipTests
endif

ifeq ($(INTEGRATION_TEST), true)
clean_java: teardown_router
	rm -rf build target
else
clean_java: 
	rm -rf build target
endif

clean: clean_java
