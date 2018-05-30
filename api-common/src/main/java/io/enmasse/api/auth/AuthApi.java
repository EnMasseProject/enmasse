/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

public interface AuthApi {
    TokenReview performTokenReview(String token);
    SubjectAccessReview performSubjectAccessReviewResource(String user, String namespace, String resource, String verb);
    SubjectAccessReview performSubjectAccessReviewPath(String user, String path, String verb);
    String getCert(String secretName, String namespace);
    String getNamespace();
}
