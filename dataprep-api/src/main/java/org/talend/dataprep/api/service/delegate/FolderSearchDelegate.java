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

package org.talend.dataprep.api.service.delegate;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.daikon.client.ClientService;
import org.talend.dataprep.api.folder.Folder;
import org.talend.dataprep.services.folder.IFolderService;

/**
 * A {@link SearchDelegate} implementation to search in folders.
 */
@Component
public class FolderSearchDelegate extends AbstractSearchDelegate<Folder> {

    @Autowired
    private ClientService clients;

    @Override
    public String getSearchCategory() {
        return "folders";
    }

    @Override
    public String getSearchLabel() {
        return "folders";
    }

    @Override
    public String getInventoryType() {
        return "folder";
    }

    @Override
    public Stream<Folder> search(String query, boolean strict) {
        return clients.of(IFolderService.class).search(query, strict, null);
    }
}
