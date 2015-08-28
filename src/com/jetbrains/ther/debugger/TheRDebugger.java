package com.jetbrains.ther.debugger;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.ther.debugger.data.TheRLocation;
import com.jetbrains.ther.debugger.evaluator.TheRDebuggerEvaluatorFactory;
import com.jetbrains.ther.debugger.evaluator.TheRExpressionHandler;
import com.jetbrains.ther.debugger.exception.TheRDebuggerException;
import com.jetbrains.ther.debugger.executor.TheRExecutionResultType;
import com.jetbrains.ther.debugger.executor.TheRExecutor;
import com.jetbrains.ther.debugger.executor.TheRExecutorUtils;
import com.jetbrains.ther.debugger.frame.TheRStackFrame;
import com.jetbrains.ther.debugger.frame.TheRValueModifierFactory;
import com.jetbrains.ther.debugger.frame.TheRValueModifierHandler;
import com.jetbrains.ther.debugger.frame.TheRVarsLoaderFactory;
import com.jetbrains.ther.debugger.function.TheRFunctionDebugger;
import com.jetbrains.ther.debugger.function.TheRFunctionDebuggerFactory;
import com.jetbrains.ther.debugger.function.TheRFunctionDebuggerHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.ther.debugger.data.TheRDebugConstants.SYS_NFRAME_COMMAND;

public class TheRDebugger implements TheRFunctionDebuggerHandler {

  @NotNull
  private static final Logger LOGGER = Logger.getInstance(TheRDebugger.class);

  @NotNull
  private final TheRExecutor myExecutor;

  @NotNull
  private final TheRFunctionDebuggerFactory myDebuggerFactory;

  @NotNull
  private final TheRVarsLoaderFactory myLoaderFactory;

  @NotNull
  private final TheRDebuggerEvaluatorFactory myEvaluatorFactory;

  @NotNull
  private final TheRScriptReader myScriptReader;

  @NotNull
  private final TheROutputReceiver myOutputReceiver;

  @NotNull
  private final TheRExpressionHandler myExpressionHandler;

  @NotNull
  private final TheRValueModifierFactory myModifierFactory;

  @NotNull
  private final TheRValueModifierHandler myModifierHandler;

  @NotNull
  private final List<TheRFunctionDebugger> myDebuggers;

  @NotNull
  private final List<TheRStackFrame> myStack;

  @NotNull
  private final List<TheRStackFrame> myUnmodifiableStack;

  private int myReturnLineNumber;

  private int myDropFrames;

  public TheRDebugger(@NotNull final TheRExecutor executor,
                      @NotNull final TheRFunctionDebuggerFactory debuggerFactory,
                      @NotNull final TheRVarsLoaderFactory loaderFactory,
                      @NotNull final TheRDebuggerEvaluatorFactory evaluatorFactory,
                      @NotNull final TheRScriptReader scriptReader,
                      @NotNull final TheROutputReceiver outputReceiver,
                      @NotNull final TheRExpressionHandler expressionHandler,
                      @NotNull final TheRValueModifierFactory modifierFactory,
                      @NotNull final TheRValueModifierHandler modifierHandler) throws TheRDebuggerException {
    myExecutor = executor;
    myDebuggerFactory = debuggerFactory;
    myLoaderFactory = loaderFactory;

    myEvaluatorFactory = evaluatorFactory;
    myScriptReader = scriptReader;
    myOutputReceiver = outputReceiver;
    myExpressionHandler = expressionHandler;
    myModifierFactory = modifierFactory;
    myModifierHandler = modifierHandler;

    myDebuggers = new ArrayList<TheRFunctionDebugger>();
    myStack = new ArrayList<TheRStackFrame>();
    myUnmodifiableStack = Collections.unmodifiableList(myStack);

    myReturnLineNumber = -1;
    myDropFrames = 1;

    appendDebugger(
      myDebuggerFactory.getMainFunctionDebugger(
        myExecutor,
        this,
        myOutputReceiver,
        myScriptReader
      )
    );
  }

  public boolean advance() throws TheRDebuggerException {
    topDebugger().advance(); // Don't forget that advance could append new debugger

    while (!topDebugger().hasNext()) {
      if (myDebuggers.size() == 1) {
        return false;
      }

      for (int i = 0; i < myDropFrames; i++) {
        popDebugger();
      }

      myDropFrames = 1;
    }

    final TheRLocation topLocation = getTopLocation();
    final TheRStackFrame lastFrame = myStack.get(myStack.size() - 1);

    myStack.set(
      myStack.size() - 1,
      new TheRStackFrame(
        topLocation,
        lastFrame.getLoader(),
        lastFrame.getEvaluator()
      )
    );

    return true;
  }

  @NotNull
  private TheRLocation getTopLocation() {
    final TheRFunctionDebugger topDebugger = topDebugger();

    if (myReturnLineNumber != -1) {
      final TheRLocation result = new TheRLocation(
        topDebugger.getLocation().getFunctionName(),
        myReturnLineNumber
      );

      myReturnLineNumber = -1;

      return result;
    }

    return topDebugger.getLocation();
  }

  @NotNull
  public List<TheRStackFrame> getStack() {
    return myUnmodifiableStack;
  }

  public void stop() {
    try {
      myScriptReader.close();
    }
    catch (final IOException e) {
      LOGGER.warn(e);
    }
  }

  @Override
  public void appendDebugger(@NotNull final TheRFunctionDebugger debugger) throws TheRDebuggerException {
    myDebuggers.add(debugger);
    myStack.add(
      new TheRStackFrame(
        debugger.getLocation(),
        myLoaderFactory.getLoader(
          myModifierFactory.getModifier(
            myExecutor,
            myDebuggerFactory,
            myOutputReceiver,
            myModifierHandler,
            myStack.size()
          ),
          myStack.isEmpty() ? 0 : loadFrameNumber()
        ),
        myEvaluatorFactory.getEvaluator(
          myExecutor,
          myDebuggerFactory,
          myOutputReceiver,
          myExpressionHandler,
          myStack.size()
        )
      )
    );

    myExpressionHandler.setMaxFrameNumber(myStack.size() - 1);
    myModifierHandler.setMaxFrameNumber(myStack.size() - 1);
  }

  @Override
  public void setReturnLineNumber(final int lineNumber) {
    myReturnLineNumber = lineNumber;
  }

  @Override
  public void setDropFrames(final int number) {
    myDropFrames = number;
  }

  @NotNull
  private TheRFunctionDebugger topDebugger() {
    return myDebuggers.get(myDebuggers.size() - 1);
  }

  private void popDebugger() {
    myDebuggers.remove(myDebuggers.size() - 1);
    myStack.remove(myStack.size() - 1);

    myExpressionHandler.setMaxFrameNumber(myStack.size() - 1);
    myModifierHandler.setMaxFrameNumber(myStack.size() - 1);
  }

  private int loadFrameNumber() throws TheRDebuggerException {
    final String frameNumber =
      TheRExecutorUtils.execute(myExecutor, SYS_NFRAME_COMMAND, TheRExecutionResultType.RESPONSE, myOutputReceiver);

    return Integer.parseInt(frameNumber.substring("[1] ".length()));
  }
}
