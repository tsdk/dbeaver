/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public abstract class PostgreToolTriggerToggle implements IExternalTool {

    private boolean isEnable;

    protected PostgreToolTriggerToggle(boolean enable) {
        this.isEnable = enable;
    }

    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
        throws DBException
    {
        List<PostgreTrigger> triggeList = CommonUtils.filterCollection(objects, PostgreTrigger.class);
        if (!triggeList.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), triggeList);
            if (dialog.open() == IDialogConstants.OK_ID) {
                refreshObjectsState(triggeList);
            }
        }
    }

    private void refreshObjectsState(List<PostgreTrigger> triggeList) {
        try {
            UIUtils.runInProgressDialog(monitor -> {
                for (PostgreTrigger trigger : triggeList) {
                    try {
                        DBNDatabaseNode triggerNode = NavigatorUtils.getNodeByObject(trigger);
                        if (triggerNode != null) {
                            triggerNode.refreshNode(monitor, PostgreToolTriggerToggle.this);
                        } else {
                            trigger.refreshObjectState(monitor);
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Refresh triggers state", "Error refreshign trigger state", e.getTargetException());
        }
    }

    class SQLDialog extends TableToolDialog {

        public SQLDialog(IWorkbenchPartSite partSite, List<PostgreTrigger> selectedTrigger) {
            super(partSite, (isEnable ? "Enable" : "Disable") + " trigger", selectedTrigger);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, PostgreObject object) {
            lines.add("ALTER TABLE " + ((PostgreTrigger) object).getTable() + " " + (isEnable ? "ENABLE" : "DISABLE")
                + " TRIGGER " + DBUtils.getQuotedIdentifier(object));
        }

        @Override
        protected void createControls(Composite parent) {
            createObjectsSelector(parent);
        }
    }

}
