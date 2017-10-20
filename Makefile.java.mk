TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.common

ifneq ($(FULL_BUILD),true)
build:
	mvn compile

test:
	mvn test

package:
	mvn package -DskipTests
endif

clean_java: 
	rm -rf build target

clean: clean_java
