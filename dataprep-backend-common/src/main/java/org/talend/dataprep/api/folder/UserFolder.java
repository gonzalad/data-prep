package org.talend.dataprep.api.folder;

import java.util.HashSet;
import java.util.Set;

import org.talend.dataprep.api.share.Owner;
import org.talend.dataprep.api.share.SharedResource;

public class UserFolder extends Folder implements SharedResource {

    /** This folder owner. */
    private Owner owner;

    /** True if this folder is shared by another user. */
    private boolean sharedFolder = false;

    /** True if this folder is shared by current user. */
    private boolean sharedByMe = false;

    /** What role has the current user on this folder. */
    private Set<String> roles = new HashSet<>();

    /**
     * @return the Owner
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set.
     */
    @Override
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * @return the SharedFolder
     */
    public boolean isSharedFolder() {
        return sharedFolder;
    }

    /**
     * @param sharedFolder the sharedFolder to set.
     */
    public void setSharedFolder(boolean sharedFolder) {
        this.sharedFolder = sharedFolder;
    }

    @Override
    public void setSharedResource(boolean shared) {
        this.setSharedFolder(shared);
    }

    /**
     * @return sharedByMe
     */
    public boolean isSharedByMe() {
        return sharedByMe;
    }

    @Override
    public void setSharedByMe(boolean sharedByMe) {
        this.sharedByMe = sharedByMe;
    }

    /**
     * @return the Roles
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * @param roles the roles to set.
     */
    @Override
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
