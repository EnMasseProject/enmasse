/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserModelTest {
    @Test
    public void testSerializeUserPassword() throws IOException {
        User user = new User.Builder()
                .setMetadata(new UserMetadata.Builder()
                        .setName("myspace.user1")
                        .setNamespace("ns1")
                        .build())
                .setSpec(new UserSpec.Builder()
                        .setUsername("user1")
                        .setAuthentication(new UserAuthentication.Builder()
                                .setType(UserAuthenticationType.password)
                                .setPassword("p4ssw0rd")
                                .build())
                        .setAuthorization(Arrays.asList(
                                new UserAuthorization.Builder()
                                        .setAddresses(Arrays.asList("queue1", "topic1"))
                                        .setOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build(),
                                new UserAuthorization.Builder()
                                        .setAddresses(Arrays.asList("direct*"))
                                        .setOperations(Arrays.asList(Operation.view))
                                        .build()))
                        .build())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        byte [] serialized = mapper.writeValueAsBytes(user);

        User deserialized = mapper.readValue(serialized, User.class);

        assertEquals(user.getMetadata().getName(), deserialized.getMetadata().getName());
        assertEquals(user.getMetadata().getNamespace(), deserialized.getMetadata().getNamespace());
        assertEquals(user.getSpec().getUsername(), deserialized.getSpec().getUsername());
        assertEquals(user.getSpec().getAuthentication().getType(), deserialized.getSpec().getAuthentication().getType());
        assertEquals(user.getSpec().getAuthentication().getPassword(), deserialized.getSpec().getAuthentication().getPassword());
        assertEquals(user.getSpec().getAuthorization().size(), deserialized.getSpec().getAuthorization().size());

        assertAuthorization(deserialized, Arrays.asList("queue1", "topic1"), Arrays.asList(Operation.send, Operation.recv));

        UserList list = new UserList();
        list.add(user);

        serialized = mapper.writeValueAsBytes(list);
        UserList deserializedList = mapper.readValue(serialized, UserList.class);

        assertEquals(1, deserializedList.getItems().size());

        deserialized = deserializedList.getItems().get(0);

        assertEquals(user.getMetadata().getName(), deserialized.getMetadata().getName());
        assertEquals(user.getMetadata().getNamespace(), deserialized.getMetadata().getNamespace());
        assertEquals(user.getSpec().getUsername(), deserialized.getSpec().getUsername());
        assertEquals(user.getSpec().getAuthentication().getType(), deserialized.getSpec().getAuthentication().getType());
        assertEquals(user.getSpec().getAuthentication().getPassword(), deserialized.getSpec().getAuthentication().getPassword());
        assertEquals(user.getSpec().getAuthorization().size(), deserialized.getSpec().getAuthorization().size());

        assertAuthorization(deserialized, Arrays.asList("queue1", "topic1"), Arrays.asList(Operation.send, Operation.recv));
    }

    @Test
    public void testSerializeUserFederated() throws IOException {
        User user = new User.Builder()
                .setMetadata(new UserMetadata.Builder()
                        .setName("myspace.user1")
                        .setNamespace("ns1")
                        .build())
                .setSpec(new UserSpec.Builder()
                        .setUsername("user1")
                        .setAuthentication(new UserAuthentication.Builder()
                                .setType(UserAuthenticationType.federated)
                                .setProvider("openshift")
                                .setFederatedUserid("uuid")
                                .setFederatedUsername("user1")
                                .build())
                        .setAuthorization(Arrays.asList(
                                new UserAuthorization.Builder()
                                        .setAddresses(Arrays.asList("queue1", "topic1"))
                                        .setOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build(),
                                new UserAuthorization.Builder()
                                        .setAddresses(Arrays.asList("direct*"))
                                        .setOperations(Arrays.asList(Operation.view))
                                        .build()))
                        .build())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        byte [] serialized = mapper.writeValueAsBytes(user);

        User deserialized = mapper.readValue(serialized, User.class);

        assertEquals(user.getMetadata().getName(), deserialized.getMetadata().getName());
        assertEquals(user.getMetadata().getNamespace(), deserialized.getMetadata().getNamespace());
        assertEquals(user.getSpec().getUsername(), deserialized.getSpec().getUsername());
        assertEquals(user.getSpec().getAuthentication().getType(), deserialized.getSpec().getAuthentication().getType());
        assertEquals(user.getSpec().getAuthentication().getPassword(), deserialized.getSpec().getAuthentication().getPassword());
        assertEquals(user.getSpec().getAuthorization().size(), deserialized.getSpec().getAuthorization().size());

        assertAuthorization(deserialized, Arrays.asList("queue1", "topic1"), Arrays.asList(Operation.send, Operation.recv));

        UserList list = new UserList();
        list.add(user);

        serialized = mapper.writeValueAsBytes(list);
        UserList deserializedList = mapper.readValue(serialized, UserList.class);

        assertEquals(1, deserializedList.getItems().size());

        deserialized = deserializedList.getItems().get(0);

        assertEquals(user.getMetadata().getName(), deserialized.getMetadata().getName());
        assertEquals(user.getMetadata().getNamespace(), deserialized.getMetadata().getNamespace());
        assertEquals(user.getSpec().getUsername(), deserialized.getSpec().getUsername());
        assertEquals(user.getSpec().getAuthentication().getType(), deserialized.getSpec().getAuthentication().getType());
        assertEquals(user.getSpec().getAuthentication().getPassword(), deserialized.getSpec().getAuthentication().getPassword());
        assertEquals(user.getSpec().getAuthorization().size(), deserialized.getSpec().getAuthorization().size());

        assertAuthorization(deserialized, Arrays.asList("queue1", "topic1"), Arrays.asList(Operation.send, Operation.recv));
    }

    @Test
    public void testValidation() {
        createAndValidate("myspace.user1", "user1", true);
        createAndValidate("myspace.usEr1", "user1", false);
        createAndValidate("myspace.user1", "usEr1", false);
        createAndValidate("myspace.user1", "usEr1", false);
        createAndValidate("myspaceuser1", "user1", false);
        createAndValidate("myspace.user1-", "user1", false);
        createAndValidate("myspace-.user1", "user1", false);
        createAndValidate("-myspace.user1", "user1", false);
        createAndValidate("myspace.-user1", "user1", false);
        createAndValidate("myspace.user1", "user1-", false);
        createAndValidate("myspace.user1", "-user1-", false);
        createAndValidate("myspace.user1-foo-bar", "user1-foo-bar", true);
        UUID uuid = UUID.randomUUID();
        createAndValidate("myspace." + uuid.toString(), uuid.toString(), true);
        createAndValidate("a.ab", "ab", true);
        createAndValidate("aa.b", "b", true);
        createAndValidate("a.b", "b", true);
    }

    private void createAndValidate(String name, String username, boolean shouldValidate) {
        User u1 = new User.Builder()
                .setMetadata(new UserMetadata.Builder()
                        .setName(name)
                        .setNamespace("ns1")
                        .build())
                .setSpec(new UserSpec.Builder()
                        .setUsername(username)
                        .setAuthentication(new UserAuthentication.Builder()
                                .setType(UserAuthenticationType.federated)
                                .setProvider("openshift")
                                .setFederatedUserid("uuid")
                                .setFederatedUsername("user1")
                                .build())
                        .setAuthorization(Arrays.asList(
                                new UserAuthorization.Builder()
                                        .setAddresses(Arrays.asList("queue1", "topic1"))
                                        .setOperations(Arrays.asList(Operation.send, Operation.recv))
                                        .build(),
                                new UserAuthorization.Builder()
                                        .setAddresses(Arrays.asList("direct*"))
                                        .setOperations(Arrays.asList(Operation.view))
                                        .build()))
                        .build())
                .build();

        try {
            u1.validate();
            assertTrue(shouldValidate);
        } catch (UserValidationFailedException e) {
            // e.printStackTrace();
            assertFalse(shouldValidate);
        }
    }

    private void assertAuthorization(User deserialized, List<String> addresses, List<Operation> operations) {
        for (UserAuthorization authorization : deserialized.getSpec().getAuthorization()) {
            if (authorization.getOperations().equals(operations) && authorization.getAddresses().equals(addresses)) {
                return;
            }
        }
        assertFalse(true, "Unable to find matching authorization");
    }
}
