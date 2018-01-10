package org.talend.dataprep.preparation.service;

import static org.springframework.web.bind.annotation.RequestMethod.*;

import java.util.stream.Stream;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.talend.dataprep.api.folder.Folder;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.util.SortAndOrderHelper;

public interface IFolderService {
    /**
     * Get folders. If parentId is supplied, it will be used as filter.
     *
     * @param parentId the parent folder id parameter
     * @return direct sub folders for the given id.
     */
    //@formatter:off
    @RequestMapping(value = "/folders", method = GET)
    @ApiOperation(value = "List children folders of the parameter if null list root children.", notes = "List all child folders of the one as parameter")
    @Timed
    Stream<Folder> list(@RequestParam(required = false) @ApiParam(value = "Parent id filter.") String parentId,
                        @RequestParam(defaultValue = "lastModificationDate") @ApiParam(value = "Sort key (by name or date).") SortAndOrderHelper.Sort sort,
                        @RequestParam(defaultValue = "desc") @ApiParam(value = "Order for sort key (desc or asc).") SortAndOrderHelper.Order order);

    /**
     * Get a folder metadata with its hierarchy
     *
     * @param id the folder id.
     * @return the folder metadata with its hierarchy.
     */
    @RequestMapping(value = "/folders/{id}", method = GET)
    @ApiOperation(value = "Get folder by id", notes = "GET a folder by id")
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
    @ApiOperation(value = "Search Folders with parameter as part of the name")
    @Timed
    Stream<Folder> search(@RequestParam(required = false, defaultValue = "") String name,
                          @RequestParam(required = false, defaultValue = "false") Boolean strict,
                          @RequestParam(required = false) String path);

    /**
     * Add a folder.
     *
     * @param parentId where to add the folder.
     * @return the created folder.
     */
    @RequestMapping(value = "/folders", method = PUT)
    @ApiOperation(value = "Create a Folder", notes = "Create a folder")
    @Timed
    StreamingResponseBody addFolder(@RequestParam(required = false) String parentId, @RequestParam String path);

    /**
     * Remove the folder. Throws an exception if the folder, or one of its sub folders, contains an entry.
     *
     * @param id the id that points to the folder to remove.
     */
    @RequestMapping(value = "/folders/{id}", method = DELETE)
    @ApiOperation(value = "Remove a Folder", notes = "Remove the folder")
    @Timed
    void removeFolder(@PathVariable String id);

    /**
     * Rename the folder to the new id.
     *
     * @param id where to look for the folder.
     * @param newName the new folder id.
     */
    @RequestMapping(value = "/folders/{id}/name", method = PUT)
    @ApiOperation(value = "Rename a Folder")
    @Timed
    void renameFolder(@PathVariable String id, @RequestBody String newName);

    @RequestMapping(value = "/folders/tree", method = GET)
    @ApiOperation(value = "List all folders")
    @Timed
    FolderTreeNode getTree();
}
