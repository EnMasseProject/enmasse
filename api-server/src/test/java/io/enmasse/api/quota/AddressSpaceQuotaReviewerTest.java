/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.quota;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.v1.quota.*;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.AddressSpaceQuotaApi;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.k8s.api.TestAddressSpaceQuotaApi;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AddressSpaceQuotaReviewerTest {
    private AddressSpaceApi addressSpaceApi;
    private AddressSpaceQuotaApi addressSpaceQuotaApi;
    private AddressSpaceQuotaReviewer reviewer;

    @Before
    public void setup() {
        addressSpaceApi = new TestAddressSpaceApi();
        addressSpaceQuotaApi = new TestAddressSpaceQuotaApi();

        reviewer = new AddressSpaceQuotaReviewer(addressSpaceQuotaApi, addressSpaceApi);
        createAddressSpaceQuota(
                TestAddressSpaceQuotaApi.createQuota("quota1", "user1", new AddressSpaceQuotaRule(2, "standard", "unlimited-standard")),
                TestAddressSpaceQuotaApi.createQuota("quota2", "user1", new AddressSpaceQuotaRule(1, "standard", "unlimited-standard")),
                TestAddressSpaceQuotaApi.createQuota("quota3", "user2", new AddressSpaceQuotaRule(2, "standard", null)),
                TestAddressSpaceQuotaApi.createQuota("quota4", "user3", new AddressSpaceQuotaRule(2, null, "unlimited-standard")),
                TestAddressSpaceQuotaApi.createQuota("quota5", "user4", new AddressSpaceQuotaRule(2, null, null)),
                TestAddressSpaceQuotaApi.createQuota("quota6", "user5", new AddressSpaceQuotaRule(1, "standard", "unlimited-standard"), new AddressSpaceQuotaRule(2, "brokered", null)),
                TestAddressSpaceQuotaApi.createQuota("quota7", "user6", new AddressSpaceQuotaRule(2, "standard", "unlimited-standard"), new AddressSpaceQuotaRule(1, null, null), new AddressSpaceQuotaRule(3, "standard", null))
        );

    }

    @Test
    public void testSimpleRule() throws Exception {
        addressSpaceQuotaApi.createAddressSpaceQuota(TestAddressSpaceQuotaApi.createQuota(
                "simplequota", "myuser", new AddressSpaceQuotaRule( 2, "standard", "unlimited-standard" ) ));

        AddressSpaceQuotaReview review = new AddressSpaceQuotaReview(
                new AddressSpaceQuotaReviewSpec("myuser", null), null);

        AddressSpaceQuotaReview evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace", "standard", "unlimited-standard", "myuser");

        evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace2", "standard", "unlimited-standard", "myuser");

        evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace3", "standard", "unlimited-standard", "myuser");

        evaluatedReview = reviewer.reviewQuota(review);
        assertTrue(evaluatedReview.getStatus().isExceeded());
    }

    @Test
    public void testQuotasMerged() throws Exception {
        AddressSpaceQuotaReview review = new AddressSpaceQuotaReview(
                new AddressSpaceQuotaReviewSpec("user1", null), null);

        createAddressSpace("myspace", "standard", "unlimited-standard", "user1");
        createAddressSpace("myspace2", "standard", "unlimited-standard", "user1");
        createAddressSpace("myspace3", "standard", "unlimited-standard", "user1");

        AddressSpaceQuotaReview evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());
        createAddressSpace("myspace4", "standard", "unlimited-standard", "user1");

        evaluatedReview = reviewer.reviewQuota(review);
        assertTrue(evaluatedReview.getStatus().isExceeded());
    }

    @Test
    public void testOmittedPlan() throws Exception {
        AddressSpaceQuotaReview review = new AddressSpaceQuotaReview(
                new AddressSpaceQuotaReviewSpec("user2", null), null);

        createAddressSpace("myspace", "standard", "unlimited-standard", "user2");
        AddressSpaceQuotaReview evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace2", "standard", "unlimited-standard", "user2");
        evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace3", "standard", "unlimited-standard", "user2");
        evaluatedReview = reviewer.reviewQuota(review);
        assertTrue(evaluatedReview.getStatus().isExceeded());
    }

    @Test
    public void testOmittedType() throws Exception {
        AddressSpaceQuotaReview review = new AddressSpaceQuotaReview(
                new AddressSpaceQuotaReviewSpec("user3", null), null);

        createAddressSpace("myspace", "standard", "unlimited-standard", "user3");
        AddressSpaceQuotaReview evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace2", "standard", "unlimited-standard", "user3");
        evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace3", "standard", "unlimited-standard", "user3");
        evaluatedReview = reviewer.reviewQuota(review);
        assertTrue(evaluatedReview.getStatus().isExceeded());
    }

    @Test
    public void testRestrictedByFewest() throws Exception {
        AddressSpaceQuotaReview review = new AddressSpaceQuotaReview(
                new AddressSpaceQuotaReviewSpec("user3", null), null);

        createAddressSpace("myspace", "standard", "unlimited-standard", "user3");
        AddressSpaceQuotaReview evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace2", "standard", "unlimited-standard", "user3");
        evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace3", "standard", "unlimited-standard", "user3");
        evaluatedReview = reviewer.reviewQuota(review);
        assertTrue(evaluatedReview.getStatus().isExceeded());
    }

    @Test
    public void testMultipleRules() throws Exception {
        AddressSpaceQuotaReview review = new AddressSpaceQuotaReview(
                new AddressSpaceQuotaReviewSpec("user5", null), null);

        createAddressSpace("myspace", "standard", "unlimited-standard", "user5");
        AddressSpaceQuotaReview evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace2", "standard", "unlimited-standard", "user5");
        evaluatedReview = reviewer.reviewQuota(review);
        assertTrue(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace2", "brokered", "unlimited-brokered", "user5");
        evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace3", "brokered", "unlimited-brokered", "user5");
        evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace4", "brokered", "unlimited-brokered", "user5");
        evaluatedReview = reviewer.reviewQuota(review);
        assertTrue(evaluatedReview.getStatus().isExceeded());
    }

    @Test
    public void testMatchesMostRestrictiveRule() throws Exception {
        AddressSpaceQuotaReview review = new AddressSpaceQuotaReview(
                new AddressSpaceQuotaReviewSpec("user6", null), null);

        createAddressSpace("myspace", "standard", "unlimited-standard", "user6");
        AddressSpaceQuotaReview evaluatedReview = reviewer.reviewQuota(review);
        assertFalse(evaluatedReview.getStatus().isExceeded());

        createAddressSpace("myspace2", "standard", "unlimited-standard", "user6");
        evaluatedReview = reviewer.reviewQuota(review);
        assertTrue(evaluatedReview.getStatus().isExceeded());
    }

    private void createAddressSpaceQuota(AddressSpaceQuota ... quotas) {
        for (AddressSpaceQuota quota : quotas) {
            addressSpaceQuotaApi.createAddressSpaceQuota(quota);
        }
    }

    private void createAddressSpace(String name, String type, String plan, String who) throws Exception {
        addressSpaceApi.createAddressSpace(new AddressSpace.Builder()
                .setName(name)
                .setNamespace("ns")
                .putLabel(LabelKeys.CREATED_BY, who)
                .setType(type)
                .setPlan(plan)
                .build());

    }
}
