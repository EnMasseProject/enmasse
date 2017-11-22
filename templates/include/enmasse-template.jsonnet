local storage = import "storage-template.jsonnet";
local common = import "common.jsonnet";
local standardInfra = import "standard-space-infra.jsonnet";
local brokeredInfra = import "brokered-space-infra.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local images = import "images.jsonnet";
{
  // Role for address-controller service account
  namespace_admin_role::
    {
      "apiVersion": "v1",
      "kind": "ClusterRole",
      "metadata": {
        "name": "enmasse-namespace-admin"
      },
      "rules": [
        {
          "apiGroups": [
            "authentication.k8s.io",
            "rbac.authorization.k8s.io",
            "authorization.k8s.io",
          ],
          "resources": [
            "tokenreviews",
            "rolebindings",
            "roles",
            "localsubjectaccessreviews"
          ],
          "verbs": [
            "create"
          ]
        },
        {
          "apiGroups": [
            "",
            "authorization.openshift.io",
            "extensions",
            "route.openshift.io"
          ],
          "resources": [
            "namespaces",
            "rolebindings",
            "policybindings",
            "pods",
            "configmaps",
            "deployments",
            "replicasets",
            "routes",
            "secrets",
            "services",
            "persistentvolumeclaims",
            "serviceaccounts"
          ],
          "verbs": [
            "get",
            "list",
            "create",
            "delete",
            "update",
            "watch",
            "patch"
          ]
        }
      ]
    },

  // Role for address space view access for address space service account
  infra_view_role::
    {
      "apiVersion": "v1",
      "kind": "ClusterRole",
      "metadata": {
        "name": "enmasse-infra-view"
      },
      "rules": [
        {
          "apiGroups": [
            "",
            "extensions"
          ],
          "resources": [
            "pods",
            "configmaps",
            "deployments"
          ],
          "verbs": [
            "list",
            "get",
            "watch"
          ]
        }
      ]
    },

  // Role for address administrators allowed to create/delete addresses
  address_admin_role::
    {
      "apiVersion": "v1",
      "kind": "ClusterRole",
      "metadata": {
        "name": "enmasse-address-admin"
      },
      "rules": [
        {
          "apiGroups": [
            ""
          ],
          "resources": [
            "configmaps"
          ],
          "verbs": [
            "create",
            "delete",
            "list",
            "get",
            "watch",
            "update",
            "patch"
          ]
        }
      ]
    },

  // Role for address space administrators
  addressspace_admin_role::
    {
      "apiVersion": "v1",
      "kind": "ClusterRole",
      "metadata": {
        "name": "enmasse-addressspace-admin"
      },
      "rules": [
        {
          "resources": [
            "configmaps"
          ],
          "verbs": [
            "get",
            "list",
            "watch",
            "create",
            "update",
            "patch",
            "delete"
          ]
        }
      ]
    },

  local me = self,

  cluster_roles::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [
      me.namespace_admin_role,
      me.infra_view_role,
      me.addressspace_admin_role,
      me.address_admin_role,
    ]
  },

  generate(with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": "enmasse"
    },
    "objects": [ storage.template(false, false),
                 storage.template(false, true),
                 storage.template(true, false),
                 storage.template(true, true),
                 standardInfra.generate(with_kafka),
                 brokeredInfra.template,
                 addressController.deployment("${ADDRESS_CONTROLLER_REPO}", "", "${ENMASSE_CA_SECRET}", "${ADDRESS_CONTROLLER_CERT_SECRET}", "${ENVIRONMENT}", "${ENABLE_RBAC}"),
                 addressController.internal_service,
                 restapiRoute.route("${RESTAPI_HOSTNAME}") ],
    "parameters": [
      {
        "name": "RESTAPI_HOSTNAME",
        "description": "The hostname to use for the exposed route for the REST API"
      },
      {
        "name": "ADDRESS_CONTROLLER_REPO",
        "description": "The docker image to use for the address controller",
        "value": images.address_controller
      },
      {
        "name": "ENMASSE_CA_SECRET",
        "description": "Name of the secret containing the EnMasse CA",
        "value": "enmasse-ca"
      },
      {
        "name": "ADDRESS_CONTROLLER_CERT_SECRET",
        "description": "Name of the secret containing the address controller certificate",
        "value": "address-controller-cert"
      },
      {
        "name": "ENABLE_RBAC",
        "description": "Enable RBAC for REST API authentication and authorization",
        "value": "false"
      },
      {
        "name": "ENVIRONMENT",
        "description": "The environment for this EnMasse instance (for instance development, testing or production).",
        "value": "development"
      }
    ]
  }
}
