def call(Closure body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def cfg = [:]   // ✅ DECLARED EARLY (IMPORTANT)

    pipeline {
        agent any

        stages {

            stage('Load Configuration') {
                steps {
                    script {
                        cfg = readYaml file: config.configFile
                        echo "Loaded configuration for ${cfg.ENVIRONMENT}"
                    }
                }
            }

            stage('Clone') {
                steps {
                    echo "Repository already checked out by Jenkins"
                }
            }

            stage('User Approval') {
                when {
                    expression { cfg.KEEP_APPROVAL_STAGE == true }
                }
                steps {
                    input message: "Approve deployment to ${cfg.ENVIRONMENT}?",
                          ok: "Deploy"
                }
            }

            stage('Ansible Playbook Execution') {
                steps {
                    script {
                        sh """
                        ansible-playbook ${cfg.ANSIBLE.PLAYBOOK} \
                        -i ${cfg.ANSIBLE.INVENTORY}
                        """
                    }
                }
            }
        }

        post {
            success {
                script {
                    if (cfg?.SLACK_CHANNEL_NAME) {
                        slackSend(
                            channel: cfg.SLACK_CHANNEL_NAME,
                            message: "✅ SUCCESS: ${cfg.ACTION_MESSAGE}"
                        )
                    }
                }
            }
            failure {
                script {
                    if (cfg?.SLACK_CHANNEL_NAME) {
                        slackSend(
                            channel: cfg.SLACK_CHANNEL_NAME,
                            message: "❌ FAILED: ${cfg.ACTION_MESSAGE}"
                        )
                    }
                }
            }
        }
    }
}

