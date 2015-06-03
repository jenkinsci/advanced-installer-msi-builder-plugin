package caphyon.jenkins.advinst;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject.DescriptorImpl;
import hudson.model.Result;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ResourceBundle;

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
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 *
 * @author Ciprian Burca
 */
public class AdvinstBuilder extends Builder
{

  private static final ResourceBundle mMessagesBundle = ResourceBundle.getBundle("Messages");
  private final AdvinstParameters mAdvinstParameters;

  /**
   * Class DataBoundConstructor. Fields in config.jelly must match the
   * parameter names in the "DataBoundConstructor"
   *
   * @param aipProjectPath               path to the Advanced Installer project to be buil
   * @param aipProjectBuild              build name to be executed
   * @param aipProjectOutputFolder       output folder for the result package
   * @param aipProjectOutputName         name of the result package
   * @param aipProjectNoDigitalSignature tells to skip the digital signature
   *                                     step
   */
  @DataBoundConstructor
  public AdvinstBuilder(String aipProjectPath, String aipProjectBuild,
                        String aipProjectOutputFolder, String aipProjectOutputName,
                        boolean aipProjectNoDigitalSignature)
  {
    this.mAdvinstParameters = new AdvinstParameters();
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipPath, aipProjectPath);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipBuild, aipProjectBuild);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipOutputFolder, aipProjectOutputFolder);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipOutputName, aipProjectOutputName);
    this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAipNoDigSig, aipProjectNoDigitalSignature);
  }


  /**
   * Performs the build.
   *
   * @param build
   * @param launcher
   * @param listener
   * @return
   */
  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
  {
    try
    {
      this.mAdvinstParameters.set(AdvinstConsts.AdvinstParamAdvinstRootPath, getDescriptor().getAdvinstRootPath());
      AdvinstTool advinstTool = new AdvinstTool(build.getEnvironment(listener), mAdvinstParameters);
      StringBuilder advinstBuildLog = new StringBuilder();
      return advinstTool.Build(build.getWorkspace(), advinstBuildLog);
    }
    catch (IOException e)
    {
      listener.fatalError(e.getMessage());
      build.setResult(Result.FAILURE);
      return false;
    }
    catch (InterruptedException e)
    {
      listener.fatalError(e.getMessage());
      build.setResult(Result.FAILURE);
      return false;
    }
    catch (AdvinstException e)
    {
      listener.fatalError(e.getMessage());
      build.setResult(Result.FAILURE);
      return false;
    }
  }

  /**
   * @return
   */
  @Override
  public final AdvinstDescriptorImpl getDescriptor()
  {
    return (AdvinstDescriptorImpl) super.getDescriptor();
  }

  /**
   * @return String containing the path to the Advanced Installer project to
   * build
   */
  public String getAipProjectPath()
  {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipPath, "");
  }

  /**
   * @return String containing the build name to performed
   */
  public String getAipProjectBuild()
  {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipBuild, "");
  }

  /**
   * @return String containing the location of the result package
   */
  public String getAipProjectOutputFolder()
  {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipOutputFolder, "");
  }

  /**
   * @return String containing the package name
   */
  public String getAipProjectOutputName()
  {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipOutputName, "");
  }

  /**
   * @return Boolean that tells whether the digital signature step should be
   * performed
   */
  public boolean getAipProjectNoDigitalSignature()
  {
    return this.mAdvinstParameters.get(AdvinstConsts.AdvinstParamAipNoDigSig, false);
  }
}
