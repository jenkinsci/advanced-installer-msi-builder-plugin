/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caphyon.jenkins.advinst;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
   * @param aAipFile path to the AIP project file on which the commands are
   * ran
   * @param aCommands List of commands
   * @param aLog Output parameter containing the output log of the Advanced
   * Installer commands line tool
   * @return true if all commands were successfully executed
   * @throws AdvinstException
   */
  public static boolean executeCommandsBatch(final String aAdvinstPath, final String aAipFile, final List<String> aCommands, StringBuilder aLog) throws AdvinstException
  {
    File commandsFile = null;
    try
    {
      commandsFile = createAicFile(aCommands);
    }
    catch (IOException ex)
    {
      throw new AdvinstException("Failed to create commands file. Exception: " + ex.getMessage(), ex);
    }

    final String executeCommand = String.format("\"%s\" /execute \"%s\" \"%s\"",
            aAdvinstPath,
            aAipFile,
            commandsFile.getAbsolutePath());

    Command cmd = new Command(executeCommand);
    int errCode;
    try
    {
      errCode = cmd.execute();
      aLog.append(cmd.getOutput());
      commandsFile.delete();
      return errCode == 0;
    }
    catch (IOException ex)
    {
      throw new AdvinstException("Failed to execute commands batch. Exception: " + ex.getMessage(), ex);
    }
    catch (InterruptedException ex)
    {
      throw new AdvinstException("Failed to execute commands batch. Exception: " + ex.getMessage(), ex);
    }
    finally
    {
      commandsFile.delete();
    }
  }

  private static File createAicFile(final List<String> aCommands) throws IOException
  {

    File aicFile = File.createTempFile("aic", null);
    FileOutputStream outStream = new FileOutputStream(aicFile);
    OutputStreamWriter writer = null;
    try
    {
      writer = new OutputStreamWriter(outStream, "UTF-16");
      writer.write(AdvinstConsts.AdvinstAicHeader + "\r\n");
      for (String command : aCommands)
      {
        writer.write(command + "\r\n");
      }
    }
    finally
    {
      try
      {

        if (null != writer)
        {
          writer.close();
        }
      }
      catch (IOException ex)
      {
      }
    }

    return aicFile;
  }
}
