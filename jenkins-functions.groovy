def storeArtifacts() {
    sh cmd: './systemtests/scripts/store_kubernetes_info.sh "artifacts/openshift-info/"', name: "Store kubernetes info"
    sh cmd: './systemtests/scripts/collect_logs.sh "/tmp/testlogs" "artifacts/openshift-logs"', name: "Collecting logs"
    sh cmd: 'rm -rf /tmp/testlogs', name: "Clear logs"
}

def tearDownOpenshift() {
    sh cmd: './systemtests/scripts/teardown-openshift.sh', name: "Tear down openshift"
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
    sh cmd: "sudo ./systemtests/scripts/enable_core_dumps.sh ${coreDir}", name: "Enable core dumps"
    sh cmd: "./systemtests/scripts/run_test_component.sh 'templates/build/enmasse-${tag}' '${profile}' '${testCases}'", name: "Running systemtests"
}

def startOpenshift() {
    sh cmd: './systemtests/scripts/setup-openshift.sh "systemtests"', name: "Setup opemshift"
    sh cmd: 'sudo chmod -R 777 /var/lib/origin/ || true', name: "Add rights to oc location"
}

def waitUntilAgentReady() {
    sh cmd: "./systemtests/scripts/wait_until_agent_ready.sh /tmp/agent_ready", name: "Wait until server ready"
}

def buildEnmasse() {
    sh cmd: 'MOCHA_ARGS="--reporter=mocha-junit-reporter" make', name: "Build enmasse"
    sh cmd: 'make docker_tag', name: "Docker tag images"
}

def postAction(String coresDir) {
    storeArtifacts() //store artifacts if build was aborted - due to timeout reached
    //store test results from build and system tests
    junit '**/TEST-*.xml'
    //archive test results and openshift logs
    archive '**/TEST-*.xml'
    archive 'templates/install/**'
    sh cmd: "sudo ./systemtests/scripts/compress_core_dumps.sh ${coresDir} artifacts", name: "Compress core dumps"
    archive 'artifacts/**'
    tearDownOpenshift()
    sh cmd: "./systemtests/scripts/check_and_clear_cores.sh ${coresDir}", name: "Check and clear core dumps"
    makePlot()
}

return this