/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases.plans;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlansTestBase extends TestBase {
    private static Logger log = CustomLogger.getLogger();


    public static Matcher<Address> assertAddressStatusNotReady(final String messageContains) {
        return PlansTestBase.assertAddressStatus(false, Optional.empty(), Optional.of(messageContains));
    }

    public static Matcher<Address> assertAddressStatusReady(String actualPlan) {
        return PlansTestBase.assertAddressStatus(true, Optional.of(actualPlan), Optional.empty());
    }

    public static Matcher<Address> assertAddressStatus(final boolean ready, final Optional<String> actualPlan, final Optional<String> messageContains) {
        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("should match ready ").appendValue(ready);
                actualPlan.ifPresent(s -> description.appendText("should match plan ").appendValue(s));
                messageContains.ifPresent(s -> description.appendText("should status should contain ").appendValue(s));
            }

            @Override
            protected void describeMismatchSafely(Address a, Description description) {
                if (a.getStatus() == null) {
                    description.appendText("address.status is absent");
                }
                if (ready != a.getStatus().isReady()) {
                    description.appendText("ready was ").appendValue(a.getStatus().isReady());
                }
                if (actualPlan.isPresent()) {
                    if (a.getStatus().getPlanStatus() == null) {
                        description.appendText("address.status.planStatus is absent");
                    } else if (!actualPlan.get().equals(a.getStatus().getPlanStatus().getName())) {
                        description.appendText("actual plan was ").appendValue(a.getStatus().getPlanStatus().getName());
                    }
                }
                if (messageContains.isPresent()) {
                    String cc = String.join(":", a.getStatus().getMessages());
                    description.appendText("messages were: ").appendValue(cc);
                }

            }

            @Override
            public boolean matchesSafely(Address a) {
                if (a.getStatus() == null) {
                    return false;
                }
                if (ready != a.getStatus().isReady()) {
                    return false;
                }
                if (actualPlan.isPresent() && !actualPlan.get().equals(a.getStatus().getPlanStatus().getName())) {
                    return false;
                }
                if (messageContains.isPresent()) {
                    Optional<String> match = a.getStatus().getMessages().stream().filter(m -> m.contains(messageContains.get())).findFirst();
                    return match.isPresent();
                } else {
                    return true;
                }
            }
        };
    }

    public void doTestUnknownAddressPlan(AddressSpace addressSpace, List<StageHolder> stageHolders) throws Exception {

       resourcesManager.createAddressSpace(addressSpace);

        do {
            log.info("Starting stage");

            List<StageHolder.Stage> stages = stageHolders.stream().filter(StageHolder::hasStage).map(StageHolder::popStage).collect(Collectors.toList());

            if (stages.isEmpty()) {
                break;
            }

            stages.stream().map(StageHolder.Stage::getAddress).forEach(address -> {
                Kubernetes.getInstance().getAddressClient(address.getMetadata().getNamespace()).createOrReplace(address);
            });

            stages.forEach(s -> {
                AtomicReference<String> lastMatch = new AtomicReference<>();

                boolean rv = TestUtils.waitUntilCondition(() -> {
                    Address current = resourcesManager.getAddress(s.getAddress().getMetadata().getNamespace(), s.getAddress());
                    Matcher<Address> matcher = s.getMatcher();
                    boolean matches = matcher.matches(current);
                    StringDescription desc = new StringDescription();
                    matcher.describeMismatch(current, desc);
                    lastMatch.set(desc.toString());
                    if (matches) {
                        log.info("Address {} is now in expected state: {}", current.getMetadata().getName(), current.getStatus());
                    } else {
                        log.info("Address {} is not in expected state: {} {}", current.getMetadata().getName(), lastMatch, current.getStatus());
                    }
                    return matches;
                }, Duration.ofMinutes(2), Duration.ofSeconds(10));
                assertTrue(rv, String.format("address %s did not reach desired state : %s", s.getAddress().getMetadata().getName(), lastMatch));
            });
        } while(true);
    }

    public class StageHolder {
        private final AddressSpace addressSpace;
        private final String addressName;
        private final List<Stage> stages = new ArrayList<>();

        public class Stage {
            private final String plan;
            private final Matcher<Address> matcher;

            Stage(Matcher<Address> matcher, String plan) {
                this.plan = plan;
                this.matcher = matcher;
            }

            public Address getAddress() {
                return new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, addressName))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress(StageHolder.this.addressName)
                        .withPlan(this.plan)
                        .endSpec()
                        .build();
            }

            public Matcher<Address> getMatcher() {
                return matcher;
            }
        }

        public StageHolder(AddressSpace addressSpace, String addressName) {
            this.addressSpace = addressSpace;
            this.addressName = addressName;
        }

        public StageHolder addStage(String plan, Matcher<Address> addressMatcher) {
            stages.add(new Stage(addressMatcher, plan));
            return this;
        }

        public boolean hasStage() {
            return !stages.isEmpty();
        }

        public Stage popStage() {
            return stages.remove(0);
        }
    }
}
