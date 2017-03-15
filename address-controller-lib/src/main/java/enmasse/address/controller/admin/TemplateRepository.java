package enmasse.address.controller.admin;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.DoneableTemplate;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.ClientTemplateResource;

/**
 * A repository of templates
 */
public class TemplateRepository {
    private final OpenShiftClient openShiftClient;
    public TemplateRepository(OpenShiftClient openShiftClient) {
        this.openShiftClient = openShiftClient;
    }

    public ClientTemplateResource<Template,KubernetesList,DoneableTemplate> getTemplate(String templateName) {
        return openShiftClient.templates().withName(templateName);
    }
}
