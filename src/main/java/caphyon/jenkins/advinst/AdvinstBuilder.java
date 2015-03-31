package caphyon.jenkins.advinst;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject.DescriptorImpl;
import hudson.model.Result;
import hudson.tasks.Builder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link AdvinstBuilder} is created. The created instance is persisted to the
 * project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #name}) to remember the configuration.
 *
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 *
 * @author Ciprian Burca
 */
public class AdvinstBuilder extends Builder
{

  private static final ResourceBundle mMessagesBundle = ResourceBundle.getBundle("Messages");
  private final String mAipProjectPath;
  private final String mAipProjectBuild;
  private final String mAipProjectOutputFolder;
  private final String mAipProjectOutputName;
  private final boolean mAipProjectNoDigitalSignature;

  /**
   * Class DataBoundConstructor. Fields in config.jelly must match the
   * parameter names in the "DataBoundConstructor"
   *
   * @param aipProjectPath path to the Advanced Installer project to be built
   * @param aipProjectBuild build name to be executed
   * @param aipProjectOutputFolder output folder for the result package
   * @param aipProjectOutputName name of the result package
   * @param aipProjectNoDigitalSignature tells to skip the digital signature
   * step
   */
  @DataBoundConstructor
  public AdvinstBuilder(String aipProjectPath, String aipProjectBuild,
          String aipProjectOutputFolder, String aipProjectOutputName,
          boolean aipProjectNoDigitalSignature)
  {
    this.mAipProjectPath = aipProjectPath;
    this.mAipProjectBuild = aipProjectBuild;
    this.mAipProjectOutputFolder = aipProjectOutputFolder;
    this.mAipProjectOutputName = aipProjectOutputName;
    this.mAipProjectNoDigitalSignature = aipProjectNoDigitalSignature;
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
    boolean success = false;

    String advinstComPath = getDescriptor().getAdvinstComPath();
    String absoluteAipPath = "";
    String absoluteOutputFolder = "";
    String buildName = "";
    String packageName = "";

    //------------------------------------------------------------------------
    // Validate Advanced Installer path
    {
      if (advinstComPath.isEmpty())
      {
        listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_FOLDER_NOT_SET"));
        build.setResult(Result.FAILURE);
        return false;
      }

      if (Files.notExists(Paths.get(advinstComPath)))
      {
        listener.fatalError(String.format(mMessagesBundle.getString("ERR_ADVINST_COM_NOT_FOUND"), advinstComPath));
        build.setResult(Result.FAILURE);
        return false;
      }
    }

    //-------------------------------------------------------------------------
    // Compute and validate AIP project path. It can be either an absolute path
    // or relative to the build workspace folder
    {
      try
      {
        //Because the output folder may reference environment variables, expand them
        //before computing the absolute path.
        EnvVars envVars = build.getEnvironment(listener);
        String expandedValue = envVars.expand(getAipProjectPath());

        File aipPath = new File(expandedValue);
        if (aipPath.isAbsolute())
        {
          absoluteAipPath = expandedValue;
        }
        else //compute absolute path using build.getWorkspace() as root.
        {
          File root = new File(build.getWorkspace().toURI());
          absoluteAipPath = new File(root, aipPath.getPath()).getAbsolutePath();
        }
      }
      catch (IOException iOException)
      {
        listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_AIP_PATH_COMPUTE"));
        build.setResult(Result.FAILURE);
        return false;
      }
      catch (InterruptedException interruptedException)
      {
        listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_AIP_PATH_COMPUTE"));
        build.setResult(Result.FAILURE);
        return false;
      }

      if (Files.notExists(Paths.get(absoluteAipPath)))
      {
        listener.fatalError(String.format(mMessagesBundle.getString("ERR_ADVINST_AIP_NOT_FOUND"), absoluteAipPath));
        build.setResult(Result.FAILURE);
        return false;
      }
    }

    //------------------------------------------------------------------------
    // Compute and validate output folder path. It can be either an absolute path
    // or relative to the build workspace folder.
    {
      try
      {
        //Because the output folder may reference environment variables, expand them
        //before computing the absolute path.
        EnvVars envVars = build.getEnvironment(listener);
        String expandedValue = envVars.expand(getAipProjectOutputFolder());

        File outputFolder = new File(expandedValue);
        if (outputFolder.isAbsolute())
        {
          absoluteOutputFolder = getAipProjectOutputFolder();
        }
        else
        {
          File root = new File(build.getWorkspace().toURI());
          absoluteOutputFolder = new File(root, outputFolder.getPath()).getAbsolutePath();
        }
      }
      catch (IOException iOException)
      {
        listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_AIP_OUTPUT_PATH_COMPUTE"));
        build.setResult(Result.FAILURE);
        return false;
      }
      catch (InterruptedException interruptedException)
      {
        listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_AIP_OUTPUT_PATH_COMPUTE"));
        build.setResult(Result.FAILURE);
        return false;
      }
    }

    //------------------------------------------------------------------------
    // compute and validate build name.
    {
      try
      {
        EnvVars envVars = build.getEnvironment(listener);
        buildName = envVars.expand(getAipProjectBuild());
      }
      catch (IOException ex)
      {
        listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_AIP_BUILD_NAME_COMPUTE"));
        build.setResult(Result.FAILURE);
        return false;
      }
      catch (InterruptedException ex)
      {
        listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_AIP_BUILD_NAME_COMPUTE"));
        build.setResult(Result.FAILURE);
        return false;
      }

      //Check if this build actually exists in the AIP
      try
      {
        AdvinstAipReader aipReader = new AdvinstAipReader(absoluteAipPath);
        if (!buildName.isEmpty() && !aipReader.getBuilds().contains(buildName))
        {
          listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_AIP_BUILD_NOT_FOUND"));
          build.setResult(Result.FAILURE);
          return false;
        }
      }
      catch (AdvinstException ex)
      {
        listener.fatalError(ex.getMessage());
        build.setResult(Result.FAILURE);
        return false;
      }
    }

    //------------------------------------------------------------------------
    //compute and validate the output package name
    {
      try
      {
        EnvVars envVars = build.getEnvironment(listener);
        packageName = envVars.expand(getAipProjectOutputName());
      }
      catch (IOException ex)
      {
        listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_AIP_OUTPUT_NAME_COMPUTE"));
        build.setResult(Result.FAILURE);
        return false;
      }
      catch (InterruptedException ex)
      {
        listener.fatalError(mMessagesBundle.getString("ERR_ADVINST_AIP_OUTPUT_NAME_COMPUTE"));
        build.setResult(Result.FAILURE);
        return false;
      }
    }

    //------------------------------------------------------------------------
    //make the necesary configurations and run the build
    {
      try
      {
        List<String> advinstCommands = new ArrayList<String>();

        if (!packageName.isEmpty())
        {
          advinstCommands.add(String.format("SetPackageName \"%s\" -buildname \"%s\"", packageName, buildName));
        }

        if (!absoluteOutputFolder.isEmpty())
        {
          advinstCommands.add(String.format("SetOutputLocation -buildname \"%s\" -path \"%s\"", buildName, absoluteOutputFolder));
        }

        if (getAipProjectNoDigitalSignature())
        {
          advinstCommands.add("ResetSig");
        }

        advinstCommands.add(String.format("Build -buildslist \"%s\"", buildName));

        StringBuilder executeLog = new StringBuilder();
        success = AdvinstCommands.executeCommandsBatch(advinstComPath, absoluteAipPath, advinstCommands, executeLog);
        listener.getLogger().println(executeLog);
        build.setResult(success ? Result.SUCCESS : Result.FAILURE);
        return success;
      }
      catch (AdvinstException ex)
      {
        listener.fatalError(ex.getMessage());
        build.setResult(Result.FAILURE);
        return false;
      }
    }
  }

  /**
   *
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
    return this.mAipProjectPath;
  }

  /**
   * @return String containing the build name to performed
   */
  public String getAipProjectBuild()
  {
    return this.mAipProjectBuild;
  }

  /**
   * @return String containing the location of the result package
   */
  public String getAipProjectOutputFolder()
  {
    return this.mAipProjectOutputFolder;
  }

  /**
   * @return String containing the package name
   */
  public String getAipProjectOutputName()
  {
    return this.mAipProjectOutputName;
  }

  /**
   * @return Boolean that tells whether the digital signature step should be
   * performed
   */
  public boolean getAipProjectNoDigitalSignature()
  {
    return this.mAipProjectNoDigitalSignature;
  }
}
