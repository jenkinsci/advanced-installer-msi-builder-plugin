package caphyon.jenkins.advinst;

import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.tasks.Builder;

import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import jenkins.tasks.SimpleBuildStep;

/**
 * Sample {@link Builder}.
 *
 * When a build is performed, the
 * {@link AdvinstBuilder#perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked.
 *
 * @author Ciprian Burca
 */
public final class AdvinstBuilder extends Builder implements SimpleBuildStep {

  private final AdvinstParameters mAdvinstParameters;
  private String mInstallName;

  /**
   * Class DataBoundConstructor. Fields in config.jelly must match the parameter
   * names in the "DataBoundConstructor"
   *
   * @param installName                  name of the selected advinst installation name
   * @param advinstRunType               execution mode for the plugin: deploy, build
   * @param aipProjectPath               path to the Advanced Installer project to be built
   * @param aipProjectBuild              build name to be executed
   * @param aipProjectOutputFolder       output folder for the result package
   * @param aipProjectOutputName         name of the result package
   * @param advinstExtraCommands         list of aic commands to be executead against the aip
   * @param aipProjectNoDigitalSignature tells to skip the digital signature step
   */
  @DataBoundConstructor
  public AdvinstBuilder(final String installName, final String advinstRunType, final String aipProjectPath,
      final String aipProjectBuild, final String aipProjectOutputFolder, final String aipProjectOutputName,
      final String advinstExtraCommands, final boolean aipProjectNoDigitalSignature) {
    this.mInstallName = installName;
    this.mAdvinstParameters = new AdvinstParameters();
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipPath, aipProjectPath);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAdvinstRunType, advinstRunType);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipBuild, aipProjectBuild);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipOutputFolder, aipProjectOutputFolder);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipOutputName, aipProjectOutputName);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipNoDigSig, aipProjectNoDigitalSignature);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamExtraCommands, advinstExtraCommands);
  }


  @Override
  public void perform(Run<?, ?> run, FilePath wotkspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {
    boolean success;
    try {
      EnvVars env = run.getEnvironment(listener);
      // Get the build parameters
      ParametersAction parameters = run.getAction(ParametersAction.class);
      if (parameters != null) {
          for (ParameterValue value : parameters.getParameters()) {
              if (value instanceof StringParameterValue) {
                  StringParameterValue stringValue = (StringParameterValue) value;
                  env.put(stringValue.getName(), stringValue.value);
              }
          }
      }
      final String advinstComPath = getAdvinstComPath(launcher, listener, env);

      if (getAdvinstRunType().equals(AdvinstConsts.AdvinstRunTypeDeploy)) {
        return;
      }

      final FilePath advinstAipPath = getAdvinstAipPath(wotkspace, launcher, env);

      AdvinstParametersProcessor paramsProcessor = new AdvinstParametersProcessor(mAdvinstParameters, advinstAipPath,
          wotkspace, env);
      final List<String> commands = paramsProcessor.getCommands();

      AdvinstTool advinstTool = new AdvinstTool(advinstComPath);
      success = advinstTool.executeCommands(commands, advinstAipPath, build, launcher, listener, env);
      run.setResult(success ? Result.SUCCESS : Result.FAILURE);
    } catch (IOException e) {
      listener.fatalError(e.getMessage());
      run.setResult(Result.FAILURE);
    } catch (InterruptedException e) {
      listener.fatalError(e.getMessage());
      run.setResult(Result.FAILURE);
    } catch (AdvinstException e) {
      listener.fatalError(e.getMessage());
      run.setResult(Result.FAILURE);
    }
  }

  @Override
  public AdvinstDescriptorImpl getDescriptor() {
    return (AdvinstDescriptorImpl) super.getDescriptor();
  }

  public String getInstallName() {
    return this.mInstallName;
  }

  @DataBoundSetter
  public void getInstallName(final String installName) {
    this.mInstallName = installName;
  }

  public String getAdvinstRunType() {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAdvinstRunType, "build");
  }

  /**
   * @return String containing the path to the Advanced Installer project to build
   */
  public String getAipProjectPath() {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipPath, "");
  }

  /**
   * @return String containing the build name to performed
   */
  public String getAipProjectBuild() {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipBuild, "");
  }

  /**
   * @return String containing the location of the result package
   */
  public String getAipProjectOutputFolder() {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipOutputFolder, "");
  }

  /**
   * @return String containing the package name
   */
  public String getAipProjectOutputName() {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipOutputName, "");
  }

  /**
   * @return String containing additional edit commands
   */
  public String getAdvinstExtraCommands() {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamExtraCommands, "");
  }

  /**
   * @return Boolean that tells whether the digital signature step should be
   *         performed
   */
  public boolean getAipProjectNoDigitalSignature() {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipNoDigSig, false);
  }

  private String getAdvinstComPath(final Launcher launcher, final TaskListener listener, final EnvVars env)
      throws AdvinstException {

    AdvinstInstallation advinstInstall = getAdvinstInstallation();
    if (null == advinstInstall) {
      throw new AdvinstException(Messages.ERR_ADVINST_INSTALL_NOT_SET());
    }

    Computer computer = Computer.currentComputer();
    Node node = computer != null ? computer.getNode() : null;
    if (node == null) {
      throw new AdvinstException(Messages.ERR_ADVINST_COM_NOT_FOUND());
    }

    String advinstComPath = null;
    try {
      advinstInstall = advinstInstall.forNode(node, listener);
      advinstInstall = advinstInstall.forEnvironment(env);
      advinstComPath = advinstInstall.getExecutable(launcher);
      if (null == advinstComPath) {
        throw new AdvinstException(Messages.ERR_ADVINST_COM_NOT_FOUND());
      }
    } catch (IOException ex) {
      throw new AdvinstException(ex);
    } catch (InterruptedException ex) {
      throw new AdvinstException(ex);
    }

    return advinstComPath;
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private FilePath getAdvinstAipPath(final FilePath workspace, final Launcher launcher, final EnvVars env)
      throws AdvinstException {
    final String advinstAipPathParam = getAipProjectPath();
    String expandedValue = Util.replaceMacro(advinstAipPathParam, env);
    assert expandedValue != null;
    FilePath advinstAipPath = new FilePath(workspace, expandedValue);
    try {
      if (!advinstAipPath.exists()) {
        throw new AdvinstException(Messages.ERR_ADVINST_AIP_NOT_FOUND(advinstAipPath.getRemote()));
      }
    } catch (IOException e) {
      throw new AdvinstException(e);
    } catch (InterruptedException e) {
      throw new AdvinstException(e);
    }

    return advinstAipPath;
  }

  public AdvinstInstallation getAdvinstInstallation() {
    for (AdvinstInstallation i : getDescriptor().getInstallations()) {
      if (mInstallName != null && i.getName().equals(mInstallName)) {
        return i;
      }
    }
    return null;
  }


}
