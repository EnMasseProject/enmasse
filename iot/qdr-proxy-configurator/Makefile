include ../../Makefile.common

CMD=qdr-proxy-configurator

build/$(CMD):
	cd $(GOPRJ)/cmd/$(@F) && go build -o $(abspath $@) .

build: build/$(CMD)

package:
	@echo "Nothing to package"

test:

.PHONY: build/$(CMD)