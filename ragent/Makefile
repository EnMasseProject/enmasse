include ../Makefile.common

build:
	@echo "Nothing to build"

package:
	tar -czf build/ragent.tgz *.js

test: npm_test

coverage: npm_install
        ifndef NPM
		$(warning "npm not found, coverage cannot be generated")
        else
	       npm run coverage
        endif
