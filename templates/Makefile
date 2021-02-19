# WARNING: this file has the be included last, otherwise the whole
# build silently fails since VERSION (etc) and TOPDIR are defined
# as a recursive variable and based on the location on the "last"
# included makefile, which might change over time, processing other
# makefiles.
include ../Makefile.common

export BUILDDIR
package:
	BUILDDIR=build/default INSTALLNAME=enmasse-$(TAG) CRDVER=v1 $(MAKE) --makefile=Makefile.bundle.mk package
	BUILDDIR=build/prekube1_16 INSTALLNAME=enmasse-$(TAG) CRDVER=v1beta1 $(MAKE) --makefile=Makefile.bundle.mk package

coverage:

.PHONY: prepare package clean $(ENMASSE_BUNDLE)
