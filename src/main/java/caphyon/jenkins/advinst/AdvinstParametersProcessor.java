package caphyon.jenkins.advinst;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Translates the parameters provided by the user into something that Advanced Installer can work with.
 * It also validates the UI data
 * <p/>
 * It returns a list of commands to be executed.
 */
public class AdvinstParametersProcessor
{
  private static final ResourceBundle mMessagesBundle = ResourceBundle.getBundle("Messages");
  private final EnvVars mEnvVars;
  private final AdvinstParameters mUiParameters;
  private final FilePath mBuildWorkspace;
  private final FilePath mAipPath;
  private final AbstractBuild<?, ?> mBuild;

  public AdvinstParametersProcessor(final AdvinstParameters uiParams,
                                    final FilePath aipPath,
                                    final AbstractBuild build,
                                    final EnvVars envVars)
  {
    mEnvVars = envVars;
    mUiParameters = uiParams;
    mBuildWorkspace = build.getWorkspace();
    mAipPath = aipPath;
    mBuild = build;
  }

  public final List<String> getCommands() throws AdvinstException
  {
    FilePath outputFolder;
    String buildName;
    String outputFileName;

    //------------------------------------------------------------------------
    // Compute and validate output folder path. It can be either an absolute path
    // or relative to the build workspace folder.
    {
      //Because the output folder may reference environment variables, expand them
      //before computing the absolute path.
      outputFolder = getExpandedFilePathValue(AdvinstConsts.AdvinstParamAipOutputFolder);
    }

    //------------------------------------------------------------------------
    // compute and validate build name.
    {

      buildName = getExpandedStringValue(AdvinstConsts.AdvinstParamAipBuild);

      //Check if this build actually exists in the AIP
      AdvinstAipReader aipReader = new AdvinstAipReader(mAipPath);
      if (!buildName.isEmpty() && !aipReader.getBuilds().contains(buildName))
      {
        throw new AdvinstException(mMessagesBundle.getString("ERR_ADVINST_AIP_BUILD_NOT_FOUND"));
      }
    }

    //------------------------------------------------------------------------
    //compute and validate the output package name
    {
      outputFileName = getExpandedStringValue(AdvinstConsts.AdvinstParamAipOutputName);
    }

    List<String> advinstCommands = new ArrayList<String>();

    if (!buildName.isEmpty())
    {
      //These parameters require a build name;
      if (!outputFileName.isEmpty())
      {
        advinstCommands.add(String.format("SetPackageName \"%s\" -buildname \"%s\"", outputFileName, buildName));
      }

      if (null != outputFolder)
      {
        advinstCommands.add(String.format("SetOutputLocation -buildname \"%s\" -path \"%s\"", buildName, outputFolder));
      }
    }

    if (mUiParameters.get(AdvinstConsts.AdvinstParamAipNoDigSig, false))
    {
      advinstCommands.add("ResetSig");
    }

    advinstCommands.add(String.format("Build -buildslist \"%s\"", buildName));

    return advinstCommands;
  }


  private String getExpandedStringValue(final String uiParamName)
  {
    String expandedValue  = Util.replaceMacro(mUiParameters.get(uiParamName, ""), mEnvVars);
    expandedValue = Util.replaceMacro(expandedValue, mBuild.getBuildVariables());
    return expandedValue;
  }

  private FilePath getExpandedFilePathValue(final String uiParamName)
  {
    final String expandedStringValue = getExpandedStringValue(uiParamName);
    if (expandedStringValue.isEmpty())
      return null;
    return new FilePath(mBuildWorkspace, expandedStringValue);
  }

}
