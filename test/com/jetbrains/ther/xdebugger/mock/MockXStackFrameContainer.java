package com.jetbrains.ther.xdebugger.mock;

import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MockXStackFrameContainer implements XExecutionStack.XStackFrameContainer {

  @NotNull
  private final List<XStackFrame> myResult = new ArrayList<XStackFrame>();

  @Override
  public void addStackFrames(@NotNull final List<? extends XStackFrame> stackFrames, final boolean last) {
    myResult.addAll(stackFrames);
  }

  @Override
  public boolean isObsolete() {
    throw new IllegalStateException();
  }

  @Override
  public void errorOccurred(@NotNull final String errorMessage) {
    throw new IllegalStateException();
  }

  @NotNull
  public List<XStackFrame> getResult() {
    return myResult;
  }
}
