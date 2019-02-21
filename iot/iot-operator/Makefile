include ../../Makefile.common

CMD=iot-operator

build/$(CMD):
	cd $(GOPRJ)/cmd/$(@F) && go build -o $(abspath $@) .

build: build/$(CMD)

package:
	@echo "Nothing to package"

test:

.PHONY: build/$(CMD)