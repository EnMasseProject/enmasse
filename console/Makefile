include ../Makefile.common

build:
	npm install

package:
	tar -czf build/console.tgz bin lib www package.json

test: npm_test

coverage: npm_install
        ifndef NPM
		$(warning "npm not found, coverage cannot be generated")
        else
	       npm run coverage
        endif
