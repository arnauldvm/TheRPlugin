package com.jetbrains.ther.debugger.evaluator;

import com.jetbrains.ther.debugger.TheRForcedFunctionDebuggerHandler;
import com.jetbrains.ther.debugger.TheROutputReceiver;
import com.jetbrains.ther.debugger.exception.TheRDebuggerException;
import com.jetbrains.ther.debugger.exception.TheRUnexpectedExecutionResultException;
import com.jetbrains.ther.debugger.executor.TheRExecutionResult;
import com.jetbrains.ther.debugger.executor.TheRExecutor;
import com.jetbrains.ther.debugger.function.TheRFunctionDebuggerFactory;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.ther.debugger.TheRDebuggerStringUtils.appendError;
import static com.jetbrains.ther.debugger.TheRDebuggerStringUtils.handleFunctionValue;
import static com.jetbrains.ther.debugger.data.TheRDebugConstants.EXECUTE_AND_STEP_COMMAND;
import static com.jetbrains.ther.debugger.executor.TheRExecutionResultType.*;
import static com.jetbrains.ther.debugger.executor.TheRExecutorUtils.execute;

class TheRDebuggerEvaluatorImpl implements TheRDebuggerEvaluator {

  @NotNull
  private final TheRExecutor myExecutor;

  @NotNull
  private final TheRFunctionDebuggerFactory myFactory;

  @NotNull
  private final TheROutputReceiver myReceiver;

  @NotNull
  private final TheRExpressionHandler myHandler;

  private final int myFrameNumber;

  public TheRDebuggerEvaluatorImpl(@NotNull final TheRExecutor executor,
                                   @NotNull final TheRFunctionDebuggerFactory factory,
                                   @NotNull final TheROutputReceiver receiver,
                                   @NotNull final TheRExpressionHandler handler,
                                   final int frameNumber) {
    myExecutor = executor;
    myFactory = factory;
    myReceiver = receiver;
    myHandler = handler;
    myFrameNumber = frameNumber;
  }

  @Override
  public void evalExpression(@NotNull final String expression, @NotNull final Receiver receiver) {
    try {
      evaluate(
        myHandler.handle(myFrameNumber, expression),
        receiver
      );
    }
    catch (final TheRDebuggerException e) {
      receiver.receiveError(e);
    }
  }

  private void evaluate(@NotNull final String expression,
                        @NotNull final Receiver receiver) throws TheRDebuggerException {
    final TheRExecutionResult result = myExecutor.execute(expression);

    switch (result.getType()) {
      case DEBUGGING_IN:
        appendError(result, myReceiver);

        receiver.receiveResult(
          handleResult(evaluateFunction())
        );

        break;
      case EMPTY:
        final String error = result.getError();

        if (!error.isEmpty()) {
          receiver.receiveError(error);
        }

        break;
      case RESPONSE:
        appendError(result, myReceiver);

        receiver.receiveResult(
          handleResult(
            result.getOutput()
          )
        );

        break;
      case DEBUG_AT:
        appendError(result, myReceiver);

        receiver.receiveResult(
          handleResult(
            execute(myExecutor, EXECUTE_AND_STEP_COMMAND, RESPONSE, myReceiver)
          )
        );

        break;
      default:
        throw new TheRUnexpectedExecutionResultException(
          "Actual type is not the same as expected: " +
          "[" +
          "actual: " + result.getType() + ", " +
          "expected: " +
          "[" + DEBUGGING_IN + ", " + EMPTY + ", " + RESPONSE + ", " + DEBUG_AT + "]" +
          "]"
        );
    }
  }

  @NotNull
  private String evaluateFunction() throws TheRDebuggerException {
    final TheRForcedFunctionDebuggerHandler handler = new TheRForcedFunctionDebuggerHandler(
      myExecutor,
      myFactory,
      myReceiver
    );

    //noinspection StatementWithEmptyBody
    while (handler.advance()) {
    }

    return handler.getResult();
  }

  @NotNull
  private String handleResult(@NotNull final String result) {
    return handleFunctionValue(result);
  }
}
