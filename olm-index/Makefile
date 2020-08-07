include ../Makefile.java.mk

image_deps:: Dockerfile

Dockerfile:
	cat Dockerfile.template | sed -e 's,\$${OLM_MANIFEST_IMAGE},$(OLM_MANIFEST_IMAGE),g' -e 's,\$${OLM_INDEX_IMAGE_PREVIOUS},$(OLM_INDEX_IMAGE_PREVIOUS),g' -e 's,\$${OLM_VERSION},$(OLM_VERSION),g' > Dockerfile

.PHONY: Dockerfile
