package com.fulcrumgenomics.nextflow.plugin

import test.MockScriptRunner
import test.MockSession
import test.TestHelper

import java.nio.file.Files
import java.nio.file.Path

/** A custom MockSession that overrides baseDir to point to a real filesystem directory. */
class DotenvMockSession extends MockSession {
    private Path realBaseDir

    DotenvMockSession(Map config) {
        super(config)
    }

    void setRealBaseDir(Path path) {
        this.realBaseDir = path
    }

    @Override
    Path getBaseDir() {
        return realBaseDir ?: super.getBaseDir()
    }
}

/** A class for mock running of a Nextflow main script with dotenv support. */
class DotenvMockScriptRunner extends MockScriptRunner {

    private Path scriptDir
    private Path realTempDir  // A real filesystem directory for dotenv files

    DotenvMockScriptRunner() {
        super(new DotenvMockSession([:]))
    }

    DotenvMockScriptRunner(Map config) {
        super(new DotenvMockSession(config))
    }

    DotenvMockScriptRunner(MockSession session) {
        super(session instanceof DotenvMockSession ? session : new DotenvMockSession([:]))
    }

    @Override
    DotenvMockScriptRunner setScript(String str) {
        def scriptPath = TestHelper.createInMemTempFile('main.nf', str)
        this.scriptDir = scriptPath.parent
        setScript(scriptPath)
        return this
    }

    /** Set the configuration file `.env` with specific contents.
      *
      * @param content The content of the .env file
      * @param filename The name of the dotenv file (default: .env)
      * @param relative The relative directory path where the file should be created (e.g., 'test', '.')
      * @return this instance for method chaining
      */
    DotenvMockScriptRunner setDotenv(
        String content,
        String filename = com.fulcrumgenomics.nextflow.plugin.DotenvConfig.DEFAULT_FILENAME,
        String relative = null
    ) {
        if (!scriptDir) {
            throw new IllegalStateException("Must call setScript() before setDotenv()")
        }

        if (!realTempDir) {
            this.realTempDir = Files.createTempDirectory('test')
        }

        Path targetDir = realTempDir
        if (relative && relative != '.') {
            targetDir = realTempDir.resolve(relative)
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir)
            }
        }

        Path dotenvFile = targetDir.resolve(filename)
        Files.writeString(dotenvFile, content)

        return this
    }

    @Override
    def execute() {
        if (realTempDir) {
            def session = this.getSession()
            if (session instanceof DotenvMockSession) {
                session.setRealBaseDir(realTempDir)
            }
        }
        return super.execute()
    }
}
