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
public class AdvinstException extends Exception {

  private static final long serialVersionUID = -6308290342414398576L;

  public AdvinstException(final String message, final Throwable t) {
    super(message, t);
  }

  public AdvinstException(final String message) {
    super(message);
  }

  public AdvinstException(final Throwable t) {
    super(t);
  }
}
