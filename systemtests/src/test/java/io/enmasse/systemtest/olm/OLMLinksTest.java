/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.olm;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.NON_PR;
import static io.enmasse.systemtest.TestTag.OLM;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;

import io.enmasse.systemtest.EnmasseInstallType;
import io.enmasse.systemtest.OLMInstallationType;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.condition.SupportedInstallType;
import io.enmasse.systemtest.executor.ExecutionResultData;
import io.enmasse.systemtest.operator.EnmasseOperatorManager;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.annotations.SeleniumChrome;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.vertx.core.json.JsonObject;

@Tag(OLM)
@Tag(ACCEPTANCE)
@Tag(NON_PR)
@SupportedInstallType(value = EnmasseInstallType.OLM, olmInstallType = OLMInstallationType.DEFAULT)
@SeleniumChrome
public class OLMLinksTest extends TestBase implements ITestBaseIsolated {

    SeleniumProvider selenium = SeleniumProvider.getInstance();

    @Test
    void testOLMLinks() {
        String namespace = EnmasseOperatorManager.getInstance().getNamespaceByOlmInstallationType(OLMInstallationType.DEFAULT);
        ExecutionResultData result = KubeCMDClient.runOnCluster("get", "csv", "-n", namespace, "-o", "json", "-l", "app=enmasse");
        JsonObject csvList = new JsonObject(result.getStdOut());
        JsonObject csv = csvList.getJsonArray("items").getJsonObject(0);

        List<JsonObject> links = csv.getJsonObject("spec").getJsonArray("links").stream().map(o -> (JsonObject) o).collect(Collectors.toList());

        boolean privateRepo = links.stream().noneMatch(o -> o.getString("name").equals("GitHub"));


        for (JsonObject link : links) {
            String name = link.getString("name");
            String url = link.getString("url");
            if (name.equals("GitHub")) {

                assertTrue(url.equals("https://github.com/EnMasseProject/enmasse"));
                continue;

            } else if (name.equals("Documentation")) {

                assertTrue(url.equals(environment.enmasseOlmDocsUrl()));

            } else if (name.equals(environment.enmasseOlmAboutName())) {

                assertTrue(url.equals(environment.enmasseOlmAboutUrl()));

            } else {
                Assertions.fail("Unexpected link " + name + " url " + url);
            }

            testUrl(url, privateRepo);

        }

        testUrl(environment.enmasseOlmDocConfigUrl(), privateRepo);

    }

    private void testUrl(String url, boolean privateRepo) {
        if (privateRepo) {
            assertFalse(url.contains("enmasse.io"));
        }
        try {
            selenium.getDriver().get(url);
            selenium.getDriverWait().withTimeout(Duration.ofSeconds(30)).until(ExpectedConditions.urlContains(url));
        } finally {
            selenium.takeScreenShot();
        }
    }

}
