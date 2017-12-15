package caphyon.jenkins.advinst;

import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.FreeStyleProject.DescriptorImpl;
import hudson.tasks.Builder;

/**
 * Sample {@link Builder}.
 * <p/>
 * <p/>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link AdvinstBuilder} is created. The created instance is persisted to the
 * project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #name}) to remember the configuration.
 * <p/>
 * <p/>
 * When a build is performed, the
 * {@link AdvinstBuilder#perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 *
 * @author Ciprian Burca
 */
public class AdvinstBuilder extends Builder {

  private final AdvinstParameters mAdvinstParameters;
  private String mInstallName;

  /**
   * Class DataBoundConstructor. Fields in config.jelly must match the
   * parameter names in the "DataBoundConstructor"
   *
   * @param installName                  name of the selected advinst installation name
   * @param aipProjectPath               path to the Advanced Installer project to be buil
   * @param aipProjectBuild              build name to be executed
   * @param aipProjectOutputFolder       output folder for the result package
   * @param aipProjectOutputName         name of the result package
   * @param aipProjectNoDigitalSignature tells to skip the digital signature
   *                                     step
   */
  @DataBoundConstructor
  public AdvinstBuilder(String installName, String aipProjectPath, String aipProjectBuild,
      String aipProjectOutputFolder, String aipProjectOutputName, String advinstExtraCommands,
      boolean aipProjectNoDigitalSignature) {
    this.mInstallName = installName;
    this.mAdvinstParameters = new AdvinstParameters();
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipPath, aipProjectPath);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipBuild, aipProjectBuild);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipOutputFolder, aipProjectOutputFolder);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipOutputName, aipProjectOutputName);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipNoDigSig, aipProjectNoDigitalSignature);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamExtraCommands, advinstExtraCommands);
  }

  /**
   * Performs the build.
   *
   * @param build
   * @param launcher
   * @param listener
   * @return success
   */
  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    boolean success;
    try {
      EnvVars env = build.getEnvironment(listener);
      final String advinstComPath = getAdvinstComPath(launcher, listener, env);
      final FilePath advinstAipPath = getAdvinstAipPath(build, launcher, env);

      AdvinstParametersProcessor paramsProcessor = new AdvinstParametersProcessor(mAdvinstParameters, advinstAipPath,
          build, env);
      final List<String> commands = paramsProcessor.getCommands();

      AdvinstTool advinstTool = new AdvinstTool(advinstComPath);
      success = advinstTool.executeCommands(commands, advinstAipPath, build, launcher, listener, env);
      build.setResult(success ? Result.SUCCESS : Result.FAILURE);
    } catch (IOException e) {
      listener.fatalError(e.getMessage());
      build.setResult(Result.FAILURE);
      return false;
    } catch (InterruptedException e) {
      listener.fatalError(e.getMessage());
      build.setResult(Result.FAILURE);
      return false;
    } catch (AdvinstException e) {
      listener.fatalError(e.getMessage());
      build.setResult(Result.FAILURE);
      return false;
    }
    return success;
  }

  @Override
  public final AdvinstDescriptorImpl getDescriptor() {
    return (AdvinstDescriptorImpl) super.getDescriptor();
  }

  public String getInstallName() {
    return this.mInstallName;
  }

  @DataBoundSetter
  public void getInstallName(String installName) {
      this.mInstallName = installName;
  }


  /**
   * @return String containing the path to the Advanced Installer project to
   * build
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
   * performed
   */
  public boolean getAipProjectNoDigitalSignature() {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipNoDigSig, false);
  }

  private String getAdvinstComPath(Launcher launcher, BuildListener listener, final EnvVars env)
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

  private FilePath getAdvinstAipPath(final AbstractBuild<?, ?> build, final Launcher launcher, final EnvVars env)
      throws AdvinstException {
    final String advinstAipPathParam = getAipProjectPath();
    String expandedValue = Util.replaceMacro(advinstAipPathParam, env);
    expandedValue = Util.replaceMacro(expandedValue, build.getBuildVariables());
    assert expandedValue != null;
    FilePath advinstAipPath = new FilePath(build.getWorkspace(), expandedValue);
    try {
      if (!advinstAipPath.exists()) {
        throw new AdvinstException(
            Messages.ERR_ADVINST_AIP_NOT_FOUND(advinstAipPath.getRemote()));
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