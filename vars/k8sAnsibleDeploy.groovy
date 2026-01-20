def call(Map cfg) {
    node {
        try {
            // -----------------------------
            // Clone Ansible Repo
            // -----------------------------
            stage("Clone Ansible Repo") {
                echo "Cloning Ansible repo from ${cfg.ANSIBLE_REPO}..."
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/main']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'ansible']],
                    userRemoteConfigs: [[url: cfg.ANSIBLE_REPO]]])
            }

            // -----------------------------
            // User Approval
            // -----------------------------
            stage("User Approval") {
                if(cfg.KEEP_APPROVAL_STAGE) {
                    input message: "Approve deployment to ${cfg.ENVIRONMENT}?", ok: "Deploy"
                } else {
                    echo "Approval stage skipped"
                }
            }

            // -----------------------------
            // Run Ansible Playbook
            // -----------------------------
            stage("Ansible Playbook Execution") {
                sh """
                ansible-playbook ansible/${cfg.ANSIBLE.PLAYBOOK} \
                                 -i ansible/${cfg.ANSIBLE.INVENTORY}
                """
            }

            // -----------------------------
            // Slack Notification
            // -----------------------------
            stage("Notification") {
                slackSend(
                    channel: cfg.SLACK_CHANNEL_NAME,
                    message: cfg.ACTION_MESSAGE,
                    botUser: true,
                    tokenCredentialId: "slack-token" // Replace with your Jenkins Slack credential ID
                )
            }

        } catch (err) {
            // Send Slack notification on failure
            echo "Deployment failed: ${err}"
            slackSend(
                channel: cfg.SLACK_CHANNEL_NAME,
                message: "Deployment FAILED for ${cfg.ENVIRONMENT}!\nError: ${err}",
                botUser: true,
                tokenCredentialId: "slack-token"
            )
            error("Pipeline failed!")
        }
    }
}


