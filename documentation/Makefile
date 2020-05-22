include ../Makefile.java.mk

build_deps:: swagger openapi check

.PHONY: swagger openapi
swagger: build/swagger2markup.jar
	mvn process-resources
	java -jar build/swagger2markup.jar convert -i target/classes/swagger.json -f common/restapi-reference

build/swagger2markup.jar:
	mkdir -p $(dir $@)
	curl -m600 -o $@ https://repo.maven.apache.org/maven2/io/github/swagger2markup/swagger2markup-cli/1.3.3/swagger2markup-cli-1.3.3.jar

openapi: build/openapi-generator-cli.jar
	java -jar build/openapi-generator-cli.jar generate --input-spec src/main/resources/eclipse-hono.yaml --output build/eclipse-hono --generator-name asciidoc -pheaderAttributes=false
	cp build/eclipse-hono/index.adoc common/device-management-reference.adoc

build/openapi-generator-cli.jar:
	mkdir -p $(dir $@)
	curl -m600 -o $@ https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/4.3.1/openapi-generator-cli-4.3.1.jar

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
