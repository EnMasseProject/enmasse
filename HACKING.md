# Building

For building and running EnMasse unit tests:

    ./gradlew build

If you want to rerun a task even if nothing has changed, append the `--rerun-tasks` flag to gradle.
To get more verbose information, append `-i`.

To build a docker image and push them to docker hub:

    DOCKER_ORG=myorg DOCKER_USER=myuser DOCKER_PASS=mypassword ./gradlew pack buildImage tagImage pushImage

To deploy a built EnMasse to an OpenShift instance:

    OPENSHIFT_MASTER=https://localhost:8443 OPENSHIFT_PROJECT=myproject OPENSHIFT_USER=developer OPENSHIFT_PASSWD=developer ./gradlew deploy

To run systemtests:

    cd systemtests
    OPENSHIFT_MASTER=https://localhost:8443 OPENSHIFT_PROJECT=myproject OPENSHIFT_TOKEN=`oc whoami -t` gradle check -i --rerun-tasks
