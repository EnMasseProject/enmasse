/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

import java.util.Collections;
import java.util.List;

public class ApiHeaderConfig {
    private final List<String> userHeaders;
    private final List<String> groupHeaders;
    private final List<String> extraHeadersPrefix;

    public static ApiHeaderConfig DEFAULT_HEADERS_CONFIG = new ApiHeaderConfig(
            Collections.singletonList("X-Remote-User"),
            Collections.singletonList("X-Remote-Group"),
            Collections.singletonList("X-Remote-Extra-"));

    public ApiHeaderConfig(List<String> userHeaders, List<String> groupHeaders, List<String> extraHeadersPrefix) {
        this.userHeaders = userHeaders;
        this.groupHeaders = groupHeaders;
        this.extraHeadersPrefix = extraHeadersPrefix;
    }

    public List<String> getUserHeaders() {
        return userHeaders;
    }

    public List<String> getGroupHeaders() {
        return groupHeaders;
    }

    public List<String> getExtraHeadersPrefix() {
        return extraHeadersPrefix;
    }
}
