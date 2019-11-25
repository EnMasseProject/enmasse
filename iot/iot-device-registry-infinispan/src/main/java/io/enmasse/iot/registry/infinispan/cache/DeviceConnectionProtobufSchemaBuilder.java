/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.registry.infinispan.cache;

import io.enmasse.iot.registry.infinispan.devcon.DeviceConnectionKey;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
        includeClasses = {
                DeviceConnectionKey.class,
        },
        schemaFileName = "deviceConnection.proto",
        schemaFilePath = "proto/",
        schemaPackageName = "io.enmasse.iot.registry.infinispan.data")
public interface DeviceConnectionProtobufSchemaBuilder extends SerializationContextInitializer {
}
