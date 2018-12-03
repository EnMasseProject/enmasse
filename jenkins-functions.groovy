def storeArtifacts(String artifactDir) {
    sh "./systemtests/scripts/store_kubernetes_info.sh '${artifactDir}/openshift-info/'"
    sh "./systemtests/scripts/collect_logs.sh '/tmp/testlogs' '${artifactDir}/openshift-logs'"
    sh "cp -rf ${HOME}/.npm/_logs ${artifactDir}/npm-logs || true"
    sh 'rm -rf /tmp/testlogs'
}

def tearDownOpenshift() {
    echo "tear down openshift"
    sh 'sudo ./systemtests/scripts/teardown-openshift.sh'
}

def makeLinePlot() {
    plot csvFileName: 'duration_sum_report.csv',
            csvSeries: [[
                                file: 'artifacts/openshift-logs/logs/timeMeasuring/duration_sum_report.csv',
                                exclusionValues: '',
                                displayTableFlag: false,
                                inclusionFlag: 'OFF',
                                url: '']],
            group: 'TimeReport',
            title: 'Sum of test operations',
            style: 'line',
            exclZero: false,
            keepRecords: false,
            logarithmic: false,
            numBuilds: '',
            useDescr: false,
            yaxis: '',
            yaxisMaximum: '',
            yaxisMinimum: ''
}

def makeStackedPlot() {
    plot csvFileName: 'duration_sum_report.csv',
            csvSeries: [[
                                file: 'artifacts/openshift-logs/logs/timeMeasuring/duration_sum_report.csv',
                                exclusionValues: '',
                                displayTableFlag: false,
                                inclusionFlag: 'OFF',
                                url: '']],
            group: 'TimeReport',
            title: 'Sum of test operations (stacked)',
            style: 'stackedArea',
            exclZero: false,
            keepRecords: false,
            logarithmic: false,
            numBuilds: '',
            useDescr: false,
            yaxis: '',
            yaxisMaximum: '',
            yaxisMinimum: ''
}

def runSystemtests(String coreDir, String profile, String testCases) {
    sh "sudo ./systemtests/scripts/enable_core_dumps.sh ${coreDir}"
    sh "./systemtests/scripts/run_test_component.sh '${profile}' '${testCases}' 'true'"
}

def startOpenshift() {
    sh './systemtests/scripts/setup-openshift.sh "systemtests"'
    sh 'sudo chmod -R 777 /var/lib/origin/ || true'
}

def waitUntilAgentReady() {
    sh "./systemtests/scripts/wait_until_agent_ready.sh /tmp/agent_ready"
}

def buildEnmasse() {
    sh 'MOCHA_ARGS="--reporter=mocha-junit-reporter" make'
    sh 'make docker_tag'
}

def postAction(String coresDir, String artifactDir) {
    storeArtifacts(artifactDir)
    makeLinePlot()
    makeStackedPlot()
    //store test results from build and system tests
    junit testResults: '**/TEST-*.xml', allowEmptyResults: true
    //archive test results and openshift logs
    archiveArtifacts artifacts: '**/TEST-*.xml', allowEmptyArchive: true
    archiveArtifacts artifacts: 'templates/build/**', allowEmptyArchive: true
    sh "sudo ./systemtests/scripts/compress_core_dumps.sh ${coresDir} ${artifactDir}"
    sh "sudo ./systemtests/scripts/wait_until_file_close.sh ${artifactDir}"
    try {
        archiveArtifacts artifacts: "${artifactDir}/**", allowEmptyArchive: true
    } catch(all) {
        echo "Archive failed"
    } finally {
        echo "Artifacts are stored"
    }
    tearDownOpenshift()
    sh "./systemtests/scripts/check_and_clear_cores.sh ${coresDir}"
}

def installEnmasse(String tag, Boolean skipDependencies, Boolean upgrade, Boolean generateTemplates) {
    if (generateTemplates) {
        sh "make -C templates clean"
        sh 'make templates || true'
    }
    sh "./systemtests/scripts/deploy_enmasse.sh false 'templates/build/enmasse-${tag}' true ${skipDependencies} ${upgrade}"
}

def sendMail(address, jobName, buildUrl) {
    mail to:"${address}", subject:"EnMasse build of job ${jobName} has failed", body:"See ${buildUrl}"
}

return this
