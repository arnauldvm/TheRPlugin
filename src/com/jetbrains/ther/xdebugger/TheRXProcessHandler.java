package com.jetbrains.ther.xdebugger;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.process.RunnerWinProcess;
import com.intellij.execution.process.UnixProcessManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import com.jetbrains.ther.debugger.exception.TheRDebuggerException;
import com.jetbrains.ther.debugger.executor.TheRExecutionResult;
import com.jetbrains.ther.debugger.executor.TheRExecutionResultCalculator;
import com.jetbrains.ther.debugger.executor.TheRExecutionResultType;
import com.jetbrains.ther.debugger.executor.TheRExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jvnet.winp.WinProcess;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.List;
import java.util.concurrent.Future;

import static com.jetbrains.ther.debugger.data.TheRDebugConstants.LINE_SEPARATOR;

class TheRXProcessHandler extends ColoredProcessHandler implements TheRExecutor {

  @NotNull
  private static final Logger LOGGER = Logger.getInstance(TheRXProcessHandler.class);

  @NotNull
  private static final Key SERVICE_KEY = ProcessOutputTypes.STDERR;

  @NotNull
  private final List<String> myInitCommands;

  @NotNull
  private final TheRExecutionResultCalculator myResultCalculator;

  private final boolean myPrintIn;
  private final boolean myPrintOut;
  private final boolean myPrintErr;

  @NotNull
  private final StringBuilder myOutputBuffer;

  @NotNull
  private final StringBuilder myErrorBuffer;

  @NotNull
  private final OutputStreamWriter myWriter;

  @Nullable
  private Reader myOutputReader;

  @Nullable
  private Reader myErrorReader;

  private int myExecuteCounter;

  public TheRXProcessHandler(@NotNull final GeneralCommandLine commandLine,
                             @NotNull final List<String> initCommands,
                             @NotNull final TheRExecutionResultCalculator resultCalculator,
                             final boolean printIn,
                             final boolean printOut,
                             final boolean printErr)
    throws ExecutionException {
    super(commandLine);

    myInitCommands = initCommands;
    myResultCalculator = resultCalculator;

    myPrintIn = printIn;
    myPrintOut = printOut;
    myPrintErr = printErr;

    myOutputBuffer = new StringBuilder();
    myErrorBuffer = new StringBuilder();

    myWriter = new OutputStreamWriter(getProcess().getOutputStream());

    myOutputReader = null;
    myErrorReader = null;
    myExecuteCounter = 0;
  }

  @NotNull
  @Override
  public TheRExecutionResult execute(@NotNull final String command) throws TheRDebuggerException {
    try {
      myWriter.write(command);
      myWriter.write(LINE_SEPARATOR);
      myWriter.flush();

      synchronized (myOutputBuffer) {
        waitForOutput();

        synchronized (myErrorBuffer) {
          waitForError();

          final TheRExecutionResult result = myResultCalculator.calculate(myOutputBuffer, myErrorBuffer.toString());

          myExecuteCounter++;

          printInputAndOutput(command, result);

          myOutputBuffer.setLength(0);
          myErrorBuffer.setLength(0);

          return result;
        }
      }
    }
    catch (final IOException e) {
      throw new TheRDebuggerException(e);
    }
    catch (final InterruptedException e) {
      throw new TheRDebuggerException(e);
    }
  }

  public void start() throws TheRDebuggerException {
    super.startNotify();

    for (final String initCommand : myInitCommands) {
      execute(initCommand);
    }
  }

  @NotNull
  @Override
  protected BaseDataReader createOutputDataReader(@NotNull final BaseDataReader.SleepingPolicy sleepingPolicy) {
    myOutputReader = super.createProcessOutReader();

    return new TheRXBaseOutputReader(myOutputReader, sleepingPolicy, myOutputBuffer);
  }

  @NotNull
  @Override
  protected BaseDataReader createErrorDataReader(@NotNull final BaseDataReader.SleepingPolicy sleepingPolicy) {
    myErrorReader = super.createProcessErrReader();

    return new TheRXBaseOutputReader(myErrorReader, sleepingPolicy, myErrorBuffer);
  }

  @Override
  protected void doDestroyProcess() {
    // reworked version of com.intellij.execution.process.impl.OSProcessManagerImpl#killProcessTree

    if (SystemInfo.isUnix) {
      UnixProcessManager.sendSignalToProcessTree(getProcess(), UnixProcessManager.SIGTERM);
    }
    else if (SystemInfo.isWindows) {
      convertToWinProcess(getProcess()).killRecursively(); // TODO [xdbg][check]
    }
    else {
      LOGGER.warn("Unexpected OS. Process will be destroyed using Java API");

      getProcess().destroy();
    }
  }

  private void waitForOutput() throws IOException, InterruptedException {
    assert myOutputReader != null;

    synchronized (myOutputBuffer) {
      while (myOutputReader.ready() || !myResultCalculator.isComplete(myOutputBuffer)) {
        myOutputBuffer.wait();
      }
    }
  }

  private void waitForError() throws IOException, InterruptedException {
    assert myErrorReader != null;

    synchronized (myErrorBuffer) {
      while (myErrorReader.ready()) {
        myErrorBuffer.wait();
      }
    }
  }

  private void printInputAndOutput(@NotNull final String command, @NotNull final TheRExecutionResult result) {
    if (myPrintIn) {
      printInputOrOutput("COMMAND", command);
    }

    if (myPrintOut) {
      printInputOrOutput("TYPE", result.getType().toString());
      printInputOrOutput("OUTPUT", result.getOutput());

      if (result.getType() != TheRExecutionResultType.RESPONSE && !result.getResultRange().isEmpty()) {
        printInputOrOutput("RESULT", result.getResultRange().substring(result.getOutput()));
      }
    }

    if (myPrintErr && !result.getError().isEmpty()) {
      printInputOrOutput("ERROR", result.getError());
    }
  }

  @NotNull
  private WinProcess convertToWinProcess(@NotNull final Process process) {
    // copied from com.intellij.execution.process.impl.OSProcessManagerImpl#createWinProcess

    if (process instanceof RunnerWinProcess) {
      return new WinProcess(((RunnerWinProcess)process).getOriginalProcess());
    }
    else {
      return new WinProcess(process);
    }
  }

  private void printInputOrOutput(@NotNull final String title, @NotNull final String message) {
    notifyTextAvailable(title, SERVICE_KEY);
    notifyTextAvailable(" #", SERVICE_KEY);
    notifyTextAvailable(Integer.toString(myExecuteCounter), SERVICE_KEY);
    notifyTextAvailable(":\n", SERVICE_KEY);
    notifyTextAvailable(message, SERVICE_KEY);
    notifyTextAvailable("\n\n", SERVICE_KEY);
  }

  private class TheRXBaseOutputReader extends BaseOutputReader {

    @NotNull
    private final StringBuilder myBuffer;

    public TheRXBaseOutputReader(@NotNull final Reader reader,
                                 @NotNull final SleepingPolicy sleepingPolicy,
                                 @NotNull final StringBuilder buffer) {
      super(reader, sleepingPolicy);

      myBuffer = buffer;

      start();
    }

    @Override
    protected void onTextAvailable(@NotNull final String text) {
      synchronized (myBuffer) {
        myBuffer.append(text);
        myBuffer.notify();
      }
    }

    @Override
    protected Future<?> executeOnPooledThread(@NotNull final Runnable runnable) {
      return TheRXProcessHandler.this.executeOnPooledThread(runnable);
    }
  }
}
