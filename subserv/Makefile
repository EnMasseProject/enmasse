TAG?=latest

ifdef TRAVIS_TAG
	TAG=$(TRAVIS_TAG)
endif

all:
	tar -czf subserv-$(TAG).tar.gz *.js

clean:
	rm -rf subserv-$(TAG).tar.gz
