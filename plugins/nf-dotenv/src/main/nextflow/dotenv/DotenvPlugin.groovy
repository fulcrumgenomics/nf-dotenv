package nextflow.dotenv

import groovy.transform.CompileStatic
import nextflow.plugin.BasePlugin
import org.pf4j.PluginWrapper

/** The nf-dotenv Nextflow plugin entrypoint. */
@CompileStatic
class DotenvPlugin extends BasePlugin {

    DotenvPlugin(PluginWrapper wrapper) {
        super(wrapper)
    }
}
