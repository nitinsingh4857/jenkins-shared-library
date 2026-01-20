def call(Map cfg) {

    stage('Clone') {
        echo 'Repository already checked out by Jenkins'
    }

    if (cfg.KEEP_APPROVAL_STAGE) {
        stage('User Approval') {
            input message: "Approve deployment to ${cfg.ENVIRONMENT}?",
                  ok: 'Deploy'
        }
    }

    stage('Ansible Playbook Execution') {
        dir('ansible') {
            git url: 'https://github.com/nitinsingh4857/ansible-k8s-automation.git',
                branch: 'main'

            sh """
              ansible-playbook deploy-k8s.yml \
              -i inventories/${cfg.ENVIRONMENT}/hosts
            """
        }
    }

    stage('Notification') {
        slackSend(
            channel: cfg.SLACK_CHANNEL_NAME,
            message: cfg.ACTION_MESSAGE
        )
    }
}




