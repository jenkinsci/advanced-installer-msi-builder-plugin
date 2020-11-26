package caphyon.jenkins.advinst;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    implements EnvironmentSpecific<AdvinstInstallation>, NodeSpecific<AdvinstInstallation> {
  
  private static final long serialVersionUID = -6715383276188462597L;
  private final String advinstHome;
  public static final String advinstComSubPath = "bin\\x86\\AdvancedInstaller.com";

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

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
    return launcher.getChannel().call(new MasterToSlaveCallable<String, IOException>() {
      private static final long serialVersionUID = 8800376540325557778L;

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
    return new File(home, advinstComSubPath);
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

    
    @Inject
    private AdvinstDescriptorImpl mAdvinstDescriptor;

    @Override
    public String getDisplayName() {
      return Messages.ADVINST();
    }

    @Override
    public List<? extends ToolInstaller> getDefaultInstallers() {
      return Collections.singletonList(new AdvinstInstaller(null, null, null, false));
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
