/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.api.common.DefaultExceptionMapper;
import io.enmasse.api.common.Status;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.model.v1beta1.Table;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationBuilder;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.enmasse.user.model.v1.UserBuilder;
import io.enmasse.user.model.v1.UserList;
import io.enmasse.user.model.v1.UserSpecBuilder;

public class HttpUserServiceTest {
    private ObjectMapper mapper = new ObjectMapper();
    private HttpUserService userService;
    private TestAddressSpaceApi addressSpaceApi;
    private TestUserApi userApi;
    private User u1;
    private User u2;
    private DefaultExceptionMapper exceptionMapper = new DefaultExceptionMapper();
    private SecurityContext securityContext;

    @BeforeEach
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        userApi = new TestUserApi();
        securityContext = mock(SecurityContext.class);
        when(securityContext.isUserInRole(any())).thenReturn(true);

        AuthenticationServiceRegistry authenticationServiceRegistry = mock(AuthenticationServiceRegistry.class);
        AuthenticationService authenticationService = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName("standard")
                .endMetadata()
                .withNewSpec()
                .withType(AuthenticationServiceType.standard)
                .endSpec()
                .withNewStatus()
                .withHost("example")
                .withPort(5671)
                .endStatus()
                .build();
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(authenticationService));
        when(authenticationServiceRegistry.resolveDefaultAuthenticationService()).thenReturn(Optional.of(authenticationService));
        when(authenticationServiceRegistry.listAuthenticationServices()).thenReturn(Collections.singletonList(authenticationService));

        this.userService = new HttpUserService(addressSpaceApi, userApi, authenticationServiceRegistry, Clock.systemUTC());
        addressSpaceApi.createAddressSpace(new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("myspace")
                .withNamespace("ns1")
                .addToAnnotations(AnnotationKeys.REALM_NAME, "r1")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()

                .build());

        addressSpaceApi.createAddressSpace(new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("otherspace")
                .withNamespace("ns2")
                .addToAnnotations(AnnotationKeys.REALM_NAME, "r2")
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")
                .endSpec()
                .build());

        u1 = new UserBuilder()
                .withNewMetadata()
                .withName("myspace.user1")
                .withNamespace("ns1")
                .endMetadata()

                .withSpec(new UserSpecBuilder()
                        .withUsername("user1")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build(),
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("direct*"))
                                        .withOperations(Arrays.asList(Operation.view))
                                        .build()))
                        .build())
                .build();

        u2 = new UserBuilder()
                .withNewMetadata()
                .withName("otherspace.user2")
                .withNamespace("ns2")
                .endMetadata()

                .withSpec(new UserSpecBuilder()
                        .withUsername("user2")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("pswd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withOperations(Arrays.asList(Operation.manage))
                                        .build()))
                        .build())
                .build();

        userApi.createUser(authenticationService, "r1", u1);
        userApi.createUser(authenticationService, "r2", u2);
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
        Response response = invoke(() -> userService.getUserList(securityContext, null, "ns1", null));

        assertThat(response.getStatus(), is(200));
        UserList list = (UserList) response.getEntity();

        assertThat(list.getItems().size(), is(1));
        assertThat(list.getItems(), hasItem(u1));
    }

    @Test
    public void testListTable() {
        Response response = invoke(() -> userService.getUserList(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "ns1", null));

        assertThat(response.getStatus(), is(200));
        Table table = (Table) response.getEntity();

        assertThat(table.getColumnDefinitions().size(), is(4));
        assertThat(table.getRows().size(), is(1));
    }

    @Test
    public void testGetByUser() {
        Response response = invoke(() -> userService.getUser(securityContext, null, "ns1", "myspace.user1"));

        assertThat(response.getStatus(), is(200));
        User user = (User) response.getEntity();

        assertThat(user, is(u1));
    }

    @Test
    public void testGetByUserTable() {
        Response response = invoke(() -> userService.getUser(securityContext, "application/json;as=Table;g=meta.k8s.io;v=v1beta1", "ns1", "myspace.user1"));

        Table table = (Table) response.getEntity();

        assertThat(response.getStatus(), is(200));
        assertThat(table.getColumnDefinitions().size(), is(4));
        assertThat(table.getRows().get(0).getObject().getMetadata().getName(), is(u1.getMetadata().getName()));
    }

    @Test
    public void testGetByUserNotFound() {
        Response response = invoke(() -> userService.getUser(securityContext, null, "ns1", "myspace.user2"));

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testListException() {
        userApi.throwException = true;
        Response response = invoke(() -> userService.getUserList(securityContext, null, "ns1", null));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testGetException() {
        userApi.throwException = true;
        Response response = invoke(() -> userService.getUser(securityContext, null, "ns1", "myspace.user1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testCreate() {
        User u3 = new UserBuilder()
                .withNewMetadata()
                .withName("myspace.user3")
                .withNamespace("ns1")
                .endMetadata()

                .withSpec(new UserSpecBuilder()
                        .withUsername("user3")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.createUser(securityContext, new ResteasyUriInfo("http://localhost:8443/", null, "/"), "ns1", u3));
        assertThat(response.getStatus(), is(201));

        assertThat(userApi.listUsers(null, "ns1").getItems(), hasItem(u3));
    }

    @Test
    public void testCreateException() {
        userApi.throwException = true;
        User u3 = new UserBuilder()
                .withNewMetadata()
                .withName("myspace.user3")
                .withNamespace("ns1")
                .endMetadata()

                .withSpec(new UserSpecBuilder()
                        .withUsername("user3")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.createUser(securityContext, null, "ns1", u3));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testPutException() {
        userApi.throwException = true;
        User u3 = new UserBuilder()
                .withNewMetadata()
                .withName("myspace.user1")
                .withNamespace("ns1")
                .endMetadata()

                .withSpec(new UserSpecBuilder()
                        .withUsername("user1")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.replaceUser(securityContext, "ns1", "myspace.user1", u3));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testPutNotMatchingName() {
        User u3 = new UserBuilder()
                .withNewMetadata()
                .withName("myspace.user3")
                .withNamespace("ns1")
                .endMetadata()

                .withSpec(new UserSpecBuilder()
                        .withUsername("user3")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.replaceUser(securityContext, "ns1", "myspace.user1", u3));
        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testPutNotExists() {
        User u3 = new UserBuilder()
                .withNewMetadata()
                .withName("myspace.user3")
                .withNamespace("ns1")
                .endMetadata()

                .withSpec(new UserSpecBuilder()
                        .withUsername("user3")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue1", "topic1"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.replaceUser(securityContext, "ns1", "myspace.user3", u3));
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void testPut() {
        User u3 = new UserBuilder()
                .withNewMetadata()
                .withName("myspace.user1")
                .withNamespace("ns1")
                .endMetadata()

                .withSpec(new UserSpecBuilder()
                        .withUsername("user1")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("p4ssw0rd")
                                .build())
                        .withAuthorization(Arrays.asList(
                                new UserAuthorizationBuilder()
                                        .withAddresses(Arrays.asList("queue2", "topic2"))
                                        .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build()))
                        .build())
                .build();
        Response response = invoke(() -> userService.replaceUser(securityContext, "ns1", "myspace.user1", u3));
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void testDelete() {
        Response response = invoke(() -> userService.deleteUser(securityContext, "ns1", "myspace.user1"));
        assertThat(response.getStatus(), is(200));
        assertThat(((Status) response.getEntity()).getStatusCode(), is(200));

        assertTrue(userApi.listUsers(null, "ns1").getItems().isEmpty());
        assertFalse(userApi.listUsers(null, "ns2").getItems().isEmpty());
    }

    @Test
    public void testDeleteException() {
        userApi.throwException = true;
        Response response = invoke(() -> userService.deleteUser(securityContext, "ns1", "myspace.user1"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void deleteAllUsers() {
        Response response = invoke(() -> userService.deleteUsers(securityContext, "unknown"));
        assertThat(response.getStatus(), is(200));
        assertThat(userApi.listUsers(null, "ns1").getItems().size(), is(1));

        response = invoke(() -> userService.deleteUsers(securityContext, "ns1"));
        assertThat(response.getStatus(), is(200));
        assertThat(userApi.listUsers(null, "ns1").getItems().size(), is(0));
    }

    @Test
    public void jsonPatch_RFC6902() throws Exception {
        final JsonPatch patch = mapper.readValue("[{\"op\":\"add\",\"path\":\"/spec/authentication/password\",\"value\":\"dorado\"}]", JsonPatch.class);

        Response response = userService.patchUser(securityContext, u1.getMetadata().getNamespace(), u1.getMetadata().getName(), patch);
        assertThat(response.getStatus(), is(200));
        assertThat(((User) response.getEntity()).getSpec().getAuthentication().getPassword(), is("dorado"));
    }

    @Test
    public void jsonMergePatch_RFC7386() throws Exception {
        final JsonPatch patch = mapper.readValue("[{\"op\":\"add\",\"path\":\"/spec/authentication/password\",\"value\":\"dorado\"}]", JsonPatch.class);

        Response response = userService.patchUser(securityContext, u1.getMetadata().getNamespace(), u1.getMetadata().getName(), patch);
        assertThat(response.getStatus(), is(200));
        assertThat(((User) response.getEntity()).getSpec().getAuthentication().getPassword(), is("dorado"));
    }

    @Test
    public void patchUserNotFound() throws Exception {
        final JsonPatch patch = mapper.readValue("[{\"op\":\"add\",\"path\":\"/metadata/annotations/fish\",\"value\":\"dorado\"}]", JsonPatch.class);

        Response response = userService.patchUser(securityContext, "myns", "myspace.unknown", patch);
        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void patchImmutable() throws Exception {

        final JsonPatch patch = mapper.readValue("[" +
                "{\"op\":\"replace\",\"path\":\"/metadata/name\",\"value\":\"newname\"}," +
                "{\"op\":\"replace\",\"path\":\"/metadata/namespace\",\"value\":\"newnamespace\"}" +
                "]", JsonPatch.class);

        Response response = userService.patchUser(securityContext, u1.getMetadata().getNamespace(), u1.getMetadata().getName(), patch);
        assertThat(response.getStatus(), is(200));
        User updated = (User) response.getEntity();
        assertThat(updated.getMetadata().getName(), is(u1.getMetadata().getName()));
        assertThat(updated.getMetadata().getNamespace(), is(u1.getMetadata().getNamespace()));
    }

}
