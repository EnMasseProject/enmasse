include ../../Makefile.common

CMD=iot-gc

build/$(CMD):
	cd $(GOPRJ)/cmd/$(@F) && go build -o $(abspath $@) .

build: build/$(CMD)

package:
	@echo "Nothing to package"

test:

.PHONY: build/$(CMD)