package io.enmasse.controller.api.v1.http;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.controller.api.DefaultExceptionMapper;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HttpAddressSpaceServiceTest {
    private HttpAddressSpaceService addressSpaceService;
    private TestAddressSpaceApi addressSpaceApi;
    private AddressSpace a1;
    private AddressSpace a2;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();

    @Before
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        addressSpaceService = new HttpAddressSpaceService(addressSpaceApi);
        a1 = new AddressSpace.Builder()
                .setName("a1")
                .setType(new StandardAddressSpaceType())
                .setEndpointList(Arrays.asList(
                        new Endpoint.Builder()
                            .setName("messaging")
                            .setService("messaging")
                            .setHost("messaging.example.com")
                        .build(),
                        new Endpoint.Builder()
                            .setName("mqtt")
                            .setService("mqtt")
                            .setHost("mqtt.example.com")
                        .build()))
                .build();

        a2 = new AddressSpace.Builder()
                .setName("a2")
                .setType(new StandardAddressSpaceType())
                .setNamespace("othernamespace")
                .build();
    }

    private Response invoke(Callable<Response> fn) {
        try {
            return fn.call();
        } catch (Exception e) {
            return exceptionMapper.toResponse(e);
        }
    }

    @Test
    public void testList() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList());
        assertThat(response.getStatus(), is(200));
        AddressSpaceList data = (AddressSpaceList) response.getEntity();

        assertThat(data.size(), is(2));
        assertThat(data, hasItem(a1));
        assertThat(data, hasItem(a2));
    }

    @Test
    public void testListException() {
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.getAddressSpaceList());
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.getAddressSpace("a1"));
        assertThat(response.getStatus(), is(200));
        AddressSpace data = ((AddressSpace)response.getEntity());

        assertThat(data, is(a1));
        assertThat(data.getEndpoints().size(), is(a1.getEndpoints().size()));
    }

    @Test
    public void testGetException() {
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.getAddressSpace("a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = invoke(() -> addressSpaceService.getAddressSpace("doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testCreate() {
        Response response = invoke(() -> addressSpaceService.createAddressSpace(a1));
        assertThat(response.getStatus(), is(200));

        assertThat(addressSpaceApi.listAddressSpaces(), hasItem(a1));
    }

    @Test
    public void testCreateException() {
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.createAddressSpace(a1));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.createAddressSpace(a2);

        Response response = invoke(() -> addressSpaceService.deleteAddressSpace("a1"));
        assertThat(response.getStatus(), is(200));

        assertThat(addressSpaceApi.listAddressSpaces(), hasItem(a2));
        assertThat(addressSpaceApi.listAddressSpaces().size(), is(1));
    }

    @Test
    public void testDeleteException() {
        addressSpaceApi.createAddressSpace(a1);
        addressSpaceApi.throwException = true;
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace("a1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDeleteNotFound() {
        addressSpaceApi.createAddressSpace(a1);
        Response response = invoke(() -> addressSpaceService.deleteAddressSpace("doesnotexist"));
        assertThat(response.getStatus(), is(404));
    }
}
