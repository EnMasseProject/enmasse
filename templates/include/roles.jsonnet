{
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

  // Cluster role for address-controller service account
  enmasse_admin_role::
    {
      "apiVersion": "v1",
      "kind": "ClusterRole",
      "metadata": {
        "name": "enmasse-admin"
      },
      "rules": [
        {
          "apiGroups": [
            "",
            "project.openshift.io",
            "authentication.k8s.io",
            "authorization.k8s.io"
          ],
          "resources": [
            "projectrequests",
            "localsubjectaccessreviews",
            "tokenreviews"
          ],
          "verbs": [
            "create"
          ]
        }
      ]
    },


  cluster_reader::
  {
    "apiVersion": "v1",
    "kind": "ClusterRole",
    "metadata": {
      "name": "cluster-reader"
    },
    "rules": [
      {
        "apiGroups": [
          ""
        ],
        "resources": [
          "pods"
        ],
        "verbs": [
          "get",
          "watch",
          "list"
        ]
      }
    ]
  }
}
