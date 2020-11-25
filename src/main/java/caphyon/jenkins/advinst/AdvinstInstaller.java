package caphyon.jenkins.advinst;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import com.sun.jna.platform.win32.VerRsrc.VS_FIXEDFILEINFO;
import com.sun.jna.platform.win32.VersionUtil;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.VersionNumber;
import jenkins.security.MasterToSlaveCallable;

public final class AdvinstInstaller extends ToolInstaller {

  private static final String kAdvinstUrlTemplate = "https://www.advancedinstaller.com/downloads/%s/advinst.msi";
  private static final VersionNumber kMinimumWindowsOsVersion = new VersionNumber("6.1"); //Windows 7
  private static final VersionNumber kAdvinstRegVersionSwitch = new VersionNumber("14.6");
  private static final String kAdvinstURLEnvVar = "advancedinstaller.url";
  private final String mAdvinstVersion;
  private final Secret mAdvinstLicense;
  private final boolean mEnablePowerShell;

  @DataBoundConstructor
  public AdvinstInstaller(String label, String advinstVersion, Secret advinstLicense, boolean advinstEnablePowerShell) {
    super(label);
    this.mAdvinstVersion = Util.fixEmptyAndTrim(advinstVersion);
    this.mAdvinstLicense = advinstLicense;
    this.mEnablePowerShell = advinstEnablePowerShell;
  }

  public String getAdvinstVersion() {
    return mAdvinstVersion;
  }

  public Secret getAdvinstLicense() {
    return mAdvinstLicense;
  }

  public boolean getAdvinstEnablePowerShell()
  {
    return mEnablePowerShell;
  }

  @Override
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener listener)
      throws IOException, InterruptedException {

    // Gather properties for the node to install on
    VirtualChannel channel = node.getChannel();
    if (null == channel)
      throw new InstallationFailedException(Messages.ERR_ADVINST_INSTALL_FAILED());
    String[] properties = channel.call(new GetSystemProperties("os.name", "os.version"));

    //Verify the targe os is Windows.
    if (!properties[0].toLowerCase().contains("windows")) {
      throw new InstallationFailedException(Messages.ERR_ADVINST_UNSUPPORTED_OS());
    }

    //Verify the target OS version is higher that 6.1
    if (kMinimumWindowsOsVersion.compareTo(new VersionNumber(properties[1])) > 0) {
      throw new InstallationFailedException(Messages.ERR_ADVINST_UNSUPPORTED_OS_VERSION());
    }

    final FilePath advinstRootPath = preferredLocation(tool, node);
    if (!isUpToDate(advinstRootPath, node))
    {
      final String downloadUrl = getAdvinstDownloadUrl(node);
      final String message = Messages.MSG_ADVINST_INSTALL(downloadUrl, advinstRootPath, node.getDisplayName());
      listener.getLogger().append(message);

      try (FilePathAutoDeleter advistRootPathDeleter = new FilePathAutoDeleter(advinstRootPath)) {
        // Download the advinst.msi in the working dir temp folder.
        try (FilePathAutoDeleter tempDownloadDir = new FilePathAutoDeleter(
            node.getRootPath().createTempDir("tmpAdvinstDld", null))) {

          FilePath tempDownloadFile = tempDownloadDir.GetFilePath().child("advinst.msi");

          if (!downloadFile(downloadUrl, tempDownloadFile, listener)) {
            throw new InstallationFailedException(Messages.ERR_ADVINST_DOWNLOAD_FAILED(downloadUrl, tempDownloadFile));
          }

          if (!extractMSI(tempDownloadFile, advinstRootPath, node, listener)) {
            throw new InstallationFailedException(Messages.ERR_ADVINST_EXTRACT_FAILED(downloadUrl, advinstRootPath));
          }
        }
        advistRootPathDeleter.Release();
      }
    }

    FilePath advinstComPath = advinstRootPath.child(AdvinstInstallation.advinstComSubPath);
    if (advinstComPath.exists())
    {
      if (!registerAdvinst(advinstComPath, mAdvinstLicense, node, listener)) {
        throw new InstallationFailedException(Messages.ERR_ADVINST_REGISTER_FAILED());
      }

      if (!enablePowerShell(advinstComPath, mEnablePowerShell, node, listener)) {
        throw new InstallationFailedException(Messages.ERR_ADVINST_REGISTER_COM_FAILED());
      }

    }
    return advinstRootPath;
  }

  private boolean isUpToDate(final FilePath expectedRoot, Node node) throws IOException, InterruptedException {

    FilePath advinstComPath = expectedRoot.child(AdvinstInstallation.advinstComSubPath);
    //Check if the advinst executable exists.
    if (!advinstComPath.exists())
      return false;

    return true;
  }

  private boolean downloadFile(String fileURL, FilePath targetFile, TaskListener listener) throws IOException {
    InputStream is = null;
    OutputStream os = null;

    try {
      final URL url = new URL(fileURL);
      final URLConnection conn = url.openConnection();

      conn.setUseCaches(false);
      is = conn.getInputStream();
      listener.getLogger().append(Messages.MSG_ADVINST_DOWNLOAD_PROGRESS(fileURL, targetFile.getRemote()));
      os = targetFile.write();
      final byte[] buf = new byte[8192];
      int i = 0;
      while ((i = is.read(buf)) != -1) {
        os.write(buf, 0, i);
      }
    } catch (final Exception e) {
      listener.error(Messages.ERR_ADVINST_DOWNLOAD_FAILED(fileURL, e.getMessage()));
      return false;
    } finally {
      if (is != null) {
        is.close();
      }
      if (os != null) {
        os.close();
      }
    }
    return true;
  }

  private boolean extractMSI(FilePath msiPath, FilePath targetDir, Node node, TaskListener listener)
      throws IOException, InterruptedException {

    Launcher launcher = node.createLauncher(listener);
    ArgumentListBuilder args = new ArgumentListBuilder();

    args.add("cmd.exe");
    args.add("/c");
    args.addQuoted(
      String.format("msiexec /a \"%s\" TARGETDIR=\"%s\" /qn", msiPath.getRemote(), targetDir.getRemote()));

    ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args).stdout(listener);
    ps = ps.masks(null);
    Proc proc = launcher.launch(ps);
    int retcode = proc.join();
    return retcode == 0;
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private boolean registerAdvinst(final FilePath advinstPath, final Secret licenseID, Node node, TaskListener listener)
      throws IOException, InterruptedException {

    String plainLicenseId = Secret.toString(licenseID);
    if (plainLicenseId.isEmpty())
      return true;

    final FilePath advinstExe = advinstPath.sibling("advinst.exe");
    final VersionNumber advinstVersion = new VersionNumber(
        node.getChannel().call(new GetWin32FileVersion(advinstExe.getRemote())));

    String registerCommand = "/RegisterCI";
    if (advinstVersion.isOlderThan(kAdvinstRegVersionSwitch)) {
      registerCommand = "/Register";
    }

    Launcher launcher = node.createLauncher(listener);
    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(advinstPath.getRemote(), registerCommand, plainLicenseId);
    ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args);
    ps = ps.masks(false, false, true);
    ps = ps.stdout(listener);
    Proc proc = launcher.launch(ps);
    int retcode = proc.join();
    return retcode == 0;
  }

  private String getAdvinstDownloadUrl(Node node) {
    String downloadUrl;

    EnvVars envVars = new EnvVars();
    EnvironmentVariablesNodeProperty env = node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class);
    if (env != null) {
      envVars.putAll(env.getEnvVars());
    }
    if (envVars.containsKey(kAdvinstURLEnvVar)) {
      downloadUrl = envVars.get(kAdvinstURLEnvVar);
    } else {
      downloadUrl = String.format(kAdvinstUrlTemplate, this.mAdvinstVersion);
    }

    return downloadUrl;
  }

  private boolean enablePowerShell(final FilePath advinstPath, boolean enablePowerShell, Node node, TaskListener listener) 
      throws IOException, InterruptedException {

    if (!enablePowerShell)
      return true;
      
    String registerCommand = "/REGSERVER";
    Launcher launcher = node.createLauncher(listener);
    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(advinstPath.getRemote(), registerCommand);
    ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args);
    ps = ps.stdout(listener);
    Proc proc = launcher.launch(ps);
    int retcode = proc.join();
    return retcode == 0;
  }

  @Extension
  public static class DescriptorImpl extends ToolInstallerDescriptor<AdvinstInstaller> {

    @Override
    public String getDisplayName() {
      return Messages.MSG_ADVINST_INSTALL_FROM_WEBSITE();
    }

    @Override
    public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
      return toolType == AdvinstInstallation.class;
    }

    public FormValidation doCheckAdvinstVersion(@QueryParameter String value) throws IOException, ServletException {
      if (value == null || value.length() == 0) {
        return FormValidation.error(Messages.ERR_REQUIRED());
      }

      return FormValidation.ok();
    }
  }

  /** Returns the values of the given Java system properties. */
  private static class GetSystemProperties extends MasterToSlaveCallable<String[], InterruptedException> {
    private static final long serialVersionUID = 1L;

    private final String[] properties;

    GetSystemProperties(String... properties) {
      this.properties = properties;
    }

    public String[] call() {
      String[] values = new String[properties.length];
      for (int i = 0; i < properties.length; i++) {
        values[i] = System.getProperty(properties[i]);
      }
      return values;
    }
  }

  private static class GetWin32FileVersion extends MasterToSlaveCallable<String, InterruptedException> {
    private static final long serialVersionUID = 1L;
    private final String filePath;

    GetWin32FileVersion(String filePath) {
      this.filePath = filePath;
    }

    public String call() {
      VS_FIXEDFILEINFO verInfo = VersionUtil.getFileVersionInfo(this.filePath);
      final String verString = String.format("%d.%d.%d.%d", verInfo.getProductVersionMajor(),
          verInfo.getProductVersionMinor(), verInfo.getProductVersionRevision(),
          verInfo.getProductVersionBuild());
      return verString;
    }
  }

  // Extend IOException so we can throw and stop the build if installation fails
  static class InstallationFailedException extends IOException {

    private static final long serialVersionUID = -1714895928033107556L;

    InstallationFailedException(String message) {
      super(message);
    }
  }

  static class FilePathAutoDeleter implements AutoCloseable {
    private FilePath mFilePath;

    public FilePath GetFilePath() {
      return mFilePath;
    }

    public void Release() {
      mFilePath = null;
    }

    public FilePathAutoDeleter(FilePath filePath) {
      this.mFilePath = filePath;
    }

    @Override
    public void close() throws IOException, InterruptedException {

      if (null == mFilePath || !mFilePath.exists())
        return;

      if (mFilePath.isDirectory())
        mFilePath.deleteRecursive();
      else
        mFilePath.delete();
    }

  }

}
