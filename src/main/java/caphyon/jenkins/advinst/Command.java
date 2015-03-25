/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caphyon.jenkins.advinst;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Executes an advanced installed command line option.
 *
 * @author Ciprian
 */
public class Command
{

  private final String mCommand;
  private String mOutput = "";

  public Command(String aCommand)
  {
    this.mCommand = aCommand;
  }

  public int execute() throws IOException, InterruptedException
  {
    int exitCcode;
    Process exec = Runtime.getRuntime().exec(mCommand);

    BufferedReader stdout = null;
    try
    {
      InputStreamReader inputReader = new InputStreamReader(exec.getInputStream());
      stdout = new BufferedReader(inputReader);
      int c;
      while ((c = stdout.read()) != -1)
      {
        mOutput += (char) c;
      }
    }
    catch (IOException iOException)
    {
      throw iOException;
    }
    finally
    {
      if (null != stdout)
      {
        stdout.close();
      }
    }

    exitCcode = exec.waitFor();
    return exitCcode;
  }

  /**
   * @return the Output
   */
  public String getOutput()
  {
    return mOutput;
  }
}
