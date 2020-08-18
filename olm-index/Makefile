include ../Makefile.common

OPM_ARGS ?= --skip-tls -u docker -p docker

image_deps:: Dockerfile

Dockerfile:
	opm index add --bundles $(OLM_MANIFEST_IMAGE) --from-index $(OLM_INDEX_IMAGE_PREVIOUS) --generate -d Dockerfile $(OPM_ARGS)

.PHONY: Dockerfile
