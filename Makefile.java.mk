TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.common

ifneq ($(FULL_BUILD),true)
build:
	gradle compileJava

test:
	gradle compileTestJava

package:
	gradle build
endif
