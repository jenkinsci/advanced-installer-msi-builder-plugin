package caphyon.jenkins.advinst;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

/**
 * Descriptor for {@link AdvinstBuilder}. Used as a singleton. The class is
 * marked as public so that it can be accessed from views.
 *
 * <p>
 * See <tt>src/main/resources/caphyon/jenkins/AdvinstBuilder/*.jelly</tt> for
 * the actual HTML fragment for the configuration screen.
 *
 * @author Ciprian Burca
 */
@Extension // This indicates to Jenkins that this is an implementation of an extension
           // point.
@Symbol("advinstBuilder")
public final class AdvinstDescriptorImpl extends BuildStepDescriptor<Builder> {

  @CopyOnWrite
  private volatile AdvinstInstallation[] installations = new AdvinstInstallation[0];

  public AdvinstDescriptorImpl() {
    super(AdvinstBuilder.class);
    load();
  }

  public ListBoxModel doFillInstallNameItems() {
    ListBoxModel items = new ListBoxModel();
    for (AdvinstInstallation inst : getInstallations()) {
      items.add(new ListBoxModel.Option(inst.getName()));
    }
    return items;
  }

  public FormValidation doCheckAipProjectPath(final @QueryParameter String value) throws IOException, ServletException {
    if (value == null || value.length() == 0) {
      return FormValidation.error(Messages.ERR_REQUIRED());
    }

    return FormValidation.ok();
  }

  public FormValidation doCheckAipProjectOutputFolder(final @QueryParameter String value,
      final @QueryParameter String aipProjectBuild) throws IOException, ServletException {
    if (value != null && !value.isEmpty()) {
      if (aipProjectBuild == null || aipProjectBuild.length() == 0) {
        return FormValidation.error(Messages.ERR_BUILD_NAME_REQUIRED());
      }
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckAipProjectOutputName(final @QueryParameter String value,
      final @QueryParameter String aipProjectBuild) throws IOException, ServletException {
    if (value != null && !value.isEmpty()) {
      if (aipProjectBuild == null || aipProjectBuild.length() == 0) {
        return FormValidation.error(Messages.ERR_BUILD_NAME_REQUIRED());
      }
    }
    return FormValidation.ok();
  }

  @Override
  public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
    // Indicates that this builder can be used with all kinds of project types
    return true;
  }

  /**
   * @return Human readable name is used in the configuration screen.
   */
  @Override
  public String getDisplayName() {
    return Messages.ADVINST_INVOKE();
  }

  public boolean configure(final StaplerRequest req, final JSONObject formData) throws Descriptor.FormException {
    save();
    return super.configure(req, formData);
  }

  public AdvinstInstallation.DescriptorImpl getToolDescriptor() {
    return ToolInstallation.all().get(AdvinstInstallation.DescriptorImpl.class);
  }

  protected void convertfinal(final Map<String, Object> oldPropertyBag) {
    if (oldPropertyBag.containsKey("installations")) {
      installations = (AdvinstInstallation[]) oldPropertyBag.get("installations");
    }
  }

  public AdvinstInstallation[] getInstallations() {
    return Arrays.copyOf(installations, installations.length);
  }

  public void setInstallations(final AdvinstInstallation... installations) {
    this.installations = installations;
    save();
  }
}
