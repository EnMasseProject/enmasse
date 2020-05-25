include ../Makefile.java.mk

build_deps:: swagger check

.PHONY: swagger
swagger: build/swagger2markup.jar
	mvn process-resources
	java -jar build/swagger2markup.jar convert -i target/classes/swagger.json -f common/restapi-reference

build/swagger2markup.jar:
	mkdir -p build
	curl -o build/swagger2markup.jar https://repo.maven.apache.org/maven2/io/github/swagger2markup/swagger2markup-cli/1.3.3/swagger2markup-cli-1.3.3.jar

ifdef PUBLISH_DEST
.PHONY: publish
publish: build
	rsync -av target/generated-docs/html/* $(PUBLISH_DEST)/
else
.PHONY: publish
publish:
	@echo "PUBLISH_DEST not set"
endif

.PHONY: check
check:
	cd $(TOPDIR) && $(TOPDIR)/scripts/check_docs.sh
