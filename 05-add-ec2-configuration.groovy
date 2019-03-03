import com.amazonaws.services.ec2.model.InstanceType
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.Domain
import hudson.model.*
import hudson.plugins.ec2.AmazonEC2Cloud
import hudson.plugins.ec2.AMITypeData
import hudson.plugins.ec2.EC2Tag
import hudson.plugins.ec2.SlaveTemplate
import hudson.plugins.ec2.SpotConfiguration
import hudson.plugins.ec2.UnixData
import jenkins.model.Jenkins
 
// parameters
def SlaveTemplateUsEast1Parameters = [
  ami:                      'ami-04c4518821aad93f7',
  associatePublicIp:        true,
  connectBySSHProcess:      true,
  connectUsingPublicIp:     true,
  customDeviceMapping:      '',
  deleteRootOnTermination:  true,
  description:              'Jenkins slave EC2 asia East 1',
  ebsOptimized:             false,
  iamInstanceProfile:       '',
  idleTerminationMinutes:   '5',
  initScript:               '',
  instanceCapStr:           '2',
  jvmopts:                  '',
  labelString:              'aws.ec2.us.east.jenkins.slave',
  launchTimeoutStr:         '',
  numExecutors:             '2',
  remoteAdmin:              'ec2-user',
  remoteFS:                 '',
  securityGroups:           'sg-0539806c088db6c31',
  stopOnTerminate:          false,
  subnetId:                 '',
  tags:                     new EC2Tag('Name', 'jenkins-slave'),
  tmpDir:                   '',
  type:                     't1.micro',
  useDedicatedTenancy:      false,
  useEphemeralDevices:      true,
  usePrivateDnsName:        false,
  userData:                 '',
  zone:                     'ap-southeast-1a'
]
 
def AmazonEC2CloudParameters = [
  cloudName:      'MyEC2',
  credentialsId:  'jenkins-aws-key',
  instanceCapStr: '2',
  privateKey:     '''-----BEGIN RSA PRIVATE KEY-----
MIIEpQIBAAKCAQEA3/BAzUN7YqynRKuGOHbMFMQaGCoURp0wTTzW1cEDwNZoBWNoYE0bvSU5Duwt
t2/n3LiyZh8F7asbefIRRlaoAuCvAyOlkmsIGDaO/igvRAOHG+7K7uX5arLI8qAPogF4OmCbCElv
CHykyePTckvZPiRtRiB1O7UPEx10ZHLXtUeLoV7xgKzA0W3T0Ils+It7tjuhp8yp66DXndC1ka77
M9Kv2BeM60l1iOfyHXzsBdiNUwvJokLsrdeog9bHFWKTi9Nx97ddG7G+NnB/r98Ua2s9q+ErwbMj
Q0cgn7YdNfOY7E88vP6amifg3iLEcvTKASTig9no/Ne6QUeYSfssGwIDAQABAoIBAQC6x2LlUbGp
/aOODRpoIf3aHC4/lWlP9G+DHIl7w/9jafFc/Srjl6zZOCC9J5SB+Z6EZLC3qIyDTUIflh1+c2yy
6cBPd/3zf75Co87kgZPawK6CR8uPMtWzfudIJuSjFWK4Hjdw0/7+LygrFBEatdS+lhdmdFATutKV
+O1JktgD49c4T14AQxLrCuDdOcIlziZbZ/c7Fkl4cOBIN591oI4OgimRRtqRYjajBmq2GOmLUhLK
l8rh1lqF3m0taEVf/0db9iqZ6n/+jhpnsFlsyXZKBR71rDsR2IXueo+ByDSOJRjN2DBMZBbttcpl
TjSq5CJTzRDgunfAlr0XjBo/qawRAoGBAPT/9d4k6wlFExvVBcZ3umdVWEAHldM9A7lR+5gnbVv9
+BrOqGGjR/ra+F/qr/Tvzi1GZ0+4gKh7OylyeNgtPUPMWgVH+XEJSkByn1CUwWgvnZ2EyqsKmxva
7+iyMMk3KTdhd8oe5AbNhLS0W7YiJYfQI+83iNCPqLpYOeMzIYNPAoGBAOn+NmaLn+g+tK0sQWJf
SF9etrCoHzaaQiVhtD1Ne15qjxiQ5vW9iO4ntO1+KDnluEHZIe0f66C61eGFgbNomNL4DRZTWBQ2
YZEqCo8UqHtGywbFIDjOmydcphhZ6b8VJBmJVplwjU2k4cdaqMh0OwQloqv9oW5SuHKJwtAE/Qd1
AoGARqHcclTWZe/CGI4LgjJWTSAvtxX4vdOjVTQrgqwrPRA1DRVzOeLnB41FefGhFM5l9GuNsVAR
14Dfh413BWvVc8xgT5F2en4hJ/9jqsmYEL9Zg2YbP8BpA1jVPZLRudDrUwoIjP7m4ocxsR8mKZYi
l8sL8RVjni0sibyRw8yj5sUCgYEAyzHdbdlL6HMjlMo8kT9q6p6mCOxGBrWYQTwCN8XkHw5r6wpR
9g48LYu/mwkVx4lMI6p5wqAYjwAQIYI9Kl3ncDTFsKB5eTvsKBIZDz0CPeMxBVUDcTFBHMJg3f6e
dDfYnHBeCmrp2gYXpnV84m3EngpMpAxhNhuRXq1wxhG+5M0CgYEApRL0OhK+RZ+po46BmWXFjBeC
zcwb+MMM4fZUvVbnUJn9+YZvTWPCA+EYVbzG+PmSPlBFP/OcEs8C2iMgY52jXIJwrUKcvu5ynfRI
kqcUTES5J1mMaASqoau/2hu+xJx6BSLUB5kqEXKz9TOJ0cu3KoeknY7Pllv6AA3UzsjllJs=
-----END RSA PRIVATE KEY-----''',
  region: 'ap-southeast-1',
  useInstanceProfileForCredentials: false
]
 
def AWSCredentialsImplParameters = [
  id:           'jenkins-aws-key',
  description:  'Jenkins AWS IAM key',
  accessKey:    'AKIAJ6IGM5YN2TYPQMVQ',
  secretKey:    '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'
]
 
// https://github.com/jenkinsci/aws-credentials-plugin/blob/aws-credentials-1.23/src/main/java/com/cloudbees/jenkins/plugins/awscredentials/AWSCredentialsImpl.java
AWSCredentialsImpl aWSCredentialsImpl = new AWSCredentialsImpl(
  CredentialsScope.GLOBAL,
  AWSCredentialsImplParameters.id,
  AWSCredentialsImplParameters.accessKey,
  AWSCredentialsImplParameters.secretKey,
  AWSCredentialsImplParameters.description
)
 
// https://github.com/jenkinsci/ec2-plugin/blob/ec2-1.38/src/main/java/hudson/plugins/ec2/SlaveTemplate.java
SlaveTemplate slaveTemplateUsEast1 = new SlaveTemplate(
  SlaveTemplateUsEast1Parameters.ami,
  SlaveTemplateUsEast1Parameters.zone,
  null,
  SlaveTemplateUsEast1Parameters.securityGroups,
  SlaveTemplateUsEast1Parameters.remoteFS,
  InstanceType.fromValue(SlaveTemplateUsEast1Parameters.type),
  SlaveTemplateUsEast1Parameters.ebsOptimized,
  SlaveTemplateUsEast1Parameters.labelString,
  Node.Mode.NORMAL,
  SlaveTemplateUsEast1Parameters.description,
  SlaveTemplateUsEast1Parameters.initScript,
  SlaveTemplateUsEast1Parameters.tmpDir,
  SlaveTemplateUsEast1Parameters.userData,
  SlaveTemplateUsEast1Parameters.numExecutors,
  SlaveTemplateUsEast1Parameters.remoteAdmin,
  null,
  SlaveTemplateUsEast1Parameters.jvmopts,
  SlaveTemplateUsEast1Parameters.stopOnTerminate,
  SlaveTemplateUsEast1Parameters.subnetId,
  [SlaveTemplateUsEast1Parameters.tags],
  SlaveTemplateUsEast1Parameters.idleTerminationMinutes,
  SlaveTemplateUsEast1Parameters.usePrivateDnsName,
  SlaveTemplateUsEast1Parameters.instanceCapStr,
  SlaveTemplateUsEast1Parameters.iamInstanceProfile,
  SlaveTemplateUsEast1Parameters.deleteRootOnTermination,
  SlaveTemplateUsEast1Parameters.useEphemeralDevices,
  SlaveTemplateUsEast1Parameters.useDedicatedTenancy,
  SlaveTemplateUsEast1Parameters.launchTimeoutStr,
  SlaveTemplateUsEast1Parameters.associatePublicIp,
  SlaveTemplateUsEast1Parameters.customDeviceMapping,
  SlaveTemplateUsEast1Parameters.connectBySSHProcess,
  SlaveTemplateUsEast1Parameters.connectUsingPublicIp
)
 
// https://github.com/jenkinsci/ec2-plugin/blob/ec2-1.38/src/main/java/hudson/plugins/ec2/AmazonEC2Cloud.java
AmazonEC2Cloud amazonEC2Cloud = new AmazonEC2Cloud(
  AmazonEC2CloudParameters.cloudName,
  AmazonEC2CloudParameters.useInstanceProfileForCredentials,
  AmazonEC2CloudParameters.credentialsId,
  AmazonEC2CloudParameters.region,
  AmazonEC2CloudParameters.privateKey,
  AmazonEC2CloudParameters.instanceCapStr,
  [slaveTemplateUsEast1]
)
 
// get Jenkins instance
Jenkins jenkins = Jenkins.getInstance()
 
// get credentials domain
def domain = Domain.global()
 
// get credentials store
def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
 
// add credential to store
store.addCredentials(domain, aWSCredentialsImpl)
 
// add cloud configuration to Jenkins
jenkins.clouds.add(amazonEC2Cloud)
 
// save current Jenkins state to disk
jenkins.save()