package nextflow.dotenv

import io.github.cdimascio.dotenv.DotenvException
import nextflow.Channel
import nextflow.plugin.Plugins
import nextflow.plugin.TestPluginDescriptorFinder
import nextflow.plugin.TestPluginManager
import nextflow.plugin.extension.PluginExtensionProvider
import org.pf4j.PluginDescriptorFinder
import spock.lang.Shared
import test.Dsl2Spec

import java.nio.file.Path

/** Unit tests for the nf-dotenv plugin that use virtual file systems and mocking to run. */
class DotenvTest extends Dsl2Spec{

    /** Share the plugin mode across all features in this specification. */
    @Shared String pluginsMode

    /** Setup the test class by loading all plugins. */
    def setup() {
        PluginExtensionProvider.reset()
        pluginsMode = System.getProperty('pf4j.mode')
        System.setProperty('pf4j.mode', 'dev')
        Path root = Path.of('.').toAbsolutePath().normalize()
        def manager = new TestPluginManager(root) {
            @Override
            protected PluginDescriptorFinder createPluginDescriptorFinder() {
                return new TestPluginDescriptorFinder() {
                    @Override
                    protected Path getManifestPath(Path pluginPath) {
                        return pluginPath.resolve('build/resources/main/META-INF/MANIFEST.MF')
                    }
                }
            }
        }
        Plugins.init(root, 'dev', manager)
    }

    /** Cleanup the test class by unloading and resetting all plugins. */
    def cleanup() {
        Plugins.stop()
        PluginExtensionProvider.reset()
        pluginsMode ? System.setProperty('pf4j.mode',pluginsMode) : System.clearProperty('pf4j.mode')
    }

    def 'should have the plugin installed but not imported and raise no exception if a dotenv is not found' () {
        when:
            String SCRIPT= '''
                channel.of('hi-mom')
            '''
        and:
            def result = new MockScriptRunner([:]).setScript(SCRIPT).execute()
        then:
            result.val == 'hi-mom'
            result.val == Channel.STOP
    }

    def 'should import the plugin and not raise an exception if a dotenv is not found but unused' () {
        when:
            String SCRIPT = '''
                include { dotenv } from 'plugin/nf-dotenv'
                channel.of('hi-mom')
            '''
        and:
            def result = new MockScriptRunner([:]).setScript(SCRIPT).execute()
        then:
            result.val == 'hi-mom'
            result.val == Channel.STOP
    }

    def 'should import the plugin and raise no exceptions when the dotenv is found' () {
        when:
            String SCRIPT = '''
                include { dotenv } from 'plugin/nf-dotenv'
                channel.of('hi-mom')
            '''
            String DOTENV = '''
                FOO=bar
            '''
        and:
            def result = new MockScriptRunner([:]).setScript(SCRIPT).setDotenv(DOTENV).execute()
        then:
            result.val == 'hi-mom'
            result.val == Channel.STOP
    }

    def 'should import the plugin and by default throw an exception for a key that does not exist' () {
        when:
            String SCRIPT = '''
                include { dotenv } from 'plugin/nf-dotenv'
                channel.of(dotenv('BAZ'))
            '''
            String DOTENV = '''
                FOO=bar
            '''
        and:
            new MockScriptRunner([:]).setScript(SCRIPT).setDotenv(DOTENV).execute()
        then:
            thrown DotenvException
    }

    def 'should import the plugin and return the correct value for a key that does exist' () {
        when:
            String SCRIPT = '''
                include { dotenv } from 'plugin/nf-dotenv'
                channel.of(dotenv('FOO'))
            '''
            String DOTENV = '''
                FOO=bar
            '''
        and:
            def result = new MockScriptRunner([:]).setScript(SCRIPT).setDotenv(DOTENV).execute()
        then:
            result.val == 'bar'
            result.val == Channel.STOP
    }


    def 'should import the plugin and allow for an override of the dotenv filename' () {
        when:
            String SCRIPT = '''
                include { dotenv } from 'plugin/nf-dotenv'
                channel.of(dotenv('FOO'))
            '''
            String DOTENV = '''
                FOO=bar
            '''
            new MockScriptRunner(['dotenv': ['filename': '.envrc']])
                .setScript(SCRIPT)
                .setDotenv(DOTENV,'.env')
                .execute()
        then:
            thrown DotenvException
    }


    def 'should import the plugin and allow for an override of the dotenv directory as a child directory to the main script' () {
        when:
            String SCRIPT = '''
                include { dotenv } from 'plugin/nf-dotenv'
                channel.of(dotenv('FOO'))
            '''
            String DOTENV = '''
                FOO=bar
            '''
        and:
            def result = new MockScriptRunner(['dotenv': ['relative': 'test']])
                .setScript(SCRIPT)
                .setDotenv(DOTENV, DotenvExtension.DEFAULT_FILENAME, 'test')
                .execute()
        then:
            result.val == 'bar'
            result.val == Channel.STOP
    }

    def 'should import the plugin and allow for an override of the dotenv directory as the same as the env file' () {
        when:
            String SCRIPT = '''
                include { dotenv } from 'plugin/nf-dotenv'
                channel.of(dotenv('FOO'))
            '''
            String DOTENV = '''
                FOO=bar
            '''
        and:
            def result = new MockScriptRunner(['dotenv': ['relative': '.']])
                .setScript(SCRIPT)
                .setDotenv(DOTENV, DotenvExtension.DEFAULT_FILENAME, '.')
                .execute()
        then:
            result.val == 'bar'
            result.val == Channel.STOP
    }

    def 'should import the plugin and raise an exception if the override dotenv directory is incorrect' () {
        when:
            String SCRIPT = '''
                include { dotenv } from 'plugin/nf-dotenv'
                channel.of(dotenv('FOO'))
            '''
            String DOTENV = '''
                FOO=bar
            '''
        and:
            new MockScriptRunner(['dotenv': ['relative': 'other/']])
                .setScript(SCRIPT)
                .setDotenv(DOTENV, DotenvExtension.DEFAULT_FILENAME)
                .execute()
        then:
            thrown DotenvException
    }

    def 'should allow for duplicate variables in the dotenv file, preferring the last one defined' () {
        when:
            String SCRIPT = '''
                include { dotenv } from 'plugin/nf-dotenv'
                channel.of(dotenv('FOO'))
            '''
            String DOTENV = '''
                FOO=bar1
                FOO=bar2
            '''
        and:
            def result = new MockScriptRunner([:]).setScript(SCRIPT).setDotenv(DOTENV).execute()
        then:
            result.val == 'bar2'
            result.val == Channel.STOP
    }
}
