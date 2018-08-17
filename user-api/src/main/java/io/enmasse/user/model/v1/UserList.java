/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.user.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserList {
    @JsonProperty("apiVersion")
    private final String apiVersion = "user.enmasse.io/v1alpha1";
    @JsonProperty("kind")
    private final String kind = "MessagingUserList";

    @JsonProperty("items")
    private final List<User> items;

    public UserList() {
        items = new ArrayList<>();
    }

    public UserList(List<User> items) {
        this.items = items;
    }

    public void add(User user) {
        items.add(user);
    }

    public List<User> getItems() {
        return items;
    }

    public void addAll(UserList userList) {
        this.items.addAll(userList.getItems());
    }
}
