package org.talend.dataprep.services.folder;

import static org.springframework.web.bind.annotation.RequestMethod.*;

import java.util.stream.Stream;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.talend.daikon.annotation.Service;
import org.talend.dataprep.api.folder.Folder;
import org.talend.dataprep.api.folder.FolderInfo;
import org.talend.dataprep.api.folder.FolderTreeNode;
import org.talend.dataprep.api.folder.UserFolder;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.util.SortAndOrderHelper.Order;
import org.talend.dataprep.util.SortAndOrderHelper.Sort;

@Service(name = "dataprep.folders")
public interface IFolderService {

    /**
     * Get folders. If parentId is supplied, it will be used as filter.
     *
     * @param parentId the parent folder id parameter
     * @return direct sub folders for the given id.
     */
    //@formatter:off
    @RequestMapping(value = "/folders", method = GET)
    @Timed
    Stream<UserFolder> list(@RequestParam(required = false) String parentId, //
                        @RequestParam(defaultValue = "lastModificationDate") Sort sort, //
                        @RequestParam(defaultValue = "desc") Order order);

    /**
     * Get a folder metadata with its hierarchy
     *
     * @param id the folder id.
     * @return the folder metadata with its hierarchy.
     */
    @RequestMapping(value = "/folders/{id}", method = GET)
    @Timed
    FolderInfo getFolderAndHierarchyById(@PathVariable(value = "id") String id);

    /**
     * Search for folders.
     *
     * @param name the folder name to search.
     * @param strict strict mode means the name is the full name.
     * @return the folders whose part of their name match the given path.
     */
    @RequestMapping(value = "/folders/search", method = GET)
    @Timed
    Stream<UserFolder> search(@RequestParam(required = false, defaultValue = "") String name, //
                          @RequestParam(required = false, defaultValue = "false") Boolean strict, //
                          @RequestParam(required = false) String path);

    /**
     * Add a folder.
     *
     * @param parentId where to add the folder.
     * @return the created folder.
     */
    @RequestMapping(value = "/folders", method = PUT)
    @Timed
    Folder addFolder(@RequestParam(required = false) String parentId, @RequestParam String path);

    /**
     * Remove the folder. Throws an exception if the folder, or one of its sub folders, contains an entry.
     *
     * @param id the id that points to the folder to remove.
     */
    @RequestMapping(value = "/folders/{id}", method = DELETE)
    @Timed
    void removeFolder(@PathVariable String id);

    /**
     * Rename the folder to the new id.
     *
     * @param id where to look for the folder.
     * @param newName the new folder id.
     */
    @RequestMapping(value = "/folders/{id}/name", method = PUT)
    @Timed
    void renameFolder(@PathVariable String id, @RequestBody String newName);

    @RequestMapping(value = "/folders/tree", method = GET)
    @Timed
    FolderTreeNode getTree();
}
