package com.jetbrains.ther.run.configuration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.ther.run.debug.TheRDebugCommandLineState;
import com.jetbrains.ther.run.run.TheRRunCommandLineState;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// TODO [run][test]
public class TheRRunConfiguration extends LocatableConfigurationBase implements TheRRunConfigurationParams {

  @NotNull
  private static final String SCRIPT_PATH = "SCRIPT_PATH";

  @NotNull
  private static final String SCRIPT_ARGS = "SCRIPT_ARGS";

  @NotNull
  private static final String WORKING_DIRECTORY_PATH = "WORKING_DIRECTORY_PATH";

  @NotNull
  private static final String PASS_PARENT_ENVS = "PASS_PARENT_ENVS";

  @NotNull
  private String myScriptPath;

  @NotNull
  private String myScriptArgs;

  @NotNull
  private String myWorkingDirectoryPath;

  @NotNull
  private final Map<String, String> myEnvs;

  private boolean myPassParentEnvs;

  TheRRunConfiguration(@NotNull final Project project, @NotNull final ConfigurationFactory configurationFactory) {
    super(project, configurationFactory, "");

    myScriptPath = "";
    myScriptArgs = "";
    myWorkingDirectoryPath = "";
    myEnvs = new LinkedHashMap<String, String>();
    myPassParentEnvs = true;
  }

  @Override
  public RunProfileState getState(@NotNull final Executor executor, @NotNull final ExecutionEnvironment env) throws ExecutionException {
    if (executor.getId().equals(DefaultDebugExecutor.EXECUTOR_ID)) {
      return new TheRDebugCommandLineState(env, this);
    }
    else {
      return new TheRRunCommandLineState(env, this);
    }
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new TheRRunConfigurationEditor(this);
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (StringUtil.isEmptyOrSpaces(myScriptPath)) {
      throw new RuntimeConfigurationException("No script specified");
    }

    // TODO [run][upd]
  }

  @Override
  @Nullable
  public String suggestedName() {
    return TheRRunConfigurationSuggests.suggestedName(this);
  }

  @NotNull
  @Override
  public String getScriptPath() {
    return myScriptPath;
  }

  @Override
  public void setScriptPath(@NotNull final String scriptPath) {
    myScriptPath = scriptPath;
  }

  @NotNull
  @Override
  public String getScriptArgs() {
    return myScriptArgs;
  }

  @Override
  public void setScriptArgs(@NotNull final String scriptArgs) {
    myScriptArgs = scriptArgs;
  }

  @NotNull
  @Override
  public String getWorkingDirectoryPath() {
    return myWorkingDirectoryPath;
  }

  @Override
  public void setWorkingDirectoryPath(@NotNull final String workingDirectoryPath) {
    myWorkingDirectoryPath = workingDirectoryPath;
  }

  @Override
  @NotNull
  public Map<String, String> getEnvs() {
    return myEnvs;
  }

  @Override
  public void setEnvs(@NotNull final Map<String, String> envs) {
    myEnvs.clear();
    myEnvs.putAll(envs);
  }

  @Override
  public boolean isPassParentEnvs() {
    return myPassParentEnvs;
  }

  @Override
  public void setPassParentEnvs(final boolean passParentEnvs) {
    myPassParentEnvs = passParentEnvs;
  }

  @Override
  public void readExternal(@NotNull final Element element) throws InvalidDataException {
    PathMacroManager.getInstance(getProject()).expandPaths(element);

    super.readExternal(element);

    myScriptPath = JDOMExternalizerUtil.readField(element, SCRIPT_PATH, "");
    myScriptArgs = JDOMExternalizerUtil.readField(element, SCRIPT_ARGS, "");
    myWorkingDirectoryPath = JDOMExternalizerUtil.readField(element, WORKING_DIRECTORY_PATH, "");

    readEnvs(element);
  }

  @Override
  public void writeExternal(@NotNull final Element element) throws WriteExternalException {
    super.writeExternal(element);

    JDOMExternalizerUtil.writeField(element, SCRIPT_PATH, myScriptPath);
    JDOMExternalizerUtil.writeField(element, SCRIPT_ARGS, myScriptArgs);
    JDOMExternalizerUtil.writeField(element, WORKING_DIRECTORY_PATH, myWorkingDirectoryPath);

    writeEnvs(element);

    PathMacroManager.getInstance(getProject()).collapsePathsRecursively(element);
  }

  public static void copyParams(@NotNull final TheRRunConfigurationParams source, @NotNull final TheRRunConfigurationParams target) {
    target.setScriptPath(source.getScriptPath());
    target.setScriptArgs(source.getScriptArgs());
    target.setWorkingDirectoryPath(source.getWorkingDirectoryPath());
    target.setPassParentEnvs(source.isPassParentEnvs());
    target.setEnvs(Collections.unmodifiableMap(source.getEnvs()));
  }

  private void readEnvs(@NotNull final Element element) {
    setPassParentEnvs(
      Boolean.parseBoolean(
        JDOMExternalizerUtil.readField(element, PASS_PARENT_ENVS, "")
      )
    );

    EnvironmentVariablesComponent.readExternal(element, getEnvs());
  }

  private void writeEnvs(@NotNull final Element element) {
    JDOMExternalizerUtil.writeField(element, PASS_PARENT_ENVS, Boolean.toString(isPassParentEnvs()));

    EnvironmentVariablesComponent.writeExternal(element, getEnvs());
  }
}
