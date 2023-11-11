/* Copyright 2022, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nextflow.dotenv

import groovyx.gpars.dataflow.DataflowBroadcast
import nextflow.Session
import nextflow.executor.Executor
import nextflow.executor.ExecutorFactory
import nextflow.processor.TaskHandler
import nextflow.processor.TaskMonitor
import nextflow.processor.TaskRun
import nextflow.processor.TaskStatus
import nextflow.script.ChannelOut
import nextflow.script.ScriptRunner
import nextflow.script.ScriptType

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** A class for mock running of a Nextflow main script. */
class MockScriptRunner extends ScriptRunner {

    // TODO: Use faster in-memory testing when upstream supports it: https://github.com/cdimascio/dotenv-java/issues/59
    // /** An in-memory filesystem for unit testing. */
    // static private FileSystem fs = Jimfs.newFileSystem(Configuration.unix())
    //
    // private Path tmpDir = {
    //     Path tmp = fs.getPath('/tmp')
    //     tmp.mkdir()
    //     Path test = Files.createTempDirectory(tmp, 'test')
    //     test
    // }

    /** Return a random temporary directory for testing. */
    private Path tmpDir = {
        Path tmp = Path.of(System.getProperty('java.io.tmpdir'))
        tmp.mkdir()
        Path test = Files.createTempDirectory(tmp, 'test')
        test
    }()

    /** Create a temporary file in the in-memory filesystem for unit tests.
      *
      * @pararm name The name of the temporary file.
      * @param content The content of the temporary file, if any.
      * @param relative The relative modifier to the temporary file e.g. 'child-folder/'
      */
    private Path createInMemTempFile(String name, String content=null, String relative=null) {
        Path result
        if (relative) {
            this.tmpDir.resolve(relative).mkdir()
            result = this.tmpDir.resolve(relative).resolve(name)
        } else {
            result = this.tmpDir.resolve(name)
        }
        if (content) {
            result.text = content
        }
        result
    }

    /** Instantiate a new mock script runner from a config dictionary. */
    MockScriptRunner(Map config) {

        super(new MockSession(config))
    }

    /** Set the script `main.nf` with specific contents. */
    MockScriptRunner setScript(String content) {
        Path script = createInMemTempFile('main.nf', content)
        setScript(script)
        return this
    }

    /** Set the configuration file `.env` with specific contents. */
    MockScriptRunner setDotenv(String content, String filename=DotenvExtension.DEFAULT_FILENAME, String relative=null) {
        createInMemTempFile(filename, content, relative)
        return this
    }

    /** Normalize the output of the script result so it is easier to compare. */
    @Override
    def normalizeOutput(output) {
        if (output instanceof ChannelOut) {
            def list = new ArrayList(output.size())
            for (int i=0; i<output.size(); i++) {
                list.add(read0(output[i]))
            }
            return list.size() == 1 ? list[0] : list
        }

        if (output instanceof Object[] || output instanceof List) {
            def result = new ArrayList<>(output.size())
            for (def item : output) {
                ((List)result).add(read0(item))
            }
            return result
        }

        else {
            return read0(output)
        }
    }

    /** Read the dataflow broadcast channel. */
    private static read0(obj) {
        if (obj instanceof DataflowBroadcast) {
            return obj.createReadChannel()
        }
        return obj
    }

}

/** A mock Nextflow session. */
class MockSession extends Session {

    @Override
    Session start() {
        this.executorFactory = new MockExecutorFactory()
        return super.start()
    }

    MockSession(Map config) {
        super(config)
    }
}

/** A mock Nextflow executor factory. */
class MockExecutorFactory extends ExecutorFactory {

    /** The class of executor this factory builds. */
    @Override
    protected Class<? extends Executor> getExecutorClass(String executorName) {
        return MockExecutor
    }

    /** This is a mock factory so of course we support everything! We're a mock! */
    @Override
    protected boolean isTypeSupported(ScriptType type, Object executor) {
        true
    }
}

/** A mock Nextflow executor. */
class MockExecutor extends Executor {

    /** Signal the executor is finished. */
    @Override
    void signal() { }

    /** Create a mock task monitor. */
    protected TaskMonitor createTaskMonitor() {
        new MockMonitor()
    }

    /** Create a mock task handler. */
    @Override
    TaskHandler createTaskHandler(TaskRun task) {
        return new  MockTaskHandler(task)
    }
}

/** A mock Nextflow monitor. */
class MockMonitor implements TaskMonitor {

    /** Schedule a task handler. */
    void schedule(TaskHandler handler) {
        handler.submit()
    }

    /** Remove a task handler. */
    boolean evict(TaskHandler handler) { void }

    /** Start this task monitor. */
    TaskMonitor start() { void as TaskMonitor }

    /** Signal the monitor is finished. */
    void signal() { }
}

/** A mock Nextflow task handler. */
class MockTaskHandler extends TaskHandler {

    /** Build a mock Nextflow task handler from a task. */
    protected MockTaskHandler(TaskRun task) {
        super(task)
    }

    /** Submit this task. */
    @Override
    void submit() {
        log.info ">> launching mock task: ${task}"
        if (task.type == ScriptType.SCRIPTLET) {
            task.workDir = Paths.get('.').complete()
            task.stdout = task.script
            task.exitStatus = 0
        }
        else {
            task.code.call()
        }
        status = TaskStatus.COMPLETED
        task.processor.finalizeTask(task)
    }

    /** Check if this task is still running but since we are mock, it's totally finished. */
    @Override
    boolean checkIfRunning() {
        false
    }

    /** Check if this task is completed but since we are mock, it's totally completed. */
    @Override
    boolean checkIfCompleted() {
        true
    }

    /** Kill the thing that never existed, yeah that's right, kill the void. */
    @Override
    void kill() { void }
}
