/*
 * The MIT License
 *
 * Copyright 2015 Ciprian Burca.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package caphyon.jenkins.advinst;

import hudson.EnvVars;
import hudson.FilePath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdvinstTool
{

  private static final ResourceBundle mMessagesBundle = ResourceBundle.getBundle("Messages");
  private final EnvVars mEnvVars;
  private final AdvinstParameters mParameters;

  public AdvinstTool(EnvVars envVars, AdvinstParameters advinstParams)
  {
    this.mEnvVars = envVars;
    this.mParameters = advinstParams;
  }

  public boolean Build(final FilePath buildWorkspace, StringBuilder outputLog) throws AdvinstException
  {
    boolean success = false;

    FilePath absoluteAipPath = null;
    FilePath absoluteOutputFolder = null;
    String buildName = "";
    String packageName = "";

    //-------------------------------------------------------------------------
    // Compute and validate AIP project path. It can be either an absolute path
    // or relative to the build workspace folder
    {
      try
      {
        //Because the output folder may reference environment variables, expand them
        //before computing the absolute path.
        String expandedValue = mEnvVars.expand(mParameters.get(AdvinstConsts.AdvinstParamAipPath, ""));

        absoluteAipPath = new FilePath(buildWorkspace, expandedValue);
        if (!absoluteAipPath.exists())
        {
          throw new AdvinstException(String.format(mMessagesBundle.getString("ERR_ADVINST_AIP_NOT_FOUND"), absoluteAipPath));
        }
      }
      catch (IOException iOException)
      {
        throw new AdvinstException(mMessagesBundle.getString("ERR_ADVINST_AIP_PATH_COMPUTE"));
      }
      catch (InterruptedException interruptedException)
      {
        throw new AdvinstException(mMessagesBundle.getString("ERR_ADVINST_AIP_PATH_COMPUTE"));
      }
    }

    //------------------------------------------------------------------------
    // Compute and validate output folder path. It can be either an absolute path
    // or relative to the build workspace folder.
    {
      //Because the output folder may reference environment variables, expand them
      //before computing the absolute path.
      String expandedValue = mEnvVars.expand(mParameters.get(AdvinstConsts.AdvinstParamAipOutputFolder, ""));
      if (!expandedValue.isEmpty())
      {
        absoluteOutputFolder = new FilePath(buildWorkspace, expandedValue);
      }
    }

    //------------------------------------------------------------------------
    // compute and validate build name.
    {

      buildName = mEnvVars.expand(mParameters.get(AdvinstConsts.AdvinstParamAipBuild, ""));

      //Check if this build actually exists in the AIP
      AdvinstAipReader aipReader = new AdvinstAipReader(absoluteAipPath);
      if (!buildName.isEmpty() && !aipReader.getBuilds().contains(buildName))
      {
        throw new AdvinstException(mMessagesBundle.getString("ERR_ADVINST_AIP_BUILD_NOT_FOUND"));
      }
    }

    //------------------------------------------------------------------------
    //compute and validate the output package name
    {
      packageName = mEnvVars.expand(mParameters.get(AdvinstConsts.AdvinstParamAipOutputName, ""));
    }

    //------------------------------------------------------------------------
    //make the necesary configurations and run the build
    {

      List<String> advinstCommands = new ArrayList<String>();

      if (!packageName.isEmpty())
      {
        advinstCommands.add(String.format("SetPackageName \"%s\" -buildname \"%s\"", packageName, buildName));
      }

      if (null != absoluteOutputFolder)
      {
        advinstCommands.add(String.format("SetOutputLocation -buildname \"%s\" -path \"%s\"", buildName, absoluteOutputFolder));
      }

      if (mParameters.get(AdvinstConsts.AdvinstParamAipNoDigSig, false))
      {
        advinstCommands.add("ResetSig");
      }

      advinstCommands.add(String.format("Build -buildslist \"%s\"", buildName));


      success = AdvinstCommands.executeCommandsBatch(getAdvinstComPath(),
        absoluteAipPath, buildWorkspace, advinstCommands, outputLog);
      return success;
    }
  }

  private FilePath getAdvinstComPath() throws AdvinstException
  {
    try
    {
      final String advinstRootPathParam = mParameters.get(AdvinstConsts.AdvinstParamAdvinstRootPath, "");
      if (advinstRootPathParam.isEmpty())
      {
        throw new AdvinstException(mMessagesBundle.getString("ERR_ADVINST_FOLDER_NOT_SET"), null);
      }

      FilePath advinstRootPath = new FilePath(new File(advinstRootPathParam));
      FilePath advinstComPath = new FilePath(advinstRootPath, AdvinstConsts.AdvinstComSubPath);
      if (!advinstComPath.exists())
      {
        throw new AdvinstException(String.format(mMessagesBundle.getString("ERR_ADVINST_COM_NOT_FOUND"), advinstComPath.toURI()), null);
      }

      return advinstComPath;
    }
    catch (IOException ex)
    {
      throw new AdvinstException(ex.getMessage(), ex);
    }
    catch (InterruptedException ex)
    {
      throw new AdvinstException(ex.getMessage(), ex);
    }
  }
}
