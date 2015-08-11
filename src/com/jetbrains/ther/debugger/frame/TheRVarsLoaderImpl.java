package com.jetbrains.ther.debugger.frame;

import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.ther.debugger.TheRDebuggerStringUtils;
import com.jetbrains.ther.debugger.TheROutputReceiver;
import com.jetbrains.ther.debugger.data.TheRVar;
import com.jetbrains.ther.debugger.exception.TheRDebuggerException;
import com.jetbrains.ther.debugger.exception.TheRUnexpectedResponseException;
import com.jetbrains.ther.debugger.interpreter.TheRProcess;
import com.jetbrains.ther.debugger.interpreter.TheRProcessResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static com.jetbrains.ther.debugger.data.TheRDebugConstants.*;
import static com.jetbrains.ther.debugger.interpreter.TheRProcessResponseType.DEBUG_AT;
import static com.jetbrains.ther.debugger.interpreter.TheRProcessResponseType.RESPONSE;
import static com.jetbrains.ther.debugger.interpreter.TheRProcessUtils.execute;

class TheRVarsLoaderImpl implements TheRVarsLoader {

  @NotNull
  private final TheRProcess myProcess;

  @NotNull
  private final TheROutputReceiver myReceiver;

  @NotNull
  private final String myFrame;

  public TheRVarsLoaderImpl(@NotNull final TheRProcess process,
                            @NotNull final TheROutputReceiver receiver,
                            final int frameNumber) {
    myProcess = process;
    myReceiver = receiver;
    myFrame = SYS_FRAME_COMMAND + "(" + frameNumber + ")";
  }

  @NotNull
  @Override
  public List<TheRVar> load() throws TheRDebuggerException {
    final String text = execute(
      myProcess,
      LS_COMMAND + "(" + myFrame + ")",
      RESPONSE,
      myReceiver
    );

    final List<TheRVar> vars = new ArrayList<TheRVar>();

    for (final String variableName : calculateVariableNames(text)) {
      final TheRVar var = loadVar(variableName);

      if (var != null) {
        vars.add(var);
      }
    }

    return vars;
  }

  @NotNull
  private List<String> calculateVariableNames(@NotNull final String response) {
    final List<String> result = new ArrayList<String>();

    for (final String line : StringUtil.splitByLines(response)) {
      for (final String token : StringUtil.tokenize(new StringTokenizer(line))) {
        final String var = getVariableName(token);

        if (var != null) {
          result.add(var);
        }
      }
    }

    return result;
  }

  @Nullable
  private TheRVar loadVar(@NotNull final String var) throws TheRDebuggerException {
    final String type = handleType(
      var,
      execute(
        myProcess,
        TYPEOF_COMMAND + "(" + myFrame + "$" + var + ")",
        RESPONSE,
        myReceiver
      )
    );

    if (type == null) {
      return null;
    }

    return new TheRVar(
      var,
      type,
      loadValue(var, type)
    );
  }

  @Nullable
  private String getVariableName(@NotNull final String token) {
    final boolean isNotEmptyQuotedString = StringUtil.isQuotedString(token) && token.length() > 2;

    if (isNotEmptyQuotedString) {
      return token.substring(1, token.length() - 1);
    }
    else {
      return null;
    }
  }

  @Nullable
  private String handleType(@NotNull final String var,
                            @NotNull final String type) {
    if (type.equals(FUNCTION_TYPE) && isService(var)) {
      return null;
    }

    return type;
  }

  @NotNull
  private String loadValue(@NotNull final String var,
                           @NotNull final String type) throws TheRDebuggerException {
    final TheRProcessResponse response = execute(myProcess, valueCommand(var), myReceiver);

    switch (response.getType()) {
      case RESPONSE:
        return handleValue(
          type,
          response.getOutput()
        );
      case DEBUG_AT:
        return handleValue(
          type,
          execute(
            myProcess,
            EXECUTE_AND_STEP_COMMAND,
            RESPONSE,
            myReceiver
          )
        );
      default:
        throw new TheRUnexpectedResponseException(
          "Actual response type is not the same as expected: " +
          "[" +
          "actual: " + response.getType() + ", " +
          "expected: " +
          "[" + RESPONSE + ", " + DEBUG_AT + "]" +
          "]"
        );
    }
  }

  private boolean isService(@NotNull final String var) {
    return var.startsWith(SERVICE_FUNCTION_PREFIX) && var.endsWith(SERVICE_ENTER_FUNCTION_SUFFIX);
  }

  @NotNull
  private String handleValue(@NotNull final String type,
                             @NotNull final String value) {
    if (type.equals(FUNCTION_TYPE)) {
      return TheRDebuggerStringUtils.handleFunctionValue(value);
    }
    else {
      return value;
    }
  }

  @NotNull
  private String valueCommand(@NotNull final String var) {
    final String globalVar = myFrame + "$" + var;

    final String isFunction = TYPEOF_COMMAND + "(" + globalVar + ") == \"" + CLOSURE + "\"";
    final String isDebugged = IS_DEBUGGED_COMMAND + "(" + globalVar + ")";

    return "if (" + isFunction + " && " + isDebugged + ") " +
           ATTR_COMMAND + "(" + globalVar + ", \"original\")" +
           " else " +
           globalVar;
  }
}
