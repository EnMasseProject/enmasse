include ../Makefile.common

test_go_vet:
	go vet ./...

ifeq (,$(GO2XUNIT))
test_go_run: 
	go test -v ./...
else
test_go_run:
	go test -v ./... 2>&1 | tee $(abspath build/go.testoutput)
	$(GO2XUNIT) -fail -input build/go.testoutput -output build/TEST-go.xml
endif

coverage:
	go test -cover .

test: test_go_vet test_go_run
