include ../Makefile.common

ASCIIDOCTOR ?= $(shell command -v asciidoctor 2> /dev/null)

build: npm_install www/tooltips.json www/help.html

www/tooltips.json: ../doc-utils/parse.py ../doc-utils/expected.json ../documentation/common/*
	../doc-utils/parse.py -i ../doc-utils/expected.json -o www/tooltips.json -v ../documentation/common/

www/help.html:  ../documentation/common/*.adoc
        ifeq ($(ASCIIDOCTOR),)
		$(warning "asciidoctor not found, help pages cannot be generated")
        else
			# TODO: Reenable when we have help documentation
	       	#asciidoctor -d book -B ../documentation/ -o ../console/www/help.html ../documentation/master.adoc
        endif

package: build
	tar -czf build/console.tgz bin lib www package.json

test: npm_test

coverage: npm_coverage
