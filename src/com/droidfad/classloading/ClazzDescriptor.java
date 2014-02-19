/*
 * Created on 21.04.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.droidfad.classloading;

/**
 * 
 *
 * Copyright 2011 Jens Glufke jglufke@googlemail.com
 *
 *   Licensed under the DROIDFAD license (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.droidfad.com/html/license/license.htm
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * -----------------------------------------------------------------------<br>
 * class to describes a class that has been found by ClazzFinder
 */
public class ClazzDescriptor {
	private Class<?>  clazz        = null;
	private String    name         = null;
	private String    jarFile      = null;
	private String    absolutePath = null;
	private String    relativePath = null;

	/**
	 * create a ClassManagerEntry if the class is already loaded
	 * @param absolutePath
	 * @param clazz
	 * @param jarFile
	 */
	public ClazzDescriptor(String absolutePath, Class<?> clazz, String jarFile)	{

		this.name         = clazz.getName();
		this.clazz        = clazz;
		this.absolutePath = absolutePath;
		this.relativePath = clazz.getName().replaceAll("\\.", "/");
		this.jarFile      = jarFile;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ClazzDescriptor other = (ClazzDescriptor) obj;
		if (clazz == null)
		{
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
		return true;
	}

	/**
	 * get the absolute path of the class file
	 *
	 * @return
	 *
	 */
	public String getAbsolutePath() {
		return absolutePath;
	}

	/**
	 * get the found class itself
	 *
	 * @return
	 *
	 */
	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * get the jar file in which the found class is located
	 *
	 * @return
	 *
	 */
	public String getJarFile() {
		return jarFile;
	}

	/**
	 * get the class name
	 *
	 * @return
	 *
	 */
	public String getName() {
		return name;
	}

	/**
	 * get the relative path of loaded class
	 *
	 * @return
	 *
	 */
	public String getRelativePath() {
		return relativePath;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((clazz == null) ? 0 : clazz.hashCode());
		return result;
	}
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder lBuf = new StringBuilder();
		lBuf.append("ClassManagerEntry ,");
		lBuf.append(this.name);
		lBuf.append(",");
		lBuf.append(this.absolutePath);
		lBuf.append(",");
		lBuf.append(this.relativePath);
		lBuf.append(",");
		lBuf.append(this.jarFile);

		return lBuf.toString();
	}
}

