node('enmasse') {
    stage ('checkout') {
        git 'https://github.com/EnMasseProject/enmasse.git'
        sh 'git checkout add-jenkins-file'
        sh 'git submodule update --init --recursive'
        sh 'rm -rf artifacts && mkdir -p artifacts'
    }
    stage ('build') {
        try {
            sh './gradlew build -q --rerun-tasks'
        } finally {
            junit '**/TEST-*.xml'
            junit '**/test-results.xml'
        }
    }
    stage ('build docker image') {
        withCredentials([string(credentialsId: 'docker-registry-host', variable: 'DOCKER_REGISTRY')]) {
            sh './gradlew pack buildImage -q --rerun-tasks'
        }
        sh 'cat templates/install/openshift/enmasse.yaml'
    }
    stage ('push docker image') {
        withCredentials([string(credentialsId: 'docker-registry-host', variable: 'DOCKER_REGISTRY'), usernamePassword(credentialsId: 'docker-registry-credentials', passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
            sh './gradlew tagImage pushImage -q'
        }
    }
    stage('system tests') {
        withCredentials([string(credentialsId: 'openshift-host', variable: 'OPENSHIFT_URL'), usernamePassword(credentialsId: 'openshift-credentials', passwordVariable: 'OPENSHIFT_PASSWD', usernameVariable: 'OPENSHIFT_USER')]) {
            try {
                sh 'ARTIFACTS_DIR=artifacts OPENSHIFT_PROJECT=$BUILD_TAG ./systemtests/scripts/run_test_component.sh templates/install /tmp/openshift systemtests'
            } finally {
                junit '**/TEST-*.xml'
            }
        }
    }
    stage('archive artifacts') {
        sh 'ls artifacts'
        archive 'artifacts/*'
    }
}
