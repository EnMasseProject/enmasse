# External support with H2

The JDBC based device registry supports PostgreSQL out of the box. However it is possible
to configure the external JDBC mode to run with other JDBC compliant databases as well.

For this you need to provide the JDBC driver JAR file, and maybe override the SQL
statement configuration. This example shows how to do this with H2.

The files for building the references container images can be found at: https://github.com/EnMasseProject/h2-iot-container-images

## Install H2

    oc new-project device-registry-storage
    oc deploy -n device-registry-storage -f deploy/

## Deploy the SQL schema

    oc rsh -n device-registry-storage deployment/h2 sh -c "java -cp h2.jar org.h2.tools.Shell -user admin -password admin1234 -url jdbc:h2:tcp://localhost:9092//data/device-registry" < create.sql

## Configure the IoTConfig

You will need to configure the external JDBC connection to H2 and also switch to `TABLE` mode.

In order to inject the JDBC JAR file, and the H2 specific SQL configuration, you will need to
provide an extension image, which copies two additional JAR files to the `/ext` folder, which
will be later on used by the device registry.

**Note:** You need to drop in all required dependencies of the JDBC driver as well.

Also see: [iot-config.yaml](iot-config.yaml)

## Enter the SQL shell

You can access a SQL shell to the H2 database by executing:

    oc rsh -n device-registry-storage deployment/h2 sh -c "java -cp h2.jar org.h2.tools.Shell -user admin -password admin1234 -url jdbc:h2:tcp://localhost:9092//data/device-registry"
