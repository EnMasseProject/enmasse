def storeArtifacts() {
    sh './systemtests/scripts/store_kubernetes_info.sh "artifacts/openshift-info/"'
    sh './systemtests/scripts/collect_logs.sh "/tmp/testlogs" "artifacts/openshift-logs"'
    sh 'rm -rf /tmp/testlogs'
}

def tearDownOpenshift() {
    echo "tear down openshift"
    sh './systemtests/scripts/teardown-openshift.sh'
}

def makePlot() {
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

def runSystemtests(String coreDir, String tag, String profile, String testCases) {
    sh "sudo ./systemtests/scripts/enable_core_dumps.sh ${coreDir}"
    sh "./systemtests/scripts/run_test_component.sh templates/build/enmasse-${tag} ${profile} ${testCases}"
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

def postAction(String coresDir) {
    storeArtifacts() //store artifacts if build was aborted - due to timeout reached
    //store test results from build and system tests
    junit '**/TEST-*.xml'
    //archive test results and openshift logs
    archive '**/TEST-*.xml'
    archive 'templates/install/**'
    sh "sudo ./systemtests/scripts/compress_core_dumps.sh ${coresDir} artifacts"
    archive 'artifacts/**'
    tearDownOpenshift()
    sh "./systemtests/scripts/check_and_clear_cores.sh ${coresDir}"
    makePlot()
}

return this