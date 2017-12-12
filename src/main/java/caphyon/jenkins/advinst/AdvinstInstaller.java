package caphyon.jenkins.advinst;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;


public final class AdvinstInstaller extends DownloadFromUrlInstaller
{

  @DataBoundConstructor
  public AdvinstInstaller(String id) {
    super(id);
  }

  @Extension
  public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<AdvinstInstaller> {
      public String getDisplayName() {
          return "Install from AdvancedInstaller.com";
      }

      @Override
      public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
          return toolType == AdvinstInstallation.class;
      }
  }
}
