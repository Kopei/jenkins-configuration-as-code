#!/usr/bin/env bash

docker stop c-jenkins
docker rm c-jenkins

# Ensure layer cache is used building and then clearing out old images
docker build -t c-jenkins .

mkdir -p $HOME/jenkins_home
rm -rf $HOME/jenkins_home/init.groovy.d/

docker create -p 8081:8080 --name c-jenkins \
    -e CASC_JENKINS_CONFIG="/var/jenkins_home/casc_configs" \
    --env-file="${HOME}/.jenkins-env" \
    c-jenkins

docker cp test_run_files/ssh-jenkins-slave-dev-key c-jenkins:/usr/share/jenkins/ref/files/private.ssh.key
docker cp test_run_files/slaves.json c-jenkins:/usr/share/jenkins/ref/files/slaves.json

docker start -a c-jenkins
