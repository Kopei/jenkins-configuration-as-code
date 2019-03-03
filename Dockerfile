# Latest LTS release
FROM jenkins/jenkins:2.150.2

RUN /usr/local/bin/install-plugins.sh workflow-aggregator:2.6 matrix-auth:2.3 ssh-slaves:1.29.4 scripttrigger:0.34 ec2:1.42 groovy-postbuild:2.4.3  jdk-tool:1.2 command-launcher:1.3 git:3.9.3 configuration-as-code:1.7 configuration-as-code-support:1.7

# The systemd service on host will copy the private SSH key to this folder
# (i.e. not volume mount it) and Jenkins will read it on startup and use it
# to connect to its slaves.
RUN mkdir /usr/share/jenkins/ref/files /var/jenkins_home/casc_configs

ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

COPY 01-setup-users.groovy /usr/share/jenkins/ref/init.groovy.d/01-setup-users.groovy
COPY plugins /var/jenkins_home/casc_configs/