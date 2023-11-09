package nextflow.dotenv

import groovy.transform.CompileStatic
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.DotenvBuilder
import nextflow.Session
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint

/** An extension to make dotenv file variables available to Nextflow scripts through a function call. */
@CompileStatic
class DotenvExtension extends PluginExtensionPoint {

    /** The default filename for the dotenv file. */
    static final String DEFAULT_FILENAME = ".env"

    /** The configuration of this Nextflow session. */
    private Map config

    /** The dotenv environment result for this Nextflow session. */
    private Dotenv dotenv

    /** The default value to return when none is found in the environmental configuration file. */
    private String defaultValue

    /** Initializes the plugin once it is loaded and the session is ready. */
    @Override
    protected void init(Session session) {
        this.config = session.config.navigate('dotenv', [:]) as Map
        this.defaultValue = config.get("default", "").toString()
        this.dotenv = new DotenvBuilder()
            // The filename of the dotenv file which is typically '.env' but could be '.envrc' or other
            .filename(config.get("filename", DEFAULT_FILENAME).toString())
            // The relative directory to the main Nextflow script.
            .directory(config.get("relative", session.baseDir.toUriString()).toString())
            .load()
    }

    /** Return a value in the dotenv environment, or an empty string if the key is missing. */
    @Function
    String dotenv(String key) {
        this.dotenv.get(key, this.defaultValue).toString()
    }
}
