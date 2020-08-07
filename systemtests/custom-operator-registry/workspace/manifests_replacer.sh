# replace this file's content with some logic similar to example_manifests_replacer.sh
echo "Doing nothing"
sed -e '/.*replaces: enmasse.*/d' -i manifests/enmasse.clusterserviceversion.yaml
