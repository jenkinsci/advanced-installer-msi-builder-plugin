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
public class AdvinstConsts
{

  //--------------------------------------------------------------------------
  public static final String AdvinstExeApp = "advinst.exe";
  public static final String AdvinstComApp = "AdvancedInstaller.com";

  public static final String AdvinstToolsSubfolder = "bin" + File.separator + "x86";
  public static final String AdvinstAicHeader = ";aic";
  public static final String AdvinstBuildAll = "All";

  //--------------------------------------------------------------------------
  //Acessors used by AdvinstData
  public static final String AipBuild = "aipProjectBuild";
  public static final String AipPath = "aipProjectPath";
  public static final String OutputFolder = "aipProjectOutputFolder";
  public static final String OutputName = "aipProjectOutputName";
  public static final String NoDigSig = "aipProjectNoDigitalSignature";

  //--------------------------------------------------------------------------
  //Advinst commands
  public static final String AdvinstCommandListBuilds = "ListBuilds";
  public static final String AdvinstCommand = "ResetSig";
}
