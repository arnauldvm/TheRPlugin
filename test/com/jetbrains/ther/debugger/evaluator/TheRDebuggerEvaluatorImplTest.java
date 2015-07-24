package com.jetbrains.ther.debugger.evaluator;

import com.intellij.openapi.util.TextRange;
import com.jetbrains.ther.debugger.TheRScriptReader;
import com.jetbrains.ther.debugger.data.TheRLocation;
import com.jetbrains.ther.debugger.data.TheRProcessResponse;
import com.jetbrains.ther.debugger.data.TheRProcessResponseType;
import com.jetbrains.ther.debugger.data.TheRVar;
import com.jetbrains.ther.debugger.exception.TheRDebuggerException;
import com.jetbrains.ther.debugger.function.TheRFunctionDebugger;
import com.jetbrains.ther.debugger.function.TheRFunctionDebuggerFactory;
import com.jetbrains.ther.debugger.function.TheRFunctionDebuggerHandler;
import com.jetbrains.ther.debugger.interpreter.TheRLoadableVarHandler;
import com.jetbrains.ther.debugger.interpreter.TheRProcess;
import com.jetbrains.ther.debugger.mock.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TheRDebuggerEvaluatorImplTest {

  @Test
  public void evalTrueCondition() {
    final String text = "[1] TRUE";

    final AlwaysSameResponseTheRProcess process = new AlwaysSameResponseTheRProcess(
      text,
      TheRProcessResponseType.RESPONSE,
      TextRange.allOf(text),
      ""
    );

    final TheRDebuggerEvaluatorImpl evaluator = new TheRDebuggerEvaluatorImpl(
      process,
      new IllegalTheRFunctionDebuggerFactory(),
      new IllegalTheRFunctionDebuggerHandler(),
      new IllegalTheRLoadableVarHandler()
    );

    final MockConditionReceiver receiver = new MockConditionReceiver(true);

    evaluator.evalCondition("5 > 4", receiver);

    assertEquals(1, process.getExecuteCalled());
    assertEquals(1, receiver.getResultReceived());
  }

  @Test
  public void evalFalseCondition() {
    final String text = "[1] FALSE";

    final AlwaysSameResponseTheRProcess process = new AlwaysSameResponseTheRProcess(
      text,
      TheRProcessResponseType.RESPONSE,
      TextRange.allOf(text),
      ""
    );

    final TheRDebuggerEvaluatorImpl evaluator = new TheRDebuggerEvaluatorImpl(
      process,
      new IllegalTheRFunctionDebuggerFactory(),
      new IllegalTheRFunctionDebuggerHandler(),
      new IllegalTheRLoadableVarHandler()
    );

    final MockConditionReceiver receiver = new MockConditionReceiver(false);

    evaluator.evalCondition("5 < 4", receiver);

    assertEquals(1, process.getExecuteCalled());
    assertEquals(1, receiver.getResultReceived());
  }

  @Test
  public void evalInvalidResponseFormatCondition() {
    final String text = "ghi";

    final AlwaysSameResponseTheRProcess process = new AlwaysSameResponseTheRProcess(
      text,
      TheRProcessResponseType.RESPONSE,
      TextRange.allOf(text),
      ""
    );

    final TheRDebuggerEvaluatorImpl evaluator = new TheRDebuggerEvaluatorImpl(
      process,
      new IllegalTheRFunctionDebuggerFactory(),
      new IllegalTheRFunctionDebuggerHandler(),
      new IllegalTheRLoadableVarHandler()
    );

    final MockConditionReceiver receiver = new MockConditionReceiver(false);

    evaluator.evalCondition("def", receiver);

    assertEquals(1, process.getExecuteCalled());
    assertEquals(1, receiver.getResultReceived());
  }

  @Test
  public void evalUnexpectedResponseType() {
    final AlwaysSameResponseTheRProcess process = new AlwaysSameResponseTheRProcess(
      "",
      TheRProcessResponseType.PLUS,
      TextRange.EMPTY_RANGE,
      ""
    );

    final TheRDebuggerEvaluatorImpl evaluator = new TheRDebuggerEvaluatorImpl(
      process,
      new IllegalTheRFunctionDebuggerFactory(),
      new IllegalTheRFunctionDebuggerHandler(),
      new IllegalTheRLoadableVarHandler()
    );

    final ErrorExpressionReceiver receiver = new ErrorExpressionReceiver();

    evaluator.evalExpression("def <- function() {", receiver);

    assertEquals(1, process.getExecuteCalled());
    assertEquals(1, receiver.getErrorReceived());
  }

  @Test
  public void evalExceptionDuringExecution() {
    final ExceptionDuringExecutionTheRProcess process = new ExceptionDuringExecutionTheRProcess();

    final TheRDebuggerEvaluatorImpl evaluator = new TheRDebuggerEvaluatorImpl(
      process,
      new IllegalTheRFunctionDebuggerFactory(),
      new IllegalTheRFunctionDebuggerHandler(),
      new IllegalTheRLoadableVarHandler()
    );

    final ErrorExpressionReceiver receiver = new ErrorExpressionReceiver();

    evaluator.evalExpression("def", receiver);

    assertEquals(1, process.getExecuteCalled());
    assertEquals(1, receiver.getErrorReceived());
  }

  @Test
  public void evalFunction() {
    final String text = "[1] 1 2 3";

    final AlwaysSameResponseTheRProcess process = new AlwaysSameResponseTheRProcess(
      text,
      TheRProcessResponseType.RESPONSE,
      TextRange.allOf(text),
      ""
    );

    final TheRDebuggerEvaluatorImpl evaluator = new TheRDebuggerEvaluatorImpl(
      process,
      new IllegalTheRFunctionDebuggerFactory(),
      new IllegalTheRFunctionDebuggerHandler(),
      new IllegalTheRLoadableVarHandler()
    );

    final MockExpressionReceiver receiver = new MockExpressionReceiver(text);

    evaluator.evalExpression("def(c(1:5))", receiver);

    assertEquals(1, process.getExecuteCalled());
    assertEquals(1, receiver.getResultReceived());
  }

  @Test
  public void evalDebuggedFunction() {
    final String command = "def(c(1:5))";

    final AlwaysSameResponseTheRProcess process = new AlwaysSameResponseTheRProcess(
      "debugging in: " + command,
      TheRProcessResponseType.DEBUGGING_IN,
      TextRange.EMPTY_RANGE,
      ""
    );

    final DebuggedTheRFunctionDebuggerFactory debuggerFactory = new DebuggedTheRFunctionDebuggerFactory();

    final TheRDebuggerEvaluatorImpl evaluator = new TheRDebuggerEvaluatorImpl(
      process,
      debuggerFactory,
      new IllegalTheRFunctionDebuggerHandler(),
      new IllegalTheRLoadableVarHandler()
    );

    final MockExpressionReceiver receiver = new MockExpressionReceiver("[1] 1 2 3");

    evaluator.evalExpression(command, receiver);

    assertEquals(1, process.getExecuteCalled());
    assertEquals(1, debuggerFactory.getNotMainCalled());
    assertEquals(1, debuggerFactory.getAdvanceCalled());
    assertEquals(1, receiver.getResultReceived());
  }

  private static class ExceptionDuringExecutionTheRProcess implements TheRProcess {

    private int myExecuteCalled = 0;

    @NotNull
    @Override
    public TheRProcessResponse execute(@NotNull final String command) throws TheRDebuggerException {
      myExecuteCalled++;

      throw new TheRDebuggerException("");
    }

    @Override
    public void stop() {
    }

    public int getExecuteCalled() {
      return myExecuteCalled;
    }
  }

  private static class DebuggedTheRFunctionDebuggerFactory implements TheRFunctionDebuggerFactory {

    private final int[] myAdvanceCalled = new int[]{0};
    private int myNotMainCalled = 0;

    @NotNull
    @Override
    public TheRFunctionDebugger getNotMainFunctionDebugger(@NotNull final TheRProcess process,
                                                           @NotNull final TheRFunctionDebuggerFactory debuggerFactory,
                                                           @NotNull final TheRFunctionDebuggerHandler debuggerHandler,
                                                           @NotNull final TheRLoadableVarHandler varHandler)
      throws TheRDebuggerException {
      myNotMainCalled++;

      return new TheRFunctionDebugger() {

        @NotNull
        @Override
        public TheRLocation getLocation() {
          throw new IllegalStateException("GetLocation shouldn't be called");
        }

        @NotNull
        @Override
        public List<TheRVar> getVars() {
          throw new IllegalStateException("GetVars shouldn't be called");
        }

        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public void advance() throws TheRDebuggerException {
          myAdvanceCalled[0]++;

          if (myAdvanceCalled[0] > 1) {
            throw new IllegalStateException("Advance shouldn't be called");
          }
        }

        @NotNull
        @Override
        public String getResult() {
          return "[1] 1 2 3";
        }
      };
    }

    @NotNull
    @Override
    public TheRFunctionDebugger getMainFunctionDebugger(@NotNull final TheRProcess process,
                                                        @NotNull final TheRFunctionDebuggerFactory debuggerFactory,
                                                        @NotNull final TheRFunctionDebuggerHandler debuggerHandler,
                                                        @NotNull final TheRLoadableVarHandler varHandler,
                                                        @NotNull final TheRScriptReader scriptReader) {
      throw new IllegalStateException("GetMainFunctionDebugger shouldn't be called");
    }

    public int getNotMainCalled() {
      return myNotMainCalled;
    }

    public int getAdvanceCalled() {
      return myAdvanceCalled[0];
    }
  }
}