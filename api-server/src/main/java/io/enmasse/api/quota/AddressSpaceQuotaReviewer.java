/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.quota;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.v1.quota.AddressSpaceQuota;
import io.enmasse.address.model.v1.quota.AddressSpaceQuotaReview;
import io.enmasse.address.model.v1.quota.AddressSpaceQuotaReviewStatus;
import io.enmasse.address.model.v1.quota.AddressSpaceQuotaRule;
import io.enmasse.config.LabelKeys;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.AddressSpaceQuotaApi;

import java.util.*;
import java.util.stream.Collectors;

public class AddressSpaceQuotaReviewer {
    private final AddressSpaceQuotaApi quotaApi;
    private final AddressSpaceApi addressSpaceApi;

    public AddressSpaceQuotaReviewer(AddressSpaceQuotaApi quotaApi, AddressSpaceApi addressSpaceApi) {
        this.quotaApi = quotaApi;
        this.addressSpaceApi = addressSpaceApi;
    }

    public AddressSpaceQuotaReview reviewQuota(AddressSpaceQuotaReview review) {

        Collection<AddressSpaceQuotaRule> quotaRules;
        if (review.getSpec().getRules() == null) {
            List<AddressSpaceQuota> quotaList = quotaApi.listAddressSpaceQuotasWithLabels(Collections.singletonMap(LabelKeys.USER, review.getSpec().getUser())).getItems();
            quotaRules = aggregateRulesForUser(quotaList, review.getSpec().getUser());
        } else {
            quotaRules = review.getSpec().getRules();
        }

        Set<AddressSpace> addressSpaces = addressSpaceApi.listAddressSpacesWithLabels(Collections.singletonMap(LabelKeys.CREATED_BY, review.getSpec().getUser()));

        boolean exceeded = evaluateRules(quotaRules, addressSpaces);
        return new AddressSpaceQuotaReview(review.getSpec(), new AddressSpaceQuotaReviewStatus(exceeded));
    }

    private boolean evaluateRules(Collection<AddressSpaceQuotaRule> quotaRules, Set<AddressSpace> addressSpaces) {
        for (AddressSpaceQuotaRule quotaRule : quotaRules) {
            Set<AddressSpace> filtered = filterByTypeAndPlan(quotaRule.getType(), quotaRule.getPlan(), addressSpaces);
            if (filtered.size() > quotaRule.getCount()) {
                return true;
            }
        }
        return false;
    }

    private static Set<AddressSpace> filterByTypeAndPlan(String type, String plan, Set<AddressSpace> addressSpaces) {
        return addressSpaces.stream()
                .filter(addressSpace -> (type == null || type.equals(addressSpace.getType())) && (plan == null || plan.equals(addressSpace.getPlan())))
                .collect(Collectors.toSet());
    }

    private Collection<AddressSpaceQuotaRule> aggregateRulesForUser(List<AddressSpaceQuota> quotaList, String user) {
        Map<String, AddressSpaceQuotaRule> ruleByLabel = new HashMap<>();
        for (AddressSpaceQuota quota : quotaList) {
            if (quota.getSpec().getUser().equals(user)) {
                for (AddressSpaceQuotaRule rule : quota.getSpec().getRules()) {
                    String ruleLabel = ruleLabelString(rule);
                    AddressSpaceQuotaRule existingRule = ruleByLabel.get(ruleLabel);
                    if (existingRule == null) {
                        ruleByLabel.put(ruleLabel, rule);
                    } else {
                        ruleByLabel.put(ruleLabel, new AddressSpaceQuotaRule(existingRule.getCount() + rule.getCount(), existingRule.getType(), existingRule.getPlan()));
                    }
                }
            }
        }

        return ruleByLabel.values();
    }

    private static String ruleLabelString(AddressSpaceQuotaRule rule) {
        return rule.getType() + "." + rule.getPlan();
    }
}
