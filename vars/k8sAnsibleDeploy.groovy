def call(cfg) {
    node {
        stage('Clone Ansible Repo') {
            echo "Cloning Ansible repo from ${cfg.ANSIBLE_REPO_URL}..."
            checkout([$class: 'GitSCM',
                      branches: [[name: cfg.ANSIBLE_REPO_BRANCH]],
                      userRemoteConfigs: [[url: cfg.ANSIBLE_REPO_URL]]])
        }

        stage('User Approval') {
            input message: "Approve deployment to ${cfg.ENVIRONMENT}?", ok: "Deploy"
        }

        stage('Run Ansible Playbook') {
            withEnv(["K8S_AUTH_KUBECONFIG=/var/lib/jenkins/.kube/config"]) {
                sh """
                    ansible-playbook ${cfg.PLAYBOOK_PATH} -i ${cfg.INVENTORY_PATH} --extra-vars "env=${cfg.ENVIRONMENT}"
                """
            }
        }

        stage('Notify') {
            slackSend(
                channel: cfg.SLACK_CHANNEL_NAME.startsWith('#') ? cfg.SLACK_CHANNEL_NAME : "#${cfg.SLACK_CHANNEL_NAME}",
                color: 'good',
                botUser: true,
                tokenCredentialId: cfg.SLACK_CREDENTIAL_ID,
                message: cfg.ACTION_MESSAGE
            )
        }
    }
}



