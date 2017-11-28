include ../Makefile.common

ASCIIDOCTOR ?= $(shell command -v asciidoctor 2> /dev/null)

build: npm_install www/tooltips.json www/help.html www/favicon.ico

www/tooltips.json: ../doc-utils/parse.py ../doc-utils/expected.json ../documentation/common/*
	../doc-utils/parse.py -i ../doc-utils/expected.json -o www/tooltips.json -v ../documentation/common/

www/help.html:  ../documentation/messaging_app_developer/*.adoc ../documentation/common/*.adoc
        ifeq ($(ASCIIDOCTOR),)
		$(warning "asciidoctor not found, help pages cannot be generated")
        else
		asciidoctor -d book -B ../documentation/messaging_app_developer/ -o ../../agent/www/help.html ../documentation/messaging_app_developer/master.adoc
        endif

www/favicon.ico:
	cp -f ../documentation/images/favicon.ico www/

package: build
	tar -czf build/agent-$(VERSION).tgz bin lib www package.json

test: npm_test

coverage: npm_coverage
