import hudson.tasks.Publisher
import hudson.model.FreeStyleProject
import jenkins.model.Jenkins

import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript
import org.jenkinsci.plugins.scriptsecurity.scripts.*


def getEnv(String param, String default_param = null) {
    def envParam = System.getenv(param)
    if (envParam == null) {
        if (default_param != null) {
            return default_param
        }
        System.err.println(" --> NextCODE ERROR: Expected '$param' to be set as a environment variable in this Docker container")
        System.exit(1)
    }
    return envParam
}

def apiUrl = getEnv('csa_api_url')


def postBuildGroovy = """
import jenkins.model.Jenkins
manager.listener.logger.println("POST BUILD: checking if this should run instantly again"); 
def apiUrl = '${apiUrl}/pending_runs.txt'
def matcher = manager.getLogMatcher(".*No sample pipeline step in return data:.*")
if(!matcher?.matches()) {
      def pending = new URL(apiUrl).text.contains('process_pipeline')
 
      if (pending && Jenkins.instance.queue.items.length == 0) {
            manager.listener.logger.println("POST BUILD: trigger is Scheduling another build.");
            def job = Jenkins.instance.getItem('CSA-TP02-Run-Pipeline-Tasks')
            Jenkins.instance.queue.schedule(job)
      }
}
"""
SecureGroovyScript script = new SecureGroovyScript( postBuildGroovy, false, [] )

GroovyPostbuildRecorder newGroovyPostBuild = new GroovyPostbuildRecorder(script, 0, false)


FreeStyleProject project = Jenkins.instance.getJob( "CSA-TP02-Run-Pipeline-Tasks" )

List<Publisher> publishers = project.getPublishersList()
publishers.removeAll { it.class == GroovyPostbuildRecorder }
publishers.add( newGroovyPostBuild )
project.save()

final ScriptApproval sa = ScriptApproval.get();
ScriptApproval.PendingScript s = new ScriptApproval.PendingScript(postBuildGroovy, GroovyLanguage.get(), ApprovalContext.create())
sa.approveScript(s.getHash())