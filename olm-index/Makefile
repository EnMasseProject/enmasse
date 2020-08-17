include ../Makefile.common

image_deps:: Dockerfile

Dockerfile:
	opm index add --bundles $(OLM_MANIFEST_IMAGE) --from-index $(OLM_INDEX_IMAGE_PREVIOUS) --generate -d Dockerfile

.PHONY: Dockerfile
