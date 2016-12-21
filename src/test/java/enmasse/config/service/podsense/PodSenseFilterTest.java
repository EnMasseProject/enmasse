package enmasse.config.service.podsense;

import io.fabric8.kubernetes.api.model.*;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class PodSenseFilterTest {
    @Test
    public void testFilter() {
        Set<PodResource> pods = new HashSet<>();
        pods.add(createPod("pod1", "Terminating"));
        pods.add(createPod("pod2", "Running"));
        pods.add(createPod("pod3", "Pending"));

        Set<PodResource> filtered = pods.stream()
                .filter(new PodSenseSubscriptionConfig().getResourceFilter())
                .collect(Collectors.toSet());
        assertThat(filtered.size(), is(1));
        PodResource resource = filtered.iterator().next();
        assertNotNull(resource);
        assertThat(resource.getName(), is("pod2"));
    }

    private static PodResource createPod(String name, String phase) {
        return new PodResource(new PodBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .build())
                .withSpec(new PodSpecBuilder()
                        .withContainers(new ContainerBuilder()
                                .withName("c")
                                .build())
                        .build())
                .withStatus(new PodStatusBuilder()
                        .withPhase(phase)
                        .build())
                .build());
    }
}
