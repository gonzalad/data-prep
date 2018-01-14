// ============================================================================
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

package org.talend.dataprep.api.service;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import java.io.OutputStream;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.talend.daikon.client.ClientService;
import org.talend.dataprep.api.folder.Folder;
import org.talend.dataprep.api.folder.FolderInfo;
import org.talend.dataprep.api.folder.FolderTreeNode;
import org.talend.dataprep.api.folder.UserFolder;
import org.talend.dataprep.dataset.DataSetMetadataBuilder;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.APIErrorCodes;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.preparation.service.UserPreparation;
import org.talend.dataprep.security.SecurityProxy;
import org.talend.dataprep.services.folder.IFolderService;
import org.talend.dataprep.services.preparation.IPreparationService;
import org.talend.dataprep.util.SortAndOrderHelper.Order;
import org.talend.dataprep.util.SortAndOrderHelper.Sort;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class FolderAPI extends APIService {

    @Autowired
    private ClientService clients;

    @Autowired
    private DataSetMetadataBuilder metadataBuilder;

    /** Security proxy let the current thread to borrow another identity for a while. */
    @Autowired
    private SecurityProxy securityProxy;

    @RequestMapping(value = "/api/folders", method = GET)
    @ApiOperation(value = "List folders. Optional filter on parent ID may be supplied.",
            produces = APPLICATION_JSON_VALUE)
    @Timed
    public Stream<UserFolder> listFolders(@RequestParam(required = false) String parentId) {
        try {
            return clients.of(IFolderService.class).list(parentId, Sort.LAST_MODIFICATION_DATE, Order.DESC);
        } catch (Exception e) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_LIST_FOLDERS, e);
        }
    }

    @RequestMapping(value = "/api/folders/tree", method = GET)
    @ApiOperation(value = "List all folders", produces = APPLICATION_JSON_VALUE)
    @Timed
    public FolderTreeNode getTree() {
        try {
            return clients.of(IFolderService.class).getTree();
        } catch (Exception e) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_LIST_FOLDERS, e);
        }
    }

    @RequestMapping(value = "/api/folders/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get folder by id", produces = APPLICATION_JSON_VALUE, notes = "Get a folder by id")
    @Timed
    public FolderInfo getFolderAndHierarchyById(@PathVariable(value = "id") final String id) {
        try {
            return clients.of(IFolderService.class).getFolderAndHierarchyById(id);
        } catch (Exception e) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_GET_FOLDERS, e);
        }
    }

    @RequestMapping(value = "/api/folders", method = PUT)
    @ApiOperation(value = "Add a folder.", produces = APPLICATION_JSON_VALUE)
    @Timed
    public Folder addFolder(@RequestParam(required = false) final String parentId,
                            @RequestParam final String path) {
        try {
            return clients.of(IFolderService.class).addFolder(parentId, path);
        } catch (Exception e) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_CREATE_FOLDER, e);
        }
    }

    /**
     * no javadoc here so see description in @ApiOperation notes.
     */
    @RequestMapping(value = "/api/folders/{id}", method = DELETE)
    @ApiOperation(value = "Remove a Folder")
    @Timed
    public void removeFolder(@PathVariable final String id, final OutputStream output) {
        try {
            clients.of(IFolderService.class).removeFolder(id);
        } catch (Exception e) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_DELETE_FOLDER, e);
        }
    }

    @RequestMapping(value = "/api/folders/{id}/name", method = PUT)
    @ApiOperation(value = "Rename a Folder")
    @Timed
    public void renameFolder(@PathVariable final String id, @RequestBody final String newName) {
        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(newName)) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_RENAME_FOLDER);
        }
        try {
            clients.of(IFolderService.class).renameFolder(id, newName);
        } catch (Exception e) {
            throw new TDPException(APIErrorCodes.UNABLE_TO_RENAME_FOLDER, e);
        }
    }

    /**
     * no javadoc here so see description in @ApiOperation notes.
     *
     * @param name The folder to search.
     * @param strict Strict mode means searched name is the full name.
     * @return the list of folders that match the given name.
     */
    @RequestMapping(value = "/api/folders/search", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Search Folders with parameter as part of the name", produces = APPLICATION_JSON_VALUE)
    @Timed
    public Stream<UserFolder> search(@RequestParam(required = false) final String name,
                                     @RequestParam(required = false) final Boolean strict, @RequestParam(required = false) final String path) {
        return clients.of(IFolderService.class).search(name, strict, path);
    }

    /**
     * List all the folders and preparations out of the given id.
     *
     * @param id Where to list folders and preparations.
     */
    //@formatter:off
    @RequestMapping(value = "/api/folders/{id}/preparations", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all preparations for a given id.", notes = "Returns the list of preparations for the given id the current user is allowed to see.")
    @Timed
    public PreparationByFolderResult listPreparationsByFolder(
            @PathVariable @ApiParam(name = "id", value = "The destination to search preparations from.") final String id, //
            @ApiParam(value = "Sort key (by name or date), defaults to 'date'.") @RequestParam(defaultValue = "creationDate") final Sort sort, //
            @ApiParam(value = "Order for sort key (desc or asc), defaults to 'desc'.") @RequestParam(defaultValue = "desc") final Order order) {
    //@formatter:on

        if (LOG.isDebugEnabled()) {
            LOG.debug("Listing preparations in destination {} (pool: {} )...", id, getConnectionStats());
        }

        LOG.info("Listing preparations in folder {}", id);
        PreparationByFolderResult result = new PreparationByFolderResult();
        result.folders = clients.of(IFolderService.class).list(id, sort, order);
        result.preparations = clients.of(IPreparationService.class).listAll("", "", id, sort, order);
        return result;
    }

    class PreparationByFolderResult {
        public Stream<UserFolder> folders;
        public Stream<UserPreparation> preparations;
    }
}
