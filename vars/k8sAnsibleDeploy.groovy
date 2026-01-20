def call(Closure body) {

    // Collect inputs from Jenkinsfile
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    pipeline {
        agent any

        stages {

            stage('Load Configuration') {
                steps {
                    script {
                        cfg = readYaml file: config.configFile
                        echo "Loaded config for ENV: ${cfg.ENVIRONMENT}"
                    }
                }
            }

            stage('Clone') {
                steps {
                    echo "Cloning repository..."
                    checkout scm
                }
            }

            stage('User Approval') {
                when {
                    expression { cfg.KEEP_APPROVAL_STAGE == true }
                }
                steps {
                    input message: "Approve deployment to ${cfg.ENVIRONMENT}?",
                          ok: "Proceed"
                }
            }

            stage('Ansible Playbook Execution') {
                steps {
                    script {
                        sh """
                        ansible-playbook ${cfg.ANSIBLE.PLAYBOOK} \
                          -i ${cfg.ANSIBLE.INVENTORY} \
                          --extra-vars '${cfg.ANSIBLE.EXTRA_VARS}'
                        """
                    }
                }
            }
        }

        post {
            success {
                slackSend(
                    channel: cfg.SLACK_CHANNEL_NAME,
                    message: "✅ SUCCESS: ${cfg.ACTION_MESSAGE}"
                )
            }
            failure {
                slackSend(
                    channel: cfg.SLACK_CHANNEL_NAME,
                    message: "❌ FAILED: ${cfg.ACTION_MESSAGE}"
                )
            }
        }
    }
}
