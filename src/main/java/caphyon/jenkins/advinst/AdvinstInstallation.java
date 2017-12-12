package caphyon.jenkins.advinst;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.security.MasterToSlaveCallable;

public final class AdvinstInstallation extends ToolInstallation
    implements EnvironmentSpecific<AdvinstInstallation>, NodeSpecific<AdvinstInstallation>, Serializable {

  private final String advinstHome;
  private static final String advinstSubPath = "bin\\x86\\AdvancedInstaller.com";

  @DataBoundConstructor
  public AdvinstInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
    super(name, home, properties);
    this.advinstHome = getHome();
  }

  @Override
  public String getHome() {
    if (advinstHome != null) {
      return advinstHome;
    }
    return super.getHome();
  }

  @Override
  public String getName()
  {
    return super.getName();
  }

  public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
    return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
      public String call() throws IOException {
        File exe = getExeFile();
        if (exe.exists()) {
          return exe.getPath();
        }
        return null;
      }
    });
  }

  private File getExeFile() {
    String home = Util.replaceMacro(advinstHome, EnvVars.masterEnvVars);
    return new File(home, advinstSubPath);
  }

  @Override
  public AdvinstInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
    return new AdvinstInstallation(getName(), translateFor(node, log), getProperties().toList());
  }

  @Override
  public AdvinstInstallation forEnvironment(EnvVars environment) {
    return new AdvinstInstallation(getName(), environment.expand(getHome()), getProperties().toList());
  }

  @Extension
  public static class DescriptorImpl extends ToolDescriptor<AdvinstInstallation> {

    private static final ResourceBundle mMessagesBundle = ResourceBundle.getBundle("Messages");

    @Inject
    private AdvinstDescriptorImpl mAdvinstDescriptor;

    @Override
    public String getDisplayName() {
      return mMessagesBundle.getString("ADVINST");
    }

    @Override
    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new AdvinstInstaller(null));
    }

    @Override
    public AdvinstInstallation[] getInstallations() {
      return mAdvinstDescriptor.getInstallations();
    }

    @Override
    public void setInstallations(AdvinstInstallation... installations) {
      mAdvinstDescriptor.setInstallations(installations);
    }
  }
}
