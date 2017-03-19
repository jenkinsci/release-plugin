package hudson.plugins.release.pipeline.ReleaseStep.DescriptorImpl

import hudson.model.AbstractProject
import hudson.model.BuildableItemWithBuildWrappers
import hudson.model.StringParameterDefinition
import hudson.plugins.release.ReleaseWrapper;
def st = namespace('jelly:stapler')
def l = namespace('/lib/layout')
l.ajax {
    def jobName = request.getParameter('job')
    if (jobName != null) {
        def contextName = request.getParameter('context')
        def context = contextName != null ? app.getItemByFullName(contextName) : null
        def project = app.getItem(jobName, context, AbstractProject)

        if (project != null) {
            if (project instanceof BuildableItemWithBuildWrappers) {
                def wrapper = project.getBuildWrappersList().get(ReleaseWrapper.class)

                if (wrapper != null) {
                    def parameterDefinitionList = wrapper.getParameterDefinitions()
                    if (parameterDefinitionList.isEmpty()) {
                        parameterDefinitionList.add(new StringParameterDefinition("RELEASE_VERSION", "", "Release version"))
                        parameterDefinitionList.add(new StringParameterDefinition("DEVELOPMENT_VERSION", "", "Next development version"))
                    }

                    table(width: '100%', class: 'parameters') {
                        for (parameterDefinition in parameterDefinitionList) {
                            tbody {
                                st.include(it: parameterDefinition, page: parameterDefinition.descriptor.valuePage)
                            }
                        }
                    }
                } else {
                    text("${project.fullDisplayName} doesn't have release plugin configuration")
                }
            } else {
                text("${project.fullDisplayName} is of wrong type and can't be released: ${project.class.simpleName}")
            }
        } else {
            text("no such job ${jobName}")
        }
    } else {
        text('no job specified')
    }
}
