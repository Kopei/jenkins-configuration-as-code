import jenkins.model.*
import hudson.model.*
import hudson.slaves.*
import hudson.plugins.sshslaves.*

import groovy.json.JsonSlurper

import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*;
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.domains.*;
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.impl.*;
import hudson.plugins.sshslaves.*;
import jenkins.model.*;

private credentials_for_username(String username) {
    def username_matcher = CredentialsMatchers.withUsername(username)
    def available_credentials =
            CredentialsProvider.lookupCredentials(
                    StandardUsernameCredentials.class,
                    Jenkins.getInstance(),
                    hudson.security.ACL.SYSTEM,
                    new SchemeRequirement("ssh")
            )

    return CredentialsMatchers.firstOrNull(
            available_credentials,
            username_matcher
    )
}

BaseStandardCredentials create_or_update_credentials(String username, String password, String description="", String private_key_content="") {
    def global_domain = Domain.global()
    def provider = 'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
    def credentials_store = Jenkins.instance.getExtensionList(provider)[0].getStore()

    def credentials

    key_source = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(private_key_content)

    credentials = new BasicSSHUserPrivateKey(
            CredentialsScope.GLOBAL,
            null,
            username,
            key_source,
            password,
            description
    )

    // Create or update the credentials in the Jenkins instance
    def existing_credentials = credentials_for_username(username)

    if(existing_credentials != null) {
        credentials_store.updateCredentials(global_domain, existing_credentials, credentials)
    } else {
        credentials_store.addCredentials(global_domain, credentials)
    }
    existing_credentials = credentials_for_username(username)
    return existing_credentials
}


String slaves_config_file_path = "/usr/share/jenkins/ref/files/slaves.json"
String slaves_config_content = new File(slaves_config_file_path).text
def slaves_config_json = new JsonSlurper().parseText( slaves_config_content )

String private_key_file_path = "/usr/share/jenkins/ref/files/private.ssh.key"
String private_key_content = new File(private_key_file_path).text

String slave_username = slaves_config_json.username
def credentials = create_or_update_credentials(slave_username, '', "", private_key_content)

for (slave_config in slaves_config_json.list_of_slaves) {
    Slave slave = new DumbSlave(
        slave_config.name,                 // Agent name, usually matches the host computer's machine name
        slave_config.description,          // Agent description
        slaves_config_json.home_dir,       // Workspace on the agent's computer
        slave_config.executors.toString(), // Number of executors, must be String because constructor method with int is deprecated
        Node.Mode.NORMAL,                  // "Usage" field, EXCLUSIVE is "only tied to node", NORMAL is "any"
        slave_config.tag,                  // Labels
        // Launch strategy
        new SSHLauncher(slave_config.ip, 22, SSHLauncher.lookupSystemCredentials(credentials.id), "", null, null, "", "", 60, 3, 15),
        new RetentionStrategy.Always(), // Availability field
        new LinkedList()
    )
    Jenkins.instance.addNode(slave)
}
