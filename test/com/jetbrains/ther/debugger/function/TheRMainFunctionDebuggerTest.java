package com.jetbrains.ther.debugger.function;

import com.intellij.openapi.util.TextRange;
import com.jetbrains.ther.debugger.TheROutputReceiver;
import com.jetbrains.ther.debugger.TheRScriptReader;
import com.jetbrains.ther.debugger.data.TheRLocation;
import com.jetbrains.ther.debugger.data.TheRScriptLine;
import com.jetbrains.ther.debugger.exception.TheRDebuggerException;
import com.jetbrains.ther.debugger.interpreter.TheRProcess;
import com.jetbrains.ther.debugger.interpreter.TheRProcessResponse;
import com.jetbrains.ther.debugger.interpreter.TheRProcessResponseType;
import com.jetbrains.ther.debugger.mock.IllegalTheRFunctionDebugger;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;

import static com.jetbrains.ther.debugger.data.TheRDebugConstants.*;
import static org.junit.Assert.*;

public class TheRMainFunctionDebuggerTest {

  @Test
  public void ordinary() throws TheRDebuggerException {
    final MockTheRProcess process = new MockTheRProcess();
    final MockTheRFunctionDebuggerFactory factory = new MockTheRFunctionDebuggerFactory();
    final MockTheRFunctionDebuggerHandler handler = new MockTheRFunctionDebuggerHandler();
    final MockTheROutputReceiver receiver = new MockTheROutputReceiver();

    final TheRMainFunctionDebugger debugger = new TheRMainFunctionDebugger(
      process,
      factory,
      handler,
      receiver,
      new MockTheRScriptReader()
    );

    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation(MAIN_FUNCTION_NAME, 0), debugger.getLocation());

    process.reset();
    receiver.reset();
    debugger.advance();

    assertTrue(process.check0());
    assertEquals(0, factory.myCounter);
    assertEquals(0, handler.myCounter);
    assertEquals(0, receiver.myErrorCount);
    assertEquals(0, receiver.myOutputCount);
    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation(MAIN_FUNCTION_NAME, 1), debugger.getLocation());

    process.reset();
    receiver.reset();
    debugger.advance();

    assertTrue(process.check1());
    assertEquals(0, factory.myCounter);
    assertEquals(0, handler.myCounter);
    assertEquals(1, receiver.myErrorCount);
    assertEquals("error1", receiver.myError);
    assertEquals(0, receiver.myOutputCount);
    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation(MAIN_FUNCTION_NAME, 2), debugger.getLocation());

    process.reset();
    receiver.reset();
    debugger.advance();

    assertTrue(process.check2());
    assertEquals(0, factory.myCounter);
    assertEquals(0, handler.myCounter);
    assertEquals(1, receiver.myErrorCount);
    assertEquals("error2", receiver.myError);
    assertEquals(0, receiver.myOutputCount);
    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation(MAIN_FUNCTION_NAME, 7), debugger.getLocation());

    process.reset();
    receiver.reset();
    debugger.advance();

    assertTrue(process.check3());
    assertEquals(0, factory.myCounter);
    assertEquals(0, handler.myCounter);
    assertEquals(1, receiver.myErrorCount);
    assertEquals(1, receiver.myOutputCount);
    assertEquals("error3", receiver.myError);
    assertEquals("character(0)", receiver.myOutput);
    assertTrue(debugger.hasNext());
    assertEquals(new TheRLocation(MAIN_FUNCTION_NAME, 10), debugger.getLocation());

    process.reset();
    receiver.reset();
    debugger.advance();

    assertTrue(process.check4());
    assertEquals(1, factory.myCounter);
    assertEquals(1, handler.myCounter);
    assertEquals(0, receiver.myOutputCount);
    assertEquals(0, receiver.myErrorCount);
    assertFalse(debugger.hasNext());
    assertEquals("", debugger.getResult());
    assertEquals(new TheRLocation(MAIN_FUNCTION_NAME, -1), debugger.getLocation());
  }

  private static class MockTheRProcess implements TheRProcess {

    private int myExecuted = 0;
    private int myTraceAndDebugExecuted = 0;

    private boolean my1Executed = false;

    private boolean my20Executed = false;
    private boolean my21Executed = false;
    private boolean my22Executed = false;
    private boolean my23Executed = false;
    private boolean my24Executed = false;

    private boolean my3Executed = false;

    private boolean my4Executed = false;

    public void reset() {
      myExecuted = 0;
      myTraceAndDebugExecuted = 0;

      my1Executed = false;

      my20Executed = false;
      my21Executed = false;
      my22Executed = false;
      my23Executed = false;
      my24Executed = false;

      my3Executed = false;

      my4Executed = false;
    }

    public boolean check0() {
      return myExecuted == 0;
    }

    public boolean check1() {
      return myExecuted == 2 && my1Executed && myTraceAndDebugExecuted == 1;
    }

    public boolean check2() {
      return myExecuted == 6 &&
             my20Executed &&
             my21Executed &&
             my22Executed &&
             my23Executed &&
             my24Executed &&
             myTraceAndDebugExecuted == 1;
    }

    public boolean check3() {
      return myExecuted == 2 && my3Executed && myTraceAndDebugExecuted == 1;
    }

    public boolean check4() {
      return myExecuted == 1 && my4Executed;
    }

    @NotNull
    @Override
    public TheRProcessResponse execute(@NotNull final String command) throws TheRDebuggerException {
      myExecuted++;

      if (command.equals("x <- c(1:10)")) {
        my1Executed = true;

        return new TheRProcessResponse(
          "",
          TheRProcessResponseType.EMPTY,
          TextRange.EMPTY_RANGE,
          "error1"
        );
      }

      if (command.equals("Filter(function(x) x == \"closure\", eapply(" + ENVIRONMENT + "(), " + TYPEOF_COMMAND + "))")) {
        myTraceAndDebugExecuted++;

        return new TheRProcessResponse(
          "named list()",
          TheRProcessResponseType.RESPONSE,
          TextRange.allOf("named list()"),
          ""
        );
      }

      if (command.equals("f <- function(x) {")) {
        my20Executed = true;

        return new TheRProcessResponse(
          "",
          TheRProcessResponseType.PLUS,
          TextRange.EMPTY_RANGE,
          "error2"
        );
      }

      if (command.equals("# comment in function")) {
        my21Executed = true;

        return new TheRProcessResponse(
          "",
          TheRProcessResponseType.PLUS,
          TextRange.EMPTY_RANGE,
          ""
        );
      }

      if (command.equals(" ")) {
        my22Executed = true;

        return new TheRProcessResponse(
          "",
          TheRProcessResponseType.PLUS,
          TextRange.EMPTY_RANGE,
          ""
        );
      }

      if (command.equals("x + 1")) {
        my23Executed = true;

        return new TheRProcessResponse(
          "",
          TheRProcessResponseType.PLUS,
          TextRange.EMPTY_RANGE,
          ""
        );
      }

      if (command.equals("}")) {
        my24Executed = true;

        return new TheRProcessResponse(
          "",
          TheRProcessResponseType.EMPTY,
          TextRange.EMPTY_RANGE,
          ""
        );
      }

      if (command.equals("ls()")) {
        my3Executed = true;

        return new TheRProcessResponse(
          "character(0)",
          TheRProcessResponseType.RESPONSE,
          TextRange.allOf("character(0)"),
          "error3"
        );
      }

      if (command.equals("f(x)")) {
        my4Executed = true;

        return new TheRProcessResponse(
          "debugging in: f(x)\n" +
          "debug: {\n" +
          "    # comment in function\n" +
          "     \n" +
          "    x + 1\n" +
          "}",
          TheRProcessResponseType.DEBUGGING_IN,
          TextRange.EMPTY_RANGE,
          ""
        );
      }

      throw new IllegalStateException("Unexpected command");
    }

    @Override
    public void stop() {
    }
  }

  private static class MockTheRFunctionDebuggerFactory implements TheRFunctionDebuggerFactory {

    private int myCounter = 0;

    @NotNull
    @Override
    public TheRFunctionDebugger getNotMainFunctionDebugger(@NotNull final TheRProcess process,
                                                           @NotNull final TheRFunctionDebuggerHandler debuggerHandler,
                                                           @NotNull final TheROutputReceiver outputReceiver) throws TheRDebuggerException {
      myCounter++;

      return new IllegalTheRFunctionDebugger();
    }

    @NotNull
    @Override
    public TheRFunctionDebugger getMainFunctionDebugger(@NotNull final TheRProcess process,
                                                        @NotNull final TheRFunctionDebuggerHandler debuggerHandler,
                                                        @NotNull final TheROutputReceiver outputReceiver,
                                                        @NotNull final TheRScriptReader scriptReader) {
      throw new IllegalStateException("GetMainFunctionDebugger shouldn't be called");
    }
  }

  private static class MockTheRFunctionDebuggerHandler implements TheRFunctionDebuggerHandler {

    private int myCounter = 0;

    @Override
    public void appendDebugger(@NotNull final TheRFunctionDebugger debugger) {
      myCounter++;
    }

    @Override
    public void setReturnLineNumber(final int lineNumber) {
      throw new IllegalStateException("SetReturnLineNumber shouldn't be called");
    }

    @Override
    public void setDropFrames(final int number) {
      throw new IllegalStateException("SetDropFrames shouldn't be called");
    }
  }

  private static class MockTheROutputReceiver implements TheROutputReceiver {

    @NotNull
    private String myOutput = "";

    @NotNull
    private String myError = "";

    private int myOutputCount = 0;
    private int myErrorCount = 0;

    @Override
    public void receiveOutput(@NotNull final String output) {
      myOutput = output;
      myOutputCount++;
    }

    @Override
    public void receiveError(@NotNull final String error) {
      myError = error;
      myErrorCount++;
    }

    public void reset() {
      myOutput = "";
      myError = "";

      myOutputCount = 0;
      myErrorCount = 0;
    }
  }

  private static class MockTheRScriptReader implements TheRScriptReader {

    @NotNull
    private final String[] myCommands = new String[]{
      NOP_COMMAND,
      "x <- c(1:10)",
      "f <- function(x) {",
      "# comment in function",
      " ", // empty line in function
      "x + 1",
      "}",
      "ls()",
      "# comment in script",
      "  ", // empty line in script
      "f(x)",
    };

    private int myCurrentNumber = 0;

    @NotNull
    @Override
    public TheRScriptLine getCurrentLine() {
      if (myCurrentNumber > myCommands.length - 1) {
        myCurrentNumber = -1;
      }

      if (myCurrentNumber == -1) {
        return new TheRScriptLine(null, -1);
      }

      return new TheRScriptLine(myCommands[myCurrentNumber], myCurrentNumber);
    }

    @Override
    public void advance() throws IOException {
      if (myCurrentNumber > myCommands.length - 1) {
        myCurrentNumber = -1;
      }

      if (myCurrentNumber == -1) {
        return;
      }

      myCurrentNumber++;
    }

    @Override
    public void close() throws IOException {
    }
  }
}