/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caphyon.jenkins.advinst;

import hudson.FilePath;

import java.io.IOException;
import java.util.List;

/**
 * Commands for Advanced Installer
 *
 * @author Ciprian Burca
 */
public class AdvinstCommands
{

  /**
   * Executes a batch of commands on the specified AIP file.
   *
   * @param aAdvinstPath Path to advanced installer command line tool
   * @param aAipFile     path to the AIP project file on which the commands are
   *                     ran
   * @param aCommands    List of commands
   * @param aLog         Output parameter containing the output log of the Advanced
   *                     Installer commands line tool
   * @return true if all commands were successfully executed
   * @throws AdvinstException
   */
  public static boolean executeCommandsBatch(final FilePath aAdvinstPath, final FilePath aAipFile, final FilePath aWorkFolder,
                                             final List<String> aCommands, StringBuilder aLog) throws AdvinstException
  {
    FilePath commandsFile = null;
    try
    {
      commandsFile = aWorkFolder;
      commandsFile = aWorkFolder.createTempFile("aic", "");
      createAicFile(commandsFile, aCommands);
      final String executeCommand = String.format("\"%s\" /execute \"%s\" \"%s\"",
        aAdvinstPath,
        aAipFile,
        commandsFile);
      Command cmd = new Command(executeCommand);
      int errCode = cmd.execute();
      aLog.append(cmd.getOutput());
      commandsFile.delete();
      return 0 == errCode;
    }
    catch (IOException ex)
    {
      throw new AdvinstException("Failed to create commands file. Exception: " + ex.getMessage(), ex);
    }
    catch (InterruptedException ex)
    {
      throw new AdvinstException("Failed to execute commands batch. Exception: " + ex.getMessage(), ex);
    }
    finally
    {
      if (null != commandsFile)
      {
        try
        {
          commandsFile.delete();
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
  }

  private static boolean createAicFile(final FilePath cmdsFilePath, final List<String> aCommands) throws IOException, InterruptedException
  {
    String fileContent = AdvinstConsts.AdvinstAicHeader + "\r\n";
    for (String command : aCommands)
    {
      fileContent += command;
      fileContent += "\r\n";
    }

    cmdsFilePath.write(fileContent, "UTF-16");
    return true;
  }

}
