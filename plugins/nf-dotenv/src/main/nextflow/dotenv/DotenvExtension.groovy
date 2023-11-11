package nextflow.dotenv

import groovy.transform.CompileStatic
import io.github.cdimascio.dotenv.Dotenv
import io.github.cdimascio.dotenv.DotenvBuilder
import io.github.cdimascio.dotenv.DotenvException
import nextflow.Session
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint

import java.nio.file.Path

/** An extension to make dotenv file variables available to Nextflow scripts through a function call. */
@CompileStatic
class DotenvExtension extends PluginExtensionPoint {

    /** The default filename for the dotenv file. */
    static final String DEFAULT_FILENAME = '.env'

    /** The configuration of this Nextflow session. */
    private Map config

    /** The directory where the dotenv file is supposed to be located. */
    private Path directory

    /** The filename of the dotenv file used in this session. */
    private String filename

    /** Initializes the plugin once it is loaded and the session is ready. */
    @Override
    protected void init(Session session) {
        this.config = session.config.navigate('dotenv', [:]) as Map
        this.directory = session.baseDir.resolve(this.config.get('relative', '.'))
        this.filename = config.get('filename', DEFAULT_FILENAME).toString()
    }

    /** The dotenv environment for this Nextflow session. Marked as lazy to only raise exceptions at call time. */
    @Lazy private Dotenv dotenv = {
        try {
            new DotenvBuilder()
                .filename(this.filename)
                .directory(this.directory.toString())
                .load()
        } catch (DotenvException) {
            throw new DotenvException(
                "Could not find dotenv file at path ${this.directory}/${this.filename}\n\n" +
                "Consider modifying the following properties in your Nextflow config:\n\n" +
                "\tdotenv.filename = '${DEFAULT_FILENAME}'\n" +
                "\tdotenv.relative = '.'\n\n"
            )
        }
    }()

    /** Return a value in the dotenv environment, or raise an exception if the key is missing. */
    @Function
    String dotenv(String key) {
        def value = this.dotenv.get(key)
        if (value == null) {
            throw new DotenvException("Could not find key ${key} in dotenv ${this.directory}/${this.filename}")
        } else {
            return value
        }
    }
}
