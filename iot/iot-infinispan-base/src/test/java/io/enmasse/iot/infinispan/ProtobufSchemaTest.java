/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.infinispan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsIterableContaining.hasItems;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.junit.jupiter.api.Test;

import io.enmasse.iot.infinispan.cache.DeviceConnectionProtobufSchemaBuilderImpl;
import io.enmasse.iot.infinispan.cache.DeviceManagementProtobufSchemaBuilderImpl;
import io.enmasse.iot.infinispan.devcon.DeviceConnectionKey;
import io.enmasse.iot.infinispan.device.DeviceInformation;

public class ProtobufSchemaTest {

    private TestSerializationContext ctx = new TestSerializationContext();

    @Test
    public void testDeviceConnectionKeyPackageName() {
        new DeviceConnectionProtobufSchemaBuilderImpl().registerMarshallers(this.ctx);

        assertThat(
                this.ctx.getMarshallers(),
                hasItems(
                        hasProperty("typeName", is(DeviceConnectionKey.class.getName()))));
    }

    @Test
    public void testDeviceInformationPackageName() {
        new DeviceManagementProtobufSchemaBuilderImpl().registerMarshallers(this.ctx);
        assertThat(
                this.ctx.getMarshallers(),
                hasItems(
                        hasProperty("typeName", is(DeviceInformation.class.getName()))));
    }

}


final class TestSerializationContext implements SerializationContext {
    private List<BaseMarshaller<?>> marshallers = new LinkedList<>();

    public List<BaseMarshaller<?>> getMarshallers() {
        return marshallers;
    }

    @Override
    public Configuration getConfiguration() {
        return null;
    }

    @Override
    public Map<String, FileDescriptor> getFileDescriptors() {
        return null;
    }

    @Override
    public Map<String, GenericDescriptor> getGenericDescriptors() {
        return null;
    }

    @Override
    public Descriptor getMessageDescriptor(String fullTypeName) {
        return null;
    }

    @Override
    public EnumDescriptor getEnumDescriptor(String fullTypeName) {
        return null;
    }

    @Override
    public boolean canMarshall(Class<?> javaClass) {
        return false;
    }

    @Override
    public boolean canMarshall(String fullTypeName) {
        return false;
    }

    @Override
    public <T> BaseMarshaller<T> getMarshaller(String fullTypeName) {
        return null;
    }

    @Override
    public <T> BaseMarshaller<T> getMarshaller(Class<T> clazz) {
        return null;
    }

    @Override
    public String getTypeNameById(Integer typeId) {
        return null;
    }

    @Override
    public Integer getTypeIdByName(String fullTypeName) {
        return null;
    }

    @Override
    public GenericDescriptor getDescriptorByTypeId(Integer typeId) {
        return null;
    }

    @Override
    public GenericDescriptor getDescriptorByName(String fullTypeName) {
        return null;
    }

    @Override
    public void registerProtoFiles(FileDescriptorSource source) throws DescriptorParserException {
    }

    @Override
    public void unregisterProtoFile(String fileName) {
    }

    @Override
    public void registerMarshaller(BaseMarshaller<?> marshaller) {
        this.marshallers .add(marshaller);
    }

    @Override
    public void unregisterMarshaller(BaseMarshaller<?> marshaller) {
    }

    @Override
    public void registerMarshallerProvider(MarshallerProvider marshallerProvider) {
    }

    @Override
    public void unregisterMarshallerProvider(MarshallerProvider marshallerProvider) {
    }
}
