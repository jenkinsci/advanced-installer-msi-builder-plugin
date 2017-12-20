package caphyon.jenkins.advinst;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.Util;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
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
  private final String mAdvinstVersion;
  private final Secret mAdvinstLicense;

  @DataBoundConstructor
  public AdvinstInstaller(String label, String advinstVersion, Secret advinstLicense) {
    super(label);
    this.mAdvinstVersion = Util.fixEmptyAndTrim(advinstVersion);
    this.mAdvinstLicense = advinstLicense;
  }

  public String getAdvinstVersion() {
    return mAdvinstVersion;
  }

  public Secret getAdvinstLicense() {
    return mAdvinstLicense;
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
    if (isUpToDate(advinstRootPath, node))
      return advinstRootPath;

    final String downloadUrl = String.format(kAdvinstUrlTemplate, this.mAdvinstVersion);
    final String message = Messages.MSG_ADVINST_INSTALL(downloadUrl, advinstRootPath, node.getDisplayName());
    listener.getLogger().append(message);

    try (FilePathAutoDeleter advistRootPathDeleter = new FilePathAutoDeleter(advinstRootPath)) {
      //Download the advinst.msi in the working dir temp folder.
      try (FilePathAutoDeleter tempDownloadDir = new FilePathAutoDeleter(
          node.getRootPath().createTempDir("tmpAdvinstDld", null))) {

        FilePath tempDownloadFile = tempDownloadDir.GetFilePath().child("advinst.msi");

        if (!downloadFile(downloadUrl, tempDownloadFile, listener)) {
          throw new InstallationFailedException(Messages.ERR_ADVINST_DOWNLOAD_FAILED(downloadUrl, tempDownloadFile));
        }

        if (!extractMSI(tempDownloadFile, advinstRootPath, node, listener)) {
          throw new InstallationFailedException(Messages.ERR_ADVINST_EXTRACT_FAILED(downloadUrl, advinstRootPath));
        }

        FilePath advinstComPath = advinstRootPath.child(AdvinstInstallation.advinstComSubPath);
        if (!registerAdvinst(advinstComPath, mAdvinstLicense, node, listener)) {
          throw new InstallationFailedException(Messages.ERR_ADVINST_REGISTER_FAILED());
        }
      }

      advistRootPathDeleter.Release();
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
    args.add("msiexec.exe", "/a", msiPath.getRemote(), "TARGETDIR=" + targetDir.getRemote(), "/qn");
    ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args).stdout(listener);
    Proc proc = launcher.launch(ps);
    int retcode = proc.join();
    return retcode == 0;
  }

  private boolean registerAdvinst(final FilePath advinstPath, final Secret licenseID, Node node, TaskListener listener)
      throws IOException, InterruptedException {

    if (null == licenseID)
      return true;

    Launcher launcher = node.createLauncher(listener);
    ArgumentListBuilder args = new ArgumentListBuilder();
    args.add(advinstPath.getRemote(), "/register", licenseID.getPlainText());
    ProcStarter ps = launcher.new ProcStarter();
    ps = ps.cmds(args);
    ps = ps.masks(false, false, true);
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

  // Extend IOException so we can throw and stop the build if installation fails
  static class InstallationFailedException extends IOException {
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
