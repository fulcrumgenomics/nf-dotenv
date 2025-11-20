package com.fulcrumgenomics.nextflow.plugin

import io.github.cdimascio.dotenv.DotenvException
import nextflow.Channel
import nextflow.plugin.Plugins
import nextflow.plugin.TestPluginDescriptorFinder
import nextflow.plugin.TestPluginManager
import nextflow.plugin.extension.PluginExtensionProvider
import org.pf4j.PluginDescriptorFinder
import spock.lang.Shared
import test.Dsl2Spec

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Manifest

/** Unit tests for the nf-dotenv plugin that use virtual file systems and mocking to run. */
class DotenvTest extends Dsl2Spec {

    /** The plugin mode for all running plugins during test time. */
    @Shared String pluginsMode

    /** The root directory for the plugin. */
    Path root = Path.of('.').toAbsolutePath().normalize()
    Path getRoot() { this.root }
    String getRootString() { this.root.toString() }

    /** Setup the plugin manager and load the dotenv plugin. */
    def setup() {
        PluginExtensionProvider.reset()

        pluginsMode = System.getProperty('pf4j.mode')
        System.setProperty('pf4j.mode', 'dev')

        def root = this.getRoot()

        def manager = new TestPluginManager(root) {
            @Override
            protected PluginDescriptorFinder createPluginDescriptorFinder() {
                return new TestPluginDescriptorFinder() {
                    @Override
                    protected Manifest readManifestFromDirectory(Path pluginPath) {
                        def manifestPath = getManifestPath(pluginPath)
                        final input = Files.newInputStream(manifestPath)
                        return new Manifest(input)
                    }
                    protected Path getManifestPath(Path pluginPath) {
                        return pluginPath.resolve('build/tmp/jar/MANIFEST.MF')
                    }
                }
            }
        }

        Plugins.init(root, 'dev', manager)
        manager.loadPlugins()
        manager.startPlugins()
    }

    /** Cleanup after tests have run. */
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
            def result = new DotenvMockScriptRunner([:]).setScript(SCRIPT).execute()
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
            def result = new DotenvMockScriptRunner([:]).setScript(SCRIPT).execute()
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
            def result = new DotenvMockScriptRunner([:]).setScript(SCRIPT).setDotenv(DOTENV).execute()
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
            new DotenvMockScriptRunner([:]).setScript(SCRIPT).setDotenv(DOTENV).execute()
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
            def result = new DotenvMockScriptRunner([:]).setScript(SCRIPT).setDotenv(DOTENV).execute()
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
            new DotenvMockScriptRunner(['dotenv': ['filename': '.envrc']])
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
            def result = new DotenvMockScriptRunner(['dotenv': ['relative': 'test']])
                .setScript(SCRIPT)
                .setDotenv(DOTENV, com.fulcrumgenomics.nextflow.plugin.DotenvConfig.DEFAULT_FILENAME, 'test')
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
            def result = new DotenvMockScriptRunner(['dotenv': ['relative': '.']])
                .setScript(SCRIPT)
                .setDotenv(DOTENV, com.fulcrumgenomics.nextflow.plugin.DotenvConfig.DEFAULT_FILENAME, '.')
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
            new DotenvMockScriptRunner(['dotenv': ['relative': 'other/']])
                .setScript(SCRIPT)
                .setDotenv(DOTENV, com.fulcrumgenomics.nextflow.plugin.DotenvConfig.DEFAULT_FILENAME)
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
            def result = new DotenvMockScriptRunner([:]).setScript(SCRIPT).setDotenv(DOTENV).execute()
        then:
            result.val == 'bar2'
            result.val == Channel.STOP
    }
}
