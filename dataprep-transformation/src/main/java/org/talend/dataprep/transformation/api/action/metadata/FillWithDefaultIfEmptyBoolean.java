package org.talend.dataprep.transformation.api.action.metadata;

import org.apache.commons.lang.StringUtils;
import org.talend.dataprep.api.type.Types;
import org.talend.dataprep.transformation.api.action.metadata.Item.Value;

public class FillWithDefaultIfEmptyBoolean extends FillWithDefaultIfEmpty {

    public static final String         FILL_EMPTY_ACTION_NAME = "fillemptywithdefaultboolean";      //$NON-NLS-1$

    public static final ActionMetadata INSTANCE = new FillWithDefaultIfEmptyBoolean();

    @Override
    public Parameter[] getParameters() {
        return new Parameter[] { new Parameter(COLUMN_NAME_PARAMETER, Types.STRING.getName(), StringUtils.EMPTY) };
    }

    @Override
    public Item[] getItems() {
        Value[] values = new Value[] { new Value("True"), new Value("False") };
        return new Item[] { new Item(DEFAULT_VALUE_PARAMETER, Type.LIST, "categ", values) };
    }

}
