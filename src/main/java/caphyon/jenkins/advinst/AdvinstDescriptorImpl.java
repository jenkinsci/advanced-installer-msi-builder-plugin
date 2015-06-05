package caphyon.jenkins.advinst;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Descriptor for {@link AdvinstBuilder}. Used as a singleton. The class is
 * marked as public so that it can be accessed from views.
 *
 * <p>
 * See
 * <tt>src/main/resources/caphyon/jenkins/AdvinstBuilder/*.jelly</tt>
 * for the actual HTML fragment for the configuration screen.
 *
 * @author Ciprian Burca
 */
@Extension // This indicates to Jenkins that this is an implementation of an extension point.
public final class AdvinstDescriptorImpl extends BuildStepDescriptor<Builder>
{

  private static final ResourceBundle mMessagesBundle = ResourceBundle.getBundle("Messages");
  //This logger writes to the Jenkins system log.
  private static final Logger LOGGER = Logger.getLogger("jenkins.advinstbuilder");

  private String mAdvinstRootPath = "";

  public AdvinstDescriptorImpl()
  {
    super(AdvinstBuilder.class);
    load();
  }

  public FormValidation doCheckAdvinstInstPath(@QueryParameter String value) throws IOException, ServletException
  {
    if (value == null || value.length() == 0)
    {
      return FormValidation.error(mMessagesBundle.getString("ERR_REQUIRED"));
    }

    return FormValidation.ok();
  }

  public FormValidation doCheckAipProjectPath(@QueryParameter String value) throws IOException, ServletException
  {
    if (value == null || value.length() == 0)
    {
      return FormValidation.error(mMessagesBundle.getString("ERR_REQUIRED"));
    }

    return FormValidation.ok();
  }

  public FormValidation doCheckAipProjectOutputFolder(@QueryParameter String value, @QueryParameter String aipProjectBuild) throws IOException, ServletException
  {
    if (value != null && !value.isEmpty())
    {
      if (aipProjectBuild == null || aipProjectBuild.length() == 0)
      {
        return FormValidation.error(mMessagesBundle.getString("ERR_BUILD_NAME_REQUIRED"));
      }
    }
    return FormValidation.ok();
  }

  public FormValidation doCheckAipProjectOutputName(@QueryParameter String value, @QueryParameter String aipProjectBuild) throws IOException, ServletException
  {
    if (value != null && !value.isEmpty())
    {
      if (aipProjectBuild == null || aipProjectBuild.length() == 0)
      {
        return FormValidation.error(mMessagesBundle.getString("ERR_BUILD_NAME_REQUIRED"));
      }
    }
    return FormValidation.ok();
  }

  /**
   * @param aClass
   * @return
   */
  @Override
  public boolean isApplicable(Class<? extends AbstractProject> aClass)
  {
    // Indicates that this builder can be used with all kinds of project types
    return true;
  }

  /**
   * @return Human readable name is used in the configuration screen.
   */
  @Override
  public String getDisplayName()
  {
    return mMessagesBundle.getString("ADVINST");
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException
  {
    try
    {
      this.mAdvinstRootPath = formData.getString("advinstRootPath");
    }
    catch (Exception e)
    {
      LOGGER.log(Level.SEVERE, e.getMessage());
    }
    finally
    {
      save();
    }
    return super.configure(req, formData);
  }

  /**
   * @return the mAdvinstRootPath
   */
  public String getAdvinstRootPath()
  {
    return Paths.get(this.mAdvinstRootPath).toString();
  }
}
