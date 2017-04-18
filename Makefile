TAG?=latest

ifdef TRAVIS_TAG
	TAG=$(TRAVIS_TAG)
endif

all:
	tar -czf ragent-$(TAG).tar.gz *.js

clean:
	rm -rf ragent-$(TAG).tar.gz
