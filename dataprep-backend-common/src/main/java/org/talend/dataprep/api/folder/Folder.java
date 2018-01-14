// ============================================================================
//
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.api.folder;

import java.io.Serializable;
import java.util.Objects;

import org.talend.dataprep.api.share.SharedResource;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model a folder.
 */
public class Folder implements Serializable, SharedResource {

    /** Serialization UID. */
    private static final long serialVersionUID = 1L;

    /** Id needed subclass implementation. */
    private String id;

    /** Folder path as "/foo/bar/beer". */
    @JsonProperty("path")
    private String path;

    /** The folder name (e.g. /marketing/q1 => q1). */
    private String name;

    /** The folder owner id. */
    private String ownerId;

    /** Id of the parent. */
    private String parentId;

    /** The folder creation date. */
    private long creationDate;

    /** The folder last modification date. */
    private long lastModificationDate;

    /** Number of preparations held in this folder. */
    private long nbPreparations;



    /**
     * Default empty constructor.
     */
    public Folder() {
        this.creationDate = System.currentTimeMillis();
        this.lastModificationDate = this.creationDate;
    }

    /**
     * @return the Path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the Name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set.
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * @return the CreationDate
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate the creationDate to set.
     */
    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @return the LastModificationDate
     */
    public long getLastModificationDate() {
        return lastModificationDate;
    }

    /**
     * @param lastModificationDate the lastModificationDate to set.
     */
    public void setLastModificationDate(long lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    /**
     * @return the NbPreparations
     */
    public long getNbPreparations() {
        return nbPreparations;
    }

    /**
     * @param nbPreparations the nbPreparations to set.
     */
    public void setNbPreparations(long nbPreparations) {
        this.nbPreparations = nbPreparations;
    }

    /**
     * @return the Id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the ParentId
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set.
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }



    /**
     * @return the OwnerId
     */
    @Override
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * @param ownerId the ownerId to set.
     */
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }



    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Folder folder = (Folder) o;
        return creationDate == folder.creationDate // NOSONAR generated code
                && lastModificationDate == folder.lastModificationDate //
                && nbPreparations == folder.nbPreparations //
                && Objects.equals(path, folder.path) //
                && Objects.equals(id, folder.id) //
                && Objects.equals(parentId, folder.parentId) //
                && Objects.equals(name, folder.name) //
                && Objects.equals(ownerId, folder.ownerId);
    }

    /**
     * @see Object#hashCode
     */
    @Override
    public int hashCode() {
        return Objects.hash(path, id, name, ownerId, creationDate, lastModificationDate, nbPreparations);
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "Folder{" + //
                "path='" + path + '\'' + //
                ", id='" + id + '\'' + //
                ", name='" + name + '\'' + //
                ", ownerId=" + ownerId + //
                ", parentId=" + parentId + //
                ", nbPreparations=" + nbPreparations + //
                ", creationDate=" + creationDate + //
                ", lastModificationDate=" + lastModificationDate + //
                '}';
    }
}
