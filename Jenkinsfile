node {
    def containerName = "nextcode/csa-jenkins"
    def buildId = "v${env.BUILD_NUMBER}-${env.BRANCH_NAME}".replaceAll(/\//, '_')
    def dockerTag = "${containerName}:${buildId}"
    def dockerCredentialsId = 'dockerhub-nextcodebuilder'

    def image = null
    def buildDockerImageFromFeatureBranch = false

    stage('Cleanup workspace') {
        step([$class: 'WsCleanup'])
    }

    try {
        stage('Checkout') {
            checkout scm
        }

        stage('Building Docker image') {
            image = docker.build dockerTag
        }

        stage('Read version.yml') {
            def yamlContent = readYaml file: 'version.yml'
            env.BUILD_VERSION = yamlContent.major + '.' + yamlContent.minor + '.' + yamlContent.patch
        }

        stage('Pushing Docker image') {
            withDockerRegistry([credentialsId: dockerCredentialsId]) {
                if (env.BRANCH_NAME == 'master') {
                    image.push(env.BUILD_VERSION)
                } else if (env.BRANCH_NAME == 'develop' || buildDockerImageFromFeatureBranch) {
                    image.push(env.BRANCH_NAME)
                }
            }
        }

        if (env.BRANCH_NAME == 'master') {
            stage('Tagging git repo with release version') {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nc-jenkins-bitbucket-user', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
                    sh('git config --global user.name "Jenkins CI server"')
                    sh('git config --global user.email "csa-data-group@wuxinextcode.com"')
                    sh("git tag -a \"${env.BUILD_VERSION}\" -m \"Tagged in Jenkis CI loop\"")
                    sh("git push https://${GIT_USERNAME}:${GIT_PASSWORD}@bitbucket.org/nextcode-health/csa-jenkins --tags")
                }
            }
        }

        if (env.BRANCH_NAME == 'develop') {
            stage('Triggering CI build') {
                sh 'curl -i https://build.nextcode.com/ci/job/Trigger%20Deployment/build?token=rosalega_g0dur_t0ken_hestur_rafhlada&cause=CSA+Jenkins'
            }
        }
    }

    finally {
        stage('Cleanup') {
            sh "docker rmi ${dockerTag}"
        }
    }
}
