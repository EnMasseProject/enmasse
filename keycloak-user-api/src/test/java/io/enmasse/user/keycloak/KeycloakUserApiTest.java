/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.keycloak;

import static io.enmasse.user.keycloak.Helpers.assertSorted;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.enmasse.user.model.v1.*;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class KeycloakUserApiTest {
    @Test
    public void testConversion() {
        UserRepresentation userRep = new UserRepresentation();

        userRep.setUsername("user1");
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("resourceName", Arrays.asList("myspace.user1"));
        attributes.put("resourceNamespace", Arrays.asList("ns1"));
        attributes.put("authenticationType", Arrays.asList(UserAuthenticationType.password.name()));
        attributes.put("creationTimestamp", Arrays.asList("12345"));
        userRep.setAttributes(attributes);

        GroupRepresentation sendQueue1 = new GroupRepresentation();
        sendQueue1.setId("sq1");
        sendQueue1.setName("send_queue_1");

        GroupRepresentation recvQueue1 = new GroupRepresentation();
        recvQueue1.setId("rq1");
        recvQueue1.setName("recv_queue_1");

        GroupRepresentation allDirect1 = new GroupRepresentation();
        allDirect1.setId("mg");
        allDirect1.setName("manage");

        userRep.setGroups(Arrays.asList("sq1", "rq1", "mg"));

        User user = KeycloakUserApi.buildUser(userRep, Arrays.asList(sendQueue1, recvQueue1, allDirect1));
        assertEquals("myspace.user1", user.getMetadata().getName());
        assertEquals("ns1", user.getMetadata().getNamespace());
        assertEquals("user1", user.getSpec().getUsername());
        assertEquals(UserAuthenticationType.password, user.getSpec().getAuthentication().getType());

        assertEquals(2, user.getSpec().getAuthorization().size());
        assertAuthorizations(Arrays.asList(new UserAuthorizationBuilder()
                .addToAddresses("queue_1")
                .addToOperations(Operation.send, Operation.recv)
                .build(), new UserAuthorizationBuilder()
                .addToOperations(Operation.manage)
                .addToAddresses()
                .build()), user.getSpec().getAuthorization());
    }

    private static UserRepresentation mockUser(String name, String namespace, String addressSpaceName) {
        final UserRepresentation userRep = new UserRepresentation();

        userRep.setUsername(name);

        final Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("resourceName", Arrays.asList(addressSpaceName + "." + name));
        attributes.put("resourceNamespace", Arrays.asList(namespace));
        attributes.put("authenticationType", Arrays.asList(UserAuthenticationType.password.name()));
        attributes.put("creationTimestamp", Arrays.asList("12345"));
        userRep.setAttributes(attributes);

        return userRep;
    }

    /**
     * Test un-escaping group names.
     */
    @Test
    public void testGroupConversionWithEscapes1() {

        final User user = runBuildUser(
                mockUser("user1", "ns1", "as1"),
                simpleGroups("recv_foo%2Fbar", "send_foo%2Fbar"));

        final List<UserAuthorization> auth = new ArrayList<>();
        auth.add(new UserAuthorization(Arrays.asList("foo/bar"), Arrays.asList(Operation.recv, Operation.send)));

        assertAuthorizations(auth, user.getSpec().getAuthorization());
    }

    /**
     * A more complex example, un-escaping group names.
     */
    @Test
    public void testGroupConversionWithEscapes2() {

        final User user = runBuildUser(
                mockUser("user1", "ns1", "as1"),

                simpleGroups(
                        "recv_foo%2Fbar%2F%23",
                        "send_foo%2Fbar%2F%23",

                        // swap order of send+recv

                        "send_foo%2Fbaz%2F%23",
                        "recv_foo%2Fbaz%2F%23",

                        // additional entry for the same address, but with a single permission only

                        "view_foo%2Fbar%2F%23"));

        // build expected result

        final List<UserAuthorization> auth = new ArrayList<>();
        auth.add(new UserAuthorization(
                asList("foo/bar/#"),
                asList(Operation.recv, Operation.send, Operation.view)));
        auth.add(new UserAuthorization(
                asList("foo/baz/#"),
                asList(Operation.recv, Operation.send)));

        assertAuthorizations(auth, user.getSpec().getAuthorization());
    }

    /**
     * Test conversion back to API object, having only one "manage" group. <br>
     * This should convert to one single manage operation, for an empty address
     * list. Normally the user would have two manage groups. See
     * {@link #testGroupConversionWithManage2()}.
     */
    @Test
    public void testGroupConversionWithManage1() {

        final User user = runBuildUser(
                mockUser("user1", "ns1", "as1"),
                simpleGroups("manage"));

        final List<UserAuthorization> auth = new ArrayList<>();
        auth.add(new UserAuthorization(Arrays.asList(), Arrays.asList(Operation.manage)));

        assertAuthorizations(auth, user.getSpec().getAuthorization());
    }

    /**
     * Test conversion back to API object, having only one "manage" group, but also
     * in the wildcard form. <br>
     * This should condense to a single "manage" operation, with an empty address
     * list.
     */
    @Test
    public void testGroupConversionWithManage2() {

        final User user = runBuildUser(
                mockUser("user1", "ns1", "as1"),
                simpleGroups("manage", "manage_#"));

        final List<UserAuthorization> auth = new ArrayList<>();
        auth.add(new UserAuthorization(Arrays.asList(), Arrays.asList(Operation.manage)));

        assertAuthorizations(auth, user.getSpec().getAuthorization());
    }

    /**
     * Test a global mapping. <br/>
     * A global mapping that is not {@code manage}, will be discarded.
     */
    @Test
    public void testGroupConversionWithGlobal() {

        final User user = runBuildUser(
                mockUser("user1", "ns1", "as1"),
                simpleGroups("recv"));

        assertAuthorizations(emptyList(), user.getSpec().getAuthorization());
    }

    private static GroupRepresentation simpleGroup(String id, String name) {
        final GroupRepresentation group = new GroupRepresentation();
        group.setId(id);
        group.setName(name);
        return group;
    }

    private static GroupRepresentation[] simpleGroups(String... names) {

        final GroupRepresentation[] result = new GroupRepresentation[names.length];

        int idx = 0;
        for (int i = 0; i < names.length; i++) {
            result[i] = simpleGroup("id" + idx++, names[i]);
        }

        return result;
    }

    private static User runBuildUser(final UserRepresentation userRep,
            final GroupRepresentation... groupRepresentations) {
        final List<GroupRepresentation> groups = Arrays.asList(groupRepresentations);
        userRep.setGroups(groups.stream().map(GroupRepresentation::getId).collect(toList()));

        return KeycloakUserApi.buildUser(userRep, groups);
    }

    public void assertAuthorizations(final List<UserAuthorization> expected, final List<UserAuthorization> actual) {

        assertEquals(expected.size(), actual.size());

        // ensure that we have mutable lists

        final List<UserAuthorization> expectedCopy = new ArrayList<>(expected);
        final List<UserAuthorization> actualCopy = new ArrayList<>(actual);

        // put them in a reproducible order

        expectedCopy.sort(Helpers::compareUserAuthorization);
        actualCopy.sort(Helpers::compareUserAuthorization);

        // and compare them

        for (int i = 0; i < expected.size(); i++) {
            final UserAuthorization expectedAuth = expectedCopy.get(i);
            final UserAuthorization actualAuth = actualCopy.get(i);

            assertSorted(expectedAuth.getAddresses(), actualAuth.getAddresses());
        }
    }

    @Test
    public void testDesiredGroupsTransformationEmpty() {

        final List<UserAuthorization> auth = Collections.emptyList();
        final Set<String> result = KeycloakUserApi.createDesiredGroupsSet(auth);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testDesiredGroupsTransformation1() {

        final List<UserAuthorization> auth = Arrays.asList(
                new UserAuthorization(Arrays.asList("foo"), Arrays.asList(Operation.recv)));

        final Set<String> result = KeycloakUserApi.createDesiredGroupsSet(auth);

        assertGroups(result, "recv_foo");
    }

    @Test
    public void testDesiredGroupsTransformationManage() {

        final List<UserAuthorization> auth = Arrays.asList(
                new UserAuthorization(Collections.emptyList(), Arrays.asList(Operation.manage)));

        final Set<String> result = KeycloakUserApi.createDesiredGroupsSet(auth);

        assertGroups(result, "manage");
    }

    @Test
    public void testDesiredGroupsTransformation2() {

        final List<UserAuthorization> auth = Arrays.asList(
                new UserAuthorization(Arrays.asList("foo/bar", "bar/baz/#", "baz_quux"),
                        Arrays.asList(Operation.recv, Operation.send)),
                new UserAuthorization(Arrays.asList("fuu/*"), Arrays.asList(Operation.view)),
                new UserAuthorization(Arrays.asList("faa/#"), Arrays.asList(Operation.manage)));

        final Set<String> result = KeycloakUserApi.createDesiredGroupsSet(auth);

        assertGroups(result,
                "recv_foo%2Fbar", "send_foo%2Fbar", "recv_bar%2Fbaz%2F%23", "send_bar%2Fbaz%2F%23",
                "monitor", "manage", "send_baz_quux", "recv_baz_quux");
    }

    @Test
    public void testDesiredGroupsTransformationEmptyAddress1() {

        final List<UserAuthorization> auth = Arrays.asList(
                new UserAuthorization(Collections.emptyList(), Arrays.asList(Operation.recv)));

        final Set<String> result = KeycloakUserApi.createDesiredGroupsSet(auth);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testDesiredGroupsTransformationEmptyAddress2() {

        final List<UserAuthorization> auth = Arrays.asList(
                new UserAuthorization(null,
                        Arrays.asList(Operation.recv, Operation.send, Operation.manage)));

        final Set<String> result = KeycloakUserApi.createDesiredGroupsSet(auth);

        assertGroups(result, "manage");
    }

    /**
     * Assert groups. <br>
     * A convenience method to assert group information. Compares the groups as
     * sorted arrays.
     *
     * @param actual   The actual result.
     * @param expected The expected result.
     */
    private static void assertGroups(final Collection<String> actual, final String... expected) {
        assertSorted(expected, actual.toArray(new String[actual.size()]));
    }

    @Test
    public void testHasAttribute1() {
        final RealmRepresentation realm = new RealmRepresentation();
        realm.setAttributes(new HashMap<>());
        realm.getAttributes().put("foo", "bar");

        assertTrue(KeycloakUserApi.hasAttribute(realm, "foo", "bar"));
        assertFalse(KeycloakUserApi.hasAttribute(realm, "foo", null));
        assertFalse(KeycloakUserApi.hasAttribute(realm, "foo2", "bar"));
        assertTrue(KeycloakUserApi.hasAttribute(realm, "foo2", null));

        assertFalse(KeycloakUserApi.hasAttribute(realm, null, null));

    }

    @Test
    public void testHasAttribute2() {
        assertFalse(KeycloakUserApi.hasAttribute(null, "foo", "bar"));
        assertFalse(KeycloakUserApi.hasAttribute(null, "foo", null));
        assertFalse(KeycloakUserApi.hasAttribute(null, null, null));
    }

    @Test
    public void testHasAttribute3() {
        final RealmRepresentation realm = new RealmRepresentation();

        assertFalse(KeycloakUserApi.hasAttribute(realm, "foo", "bar"));
        assertTrue(KeycloakUserApi.hasAttribute(realm, "foo", null));
        assertFalse(KeycloakUserApi.hasAttribute(realm, null, null));
    }

    @Test
    public void testAnnotations() {
        final Map<String,String> annotations = new HashMap<>();
        annotations.put("foo", "bar");
        annotations.put("bar", "baz");

        final List<String> encoded = KeycloakUserApi.annotationsToString(annotations);
        assertNotNull(encoded);
        assertEquals(2,encoded.size());

        final Map<String, String> actualAnnotations = KeycloakUserApi.annotationsFromString(encoded);
        assertEquals(annotations, actualAnnotations);
    }
}
