include ../Makefile.common

build: npm_install www/tooltips.json www/help.html

www/tooltips.json: ../doc-utils/parse.py ../doc-utils/expected.json ../documentation/modules/*
	../doc-utils/parse.py -i ../doc-utils/expected.json -o www/tooltips.json -v ../documentation/modules/

www/help.html:  ../documentation/master.adoc ../documentation/modules/*.adoc ../documentation/common/*.adoc
	asciidoctor -d book -B ../documentation/ -o ../console/www/help.html ../documentation/master.adoc

package: build
	tar -czf build/console.tgz bin lib www package.json

test: npm_test

coverage: npm_coverage
