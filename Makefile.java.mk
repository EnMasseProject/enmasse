TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.common

ifneq ($(FULL_BUILD),true)
build:
	mvn compile

test:
	mvn test

package_java:
	mvn package -DskipTests

package: package_java
endif

clean_java: 
	rm -rf build target

clean: clean_java
