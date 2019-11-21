/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.infinispan.cache;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

import io.enmasse.iot.infinispan.device.CredentialKey;
import io.enmasse.iot.infinispan.device.DeviceCredential;
import io.enmasse.iot.infinispan.device.DeviceInformation;
import io.enmasse.iot.infinispan.device.DeviceKey;

@AutoProtoSchemaBuilder(
        includeClasses = {
                CredentialKey.class,
                DeviceCredential.class,
                DeviceInformation.class,
                DeviceKey.class,
        },
        schemaFileName = "deviceRegistry.proto",
        schemaFilePath = "proto/",
        schemaPackageName = "io.enmasse.iot.infinispan.device")
public interface DeviceManagementProtobufSchemaBuilder extends SerializationContextInitializer {

}
