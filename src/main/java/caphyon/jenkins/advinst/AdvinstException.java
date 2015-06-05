/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caphyon.jenkins.advinst;

/**
 * Advanced Installer high level exception
 *
 * @author Ciprian Burca
 */
public class AdvinstException extends Exception
{

  public AdvinstException(String message, Throwable t)
  {
    super(message, t);
  }

  public AdvinstException(String message)
  {
    super(message);
  }

  public AdvinstException(Throwable t)
  {
    super(t);
  }
}
