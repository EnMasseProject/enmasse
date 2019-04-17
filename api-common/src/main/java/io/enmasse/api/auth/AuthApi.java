/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.auth;

public interface AuthApi {
    TokenReview performTokenReview(String token);
    SubjectAccessReview performSubjectAccessReviewResource(TokenReview tokenReview, String namespace, String resource, String verb, String apiGroup);
    SubjectAccessReview performSubjectAccessReviewPath(TokenReview tokenReview, String path, String verb);
    String getCert(String secretName);
    String getNamespace();
}
