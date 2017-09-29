package io.enmasse.controller.api.v1.http;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.address.model.types.standard.StandardType;
import io.enmasse.k8s.api.TestAddressApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HttpAddressServiceTest {
    private HttpAddressService addressService;
    private TestAddressSpaceApi addressSpaceApi;
    private TestAddressApi addressApi;
    private Address q1;
    private Address a1;

    @Before
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        this.addressService = new HttpAddressService(addressSpaceApi);

        AddressSpace addressSpace = new AddressSpace.Builder()
                .setName("myspace")
                .setType(new StandardAddressSpaceType())
                .build();
        addressSpaceApi.createAddressSpace(addressSpace);
        addressApi = (TestAddressApi) addressSpaceApi.withAddressSpace(addressSpace);
        q1 = new Address.Builder()
                .setName("q1")
                .setType(StandardType.QUEUE)
                .build();
        a1 = new Address.Builder()
                .setName("a1")
                .setType(StandardType.ANYCAST)
                .build();
        addressApi.createAddress(q1);
        addressApi.createAddress(a1);
    }

    @Test
    public void testList() {
        Response response = addressService.getAddressList("myspace");

        assertThat(response.getStatus(), is(200));
        AddressList list = (AddressList) response.getEntity();

        assertThat(list.size(), is(2));
        assertThat(list, hasItem(q1));
        assertThat(list, hasItem(a1));
    }

    @Test
    public void testListException() {
        addressApi.throwException = true;
        Response response = addressService.getAddressList("myspace");
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGet() {
        Response response = addressService.getAddress("myspace", "q1");
        assertThat(response.getStatus(), is(200));
        Address address = (Address) response.getEntity();

        assertThat(address, is(q1));
    }

    @Test
    public void testGetException() {
        addressApi.throwException = true;
        Response response = addressService.getAddress("myspace", "q1");
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetUnknown() {
        Response response = addressService.getAddress("unknownspace", "q1");
        assertThat(response.getStatus(), is(404));

        response = addressService.getAddress("myspace", "doesnotexist");
        assertThat(response.getStatus(), is(404));
    }


    @Test
    public void testCreate() {
        Address a2 = new Address.Builder()
                .setName("a2")
                .setType(StandardType.ANYCAST)
                .setAddressSpace("myspace")
                .build();
        AddressList list = new AddressList();
        list.add(a2);
        Response response = addressService.appendAddresses("myspace", list);
        assertThat(response.getStatus(), is(200));

        assertThat(addressApi.listAddresses(), hasItem(a2));
    }

    @Test
    public void testCreateException() {
        addressApi.throwException = true;
        Address a2 = new Address.Builder()
                .setName("a2")
                .setType(StandardType.ANYCAST)
                .build();
        AddressList list = new AddressList();
        list.add(a2);
        Response response = addressService.appendAddresses("myspace", list);
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testDelete() {
        Response response = addressService.deleteAddress("myspace", "a1");
        assertThat(response.getStatus(), is(200));

        assertThat(addressApi.listAddresses(), hasItem(q1));
        assertThat(addressApi.listAddresses().size(), is(1));
    }

    @Test
    public void testDeleteException() {
        addressApi.throwException = true;
        Response response = addressService.deleteAddress("myspace", "a1");
        assertThat(response.getStatus(), is(500));
    }
}
