include ../../Makefile.java.mk

ifneq ($(SKIP_QUARKUS_NATIVE_IMAGE),true)
MAVEN_PACKAGE_ARGS += -Pnative -Dquarkus.native.container-runtime=$(QUARKUS_CONTAINER_RUNTIME)
endif
