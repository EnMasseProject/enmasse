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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    private void assertAuthorization(User deserialized, List<String> addresses, List<Operation> operations) {
        for (UserAuthorization authorization : deserialized.getSpec().getAuthorization()) {
            if (authorization.getOperations().equals(operations) && authorization.getAddresses().equals(addresses)) {
                return;
            }
        }
        assertFalse(true, "Unable to find matching authorization");
    }
}
