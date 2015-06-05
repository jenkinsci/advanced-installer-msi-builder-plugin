/*
 * The MIT License
 *
 * Copyright 2015 Ciprian Burca.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package caphyon.jenkins.advinst;

import org.apache.commons.lang.StringUtils;
import java.util.Properties;

public class AdvinstParameters
{

  private final Properties mProperties;

  public AdvinstParameters()
  {
    this(new Properties());
  }

  public AdvinstParameters(Properties mProperties)
  {
    this.mProperties = mProperties;
  }

  public String get(String key, String defaultValue)
  {
    String tmp = this.mProperties.getProperty(key);
    return (StringUtils.isEmpty(tmp)) ? defaultValue : tmp;
  }

  public boolean get(String key, boolean defaultValue)
  {
    boolean rvalue = defaultValue;
    String tmp = this.mProperties.getProperty(key);
    if (tmp != null)
    {
      try
      {
        rvalue = Boolean.parseBoolean(tmp);
      }
      catch (Exception e)
      {
        // nothing to do
      }
    }
    return rvalue;
  }

  public void set(String key, String value)
  {
    this.mProperties.setProperty(key, value);
  }

  public void set(String key, boolean value)
  {
    set(key, String.valueOf(value));
  }
}
