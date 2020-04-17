
# Example with external PostgreSQL device registry

## Install PostgreSQL

    oc new-project device-registry-storage
    oc new-app postgresql:10 -ePOSTGRESQL_USER=registry -ePOSTGRESQL_PASSWORD=user12 -ePOSTGRESQL_DATABASE=device-registry -ePOSTGRESQL_ADMIN_PASSWORD=admin1234
    oc set volume dc/postgresql --add -m /var/lib/pgsql/data -t pvc --claim-size 10G --overwrite

## Deploy SQL schema

### For device connection service

	oc -n device-registry-storage rsh deployment/postgresql bash -c "PGPASSWORD=user12 psql -h postgresql device-registry registry" < create.devcon.sql

### For device registry (flat JSON)

    oc -n device-registry-storage rsh deployment/postgresql bash -c "PGPASSWORD=user12 psql -h postgresql device-registry registry" < create.sql

### For device registry (tree JSON)

    oc -n device-registry-storage rsh deployment/postgresql bash -c "PGPASSWORD=user12 psql -h postgresql device-registry registry" < create.sql
    oc -n device-registry-storage rsh deployment/postgresql bash -c "PGPASSWORD=user12 psql -h postgresql device-registry registry" < create.tree.sql

### For device registry (table)

    oc -n device-registry-storage rsh deployment/postgresql bash -c "PGPASSWORD=user12 psql -h postgresql device-registry registry" < create.table.sql

## Enter the PostgreSQL container

    oc -n device-registry-storage rsh deployment/postgresql bash -c "PGPASSWORD=user12 psql -h postgresql device-registry registry"
