/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caphyon.jenkins.advinst;

import java.io.File;

/**
 * Constants used by Advinst plugin.
 *
 * @author Ciprian
 */
final class AdvinstConsts {

  private AdvinstConsts() {
  }

  // --------------------------------------------------------------------------
  public static final String AdvinstExeApp = "advinst.exe";
  public static final String AdvinstComApp = "AdvancedInstaller.com";

  public static final String AdvinstToolsSubfolder = "bin" + File.separator + "x86";
  public static final String AdvinstComSubPath = AdvinstToolsSubfolder + File.separator + AdvinstComApp;
  public static final String AdvinstAicHeader = ";aic";
  public static final String AdvinstBuildAll = "All";

  // --------------------------------------------------------------------------
  // Advinst commands
  public static final String AdvinstCommandListBuilds = "ListBuilds";
  public static final String AdvinstCommandResetSig = "ResetSig";

  // --------------------------------------------------------------------------
  // Advinst parameters
  public static final String AdvinstParamAdvinstRootPath = "advinstRootPath";
  public static final String AdvinstParamAdvinstRunType = "advinstRunType";
  public static final String AdvinstParamAipBuild = "aipProjectBuild";
  public static final String AdvinstParamAipPath = "aipProjectPath";
  public static final String AdvinstParamAipOutputFolder = "aipProjectOutputFolder";
  public static final String AdvinstParamAipOutputName = "aipProjectOutputName";
  public static final String AdvinstParamAipNoDigSig = "aipProjectNoDigitalSignature";
  public static final String AdvinstParamExtraCommands = "advinstExtraCommands";
  public static final String AdvinstRunTypeDeploy = "deploy";
  public static final String AdvinstRunTypeBuild = "build";

  public static final int ValidReleaseIntervalMonths = 24;
}
