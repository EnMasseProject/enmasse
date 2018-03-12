TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.common
ifeq ($(SKIP_TESTS),true)
MAVEN_ARGS="-DskipTests"
endif

ifneq ($(FULL_BUILD),true)
build:
	cd $(TOPDIR); mvn install $(MAVEN_ARGS)

test:
ifeq ($(SKIP_TESTS),true)
	$(warning "java tests will be skipped")
else
	mvn test $(MAVEN_ARGS)
endif

package_java:
	mvn package -DskipTests $(MAVEN_ARGS)

package: package_java
endif

clean_java: 
	mvn clean $(MAVEN_ARGS)
	rm -rf build target

clean: clean_java
