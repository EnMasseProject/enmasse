TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
MVNPROJ=$(shell realpath --relative-to="$(realpath $(TOPDIR))" "$(shell pwd)")
include $(TOPDIR)/Makefile.common
ifeq ($(SKIP_TESTS),true)
MAVEN_ARGS="-DskipTests"
endif

ifneq ($(FULL_BUILD),true)
build:
	cd $(TOPDIR); $(IMAGE_ENV) mvn -pl $(MVNPROJ) -am clean install $(MAVEN_ARGS)

test:
ifeq ($(SKIP_TESTS),true)
	$(warning "java tests will be skipped")
else
	mvn test $(MAVEN_ARGS)
endif

package_java:
	$(IMAGE_ENV) mvn package -DskipTests $(MAVEN_ARGS)

package: package_java
endif

ifneq ($(FULL_BUILD),true)
clean_java: 
	mvn clean $(MAVEN_ARGS)
	rm -rf build target
else
clean_java:
	rm -rf build target
endif

clean: clean_java
