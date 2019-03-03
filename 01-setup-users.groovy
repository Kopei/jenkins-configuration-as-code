import jenkins.model.*
import hudson.model.*
import hudson.security.*

def jenkins = Jenkins.getInstance()

def adminUser = System.getenv('JENKINS_ADMIN_USERNAME') ?: 'admin'
def adminPass = System.getenv('JENKINS_ADMIN_PASSWORD') ?: 'admin.password.123'

jenkins.setSecurityRealm(new HudsonPrivateSecurityRealm(false))
def strategy = new GlobalMatrixAuthorizationStrategy()

def admin = jenkins.getSecurityRealm().createAccount(adminUser, adminPass)
admin.save()

def supportUser = System.getenv('JENKINS_SUPPORT_USERNAME') ?: 'support'
def supportPass = System.getenv('JENKINS_SUPPORT_PASSWORD') ?: 'support.password.123'
def support = jenkins.getSecurityRealm().createAccount(supportUser, supportPass)
support.save()

strategy.add(Jenkins.ADMINISTER, adminUser)

// support permissions as per https://nextcode.atlassian.net/wiki/spaces/CB/pages/607060077/Add+the+Jenkins+Support+User
strategy.add(Jenkins.READ, supportUser)
strategy.add(Item.BUILD, supportUser)
strategy.add(Item.CANCEL, supportUser)
strategy.add(Item.READ, supportUser)
strategy.add(Item.WORKSPACE, supportUser)
strategy.add(Run.DELETE, supportUser)
strategy.add(View.READ, supportUser)

jenkins.setAuthorizationStrategy(strategy)
jenkins.save()

//jenkins.securityRealm = new LDAPSecurityRealm(
//        “ldap://ldap1.example.com:389 ldap://ldap2.example.com:389”,
//        “DC=users,DC=example,DC=com”, “”, “(userPrincipalName={0})”, “”,
//        “”, new FromUserRecordLDAPGroupMembershipStrategy(),
//        “CN=ldap-bind-user,DC=users,DC=example,DC=com”,
//        Secret.fromString(“my-secret-password”),
//        false, false, null, null, ‘cn’, ‘mail’, null, null)