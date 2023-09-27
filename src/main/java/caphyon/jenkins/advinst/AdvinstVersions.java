package caphyon.jenkins.advinst;

import java.io.IOException;
import java.net.URL;
import org.ini4j.Profile.Section;

import hudson.util.VersionNumber;

import org.ini4j.Wini;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdvinstVersions {

  private List<Section> mVersions;

  public AdvinstVersions() {
    mVersions = getAllowedReleaseInfo();
  }

  public String getMinimumAllowedVersion() {
    if (mVersions.isEmpty())
      return "";
    return mVersions.get(mVersions.size() - 1).get("ProductVersion");
  }

  public boolean isDeprecated(String version) {
    final VersionNumber minAllowedVer = new VersionNumber(getMinimumAllowedVersion());
    final VersionNumber crtVer = new VersionNumber(version);
    return minAllowedVer.isNewerThan(crtVer);
  }

  private List<Section> getAllowedReleaseInfo() {
    try {
      final LocalDate minReleaseDate = LocalDate.now().minusMonths(AdvinstConsts.ValidReleaseIntervalMonths);
      final URL updatesIniUrl = new URL("https://www.advancedinstaller.com/downloads/updates.ini");
      Wini updatesIni = new Wini(updatesIniUrl);
      return updatesIni.values().stream().filter(s -> {
        LocalDate rd = LocalDate.parse(s.get("ReleaseDate"), DateTimeFormatter.ofPattern("dd/M/yyyy"));
        return minReleaseDate.isBefore(rd) || minReleaseDate.isEqual(rd);
      }).collect(Collectors.toList());
    } catch (IOException e) {
      return new ArrayList<Section>();
    }
  }
}
