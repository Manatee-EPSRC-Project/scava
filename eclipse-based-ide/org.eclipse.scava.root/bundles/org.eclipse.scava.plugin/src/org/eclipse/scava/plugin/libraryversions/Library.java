/*********************************************************************
* Copyright c 2017 FrontEndART Software Ltd.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse PublicLicense 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

package org.eclipse.scava.plugin.libraryversions;

import java.util.Date;

public class Library {
	private final String groupId;
	private final String artifactId;
	private final String version;
	private Date releaseDate;

	public Library(String groupId, String artifactId, String version) {
		this(groupId, artifactId, version, new Date(0));
	}

	public Library(String groupId, String artifactId, String version, Date releaseDate) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.releaseDate = releaseDate;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	@Override
	public String toString() {
		return "Library [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + ", releaseDate="
				+ releaseDate + "]";
	}

	public String toMavenCoord() {
		return groupId + ":" + artifactId + ":" + version;
	}

	public String toMavenCoordWithoutVersion() {
		return groupId + ":" + artifactId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Library other = (Library) obj;
		if (artifactId == null) {
			if (other.artifactId != null)
				return false;
		} else if (!artifactId.equals(other.artifactId))
			return false;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

}
