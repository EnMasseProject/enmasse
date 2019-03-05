include ../Makefile.common

CMD=enmasse-controller-manager

build/$(CMD):
	cd $(GOPRJ)/cmd/$(@F) && go build -o $(abspath $@) .

build: build/$(CMD)

package:
	@echo "Nothing to package"

test:

.PHONY: build/$(CMD)
