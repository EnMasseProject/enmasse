#!/bin/sh

#BINTRAY_DIR = releases | snapshots
curl -T address-space-controller/target/address-space-controller-${VERSION}.jar -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T artemis/build/artemis-image-${VERSION}.tar.gz -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T agent/build/agent-${VERSION}.tgz -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T keycloak/build/keycloak-${VERSION}.tar.gz -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T keycloak-controller/target/keycloak-controller-${VERSION}.jar -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T mqtt-gateway/target/mqtt-gateway-${VERSION}.jar -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T mqtt-lwt/target/mqtt-lwt-${VERSION}.jar -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T none-authservice/build/none-authservice-${VERSION}.tgz -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T router/build/router-${VERSION}.tgz -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T ragent/build/ragent-${VERSION}.tgz -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T router-metrics/router-metrics-${VERSION}.tgz -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T subserv/build/subserv-${VERSION}.tgz -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
curl -T topic-forwarder/target/topic-forwarder-${VERSION}.jar -u ${BINTRAY_USER}:${BINTRAY_PASSWORD} -H "X-Bintray-Package:enmasse" -H "X-Bintray-Version:${VERSION_BIN}" -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" https://api.bintray.com/content/enmasse/${BINTRAY_DIR}/
