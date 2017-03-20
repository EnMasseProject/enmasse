# Changing EnMasse components

See the README file in the corresponding repo of the component you want to build.

# Changing the OpenShift templates

The OpenShift templates for EnMasse are generated from [jsonnet](jsonnet.org) files.

To make changes to the templates, edit the jsonnet files in the include/ folder. To generate the templates,
run:

    git submodule update --init
    make

This will generate updated templates in the generated/ folder.
