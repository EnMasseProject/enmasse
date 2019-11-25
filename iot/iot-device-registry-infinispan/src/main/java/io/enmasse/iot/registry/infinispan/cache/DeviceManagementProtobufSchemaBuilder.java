/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.cache;

import io.enmasse.iot.registry.infinispan.device.data.CredentialKey;
import io.enmasse.iot.registry.infinispan.device.data.DeviceCredential;
import io.enmasse.iot.registry.infinispan.device.data.DeviceInformation;
import io.enmasse.iot.registry.infinispan.device.data.DeviceKey;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
        includeClasses = {
                CredentialKey.class,
                DeviceCredential.class,
                DeviceInformation.class,
                DeviceKey.class,
        },
        schemaFileName = "deviceRegistry.proto",
        schemaFilePath = "proto/",
        schemaPackageName = "io.enmasse.iot.registry.infinispan.data")
public interface DeviceManagementProtobufSchemaBuilder extends SerializationContextInitializer {

}
