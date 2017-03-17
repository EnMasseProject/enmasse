# EnMasse CI

EnMasse uses [Travis CI](http://travis-ci.org/) for continuous integration (CI). All components
in EnMasse is built, unit tested and dockerized in each individual repository. At the end of each
build, the docker image is pushed to [Docker Hub](https://hub.docker.com/r/enmasseproject/) and the
[systemtest](https://github.com/EnMasseProject/systemtests/) are run using the latest set of docker
images.

![Overview](ci.png)
