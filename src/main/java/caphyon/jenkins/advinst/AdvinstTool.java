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
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.ArgumentListBuilder;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

public class AdvinstTool
{
  private static final ResourceBundle mMessagesBundle = ResourceBundle.getBundle("Messages");
  private final FilePath mAdvinstComPath;


  public AdvinstTool(final FilePath advinstComPath)
  {
    this.mAdvinstComPath = advinstComPath;
  }

  public boolean executeCommands(final List<String> commands,
                                 final FilePath aipPath,
                                 AbstractBuild build,
                                 Launcher launcher,
                                 BuildListener listener,
                                 EnvVars env) throws AdvinstException
  {
    FilePath aicFilePath = null;
    try
    {
      if ( launcher.isUnix() )
      {
        throw new AdvinstException(mMessagesBundle.getString("ERR_ADVINST_UNSUPPORTED_OS"));
      }
      FilePath pwd = build.getWorkspace();
      aicFilePath = createAicFile(build.getWorkspace(), commands);
      ArgumentListBuilder cmdExecArgs = new ArgumentListBuilder();
      cmdExecArgs.add(mAdvinstComPath.getRemote(), "/execute",
        aipPath.getRemote(), aicFilePath.getRemote());

      int result = launcher.launch().cmds(cmdExecArgs).envs(env).stdout(listener).pwd(pwd).join();
      return 0 == result;

    }
    catch (IOException e)
    {
      throw new AdvinstException(e);
    }
    catch (InterruptedException e)
    {
      throw new AdvinstException(e);
    }
    finally
    {
      try
      {
        if (aicFilePath != null)
        {
          aicFilePath.delete();
        }
      }
      catch (IOException e)
      {
        throw new AdvinstException(e);
      }
      catch (InterruptedException e)
      {
        throw new AdvinstException(e);
      }
    }
  }

  private static FilePath createAicFile(final FilePath buildWorkspace, final List<String> aCommands) throws IOException, InterruptedException
  {
    FilePath aicFile = buildWorkspace.createTempFile("aic", "aic");
    String fileContent = AdvinstConsts.AdvinstAicHeader + "\r\n";
    for (String command : aCommands)
    {
      fileContent += command;
      fileContent += "\r\n";
    }

    aicFile.write(fileContent, "UTF-16");
    return aicFile;
  }
}