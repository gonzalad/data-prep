// ============================================================================
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
// ============================================================================

package org.talend.dataprep.transformation.actions.common;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.transformation.api.action.context.ActionContext;

public abstract class AbstractDataSetAction extends AbstractActionMetadata implements ColumnAction, DataSetAction {

    public void applyOnDataSet(DataSetRow row, ActionContext context) {
        for (ColumnMetadata column : row.getRowMetadata().getColumns()) {
            apply(row, column.getId(), context);
        }
    }

    public void applyOnColumn(DataSetRow row, ActionContext context) {
        String targetColumnId = ActionsUtils.getTargetColumnId(context);
        apply(row, targetColumnId ,context);
    }

    public abstract void apply(DataSetRow row, String targetColumnId, ActionContext context);

}