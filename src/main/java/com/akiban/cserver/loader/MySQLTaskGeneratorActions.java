/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.cserver.loader;

import java.util.IdentityHashMap;

import com.akiban.ais.model.AkibaInformationSchema;
import com.akiban.ais.model.Join;
import com.akiban.ais.model.UserTable;

public class MySQLTaskGeneratorActions implements TaskGenerator.Actions
{
    // TaskGenerator.Actions interface

    @Override
    public void generateTasksForTableContainingHKeyColumns(BulkLoader loader,
                                                           UserTable table,
                                                           IdentityHashMap<UserTable, TableTasks> tasks)
    {
        TableTasks tableTasks = tableTasks(tasks, table);
        if (tableTasks.generateParent() != null) {
            // Shouldn't discover the need for this task twice
            throw new BulkLoader.InternalError(table.toString());
        }
        GenerateFinalBySortTask task = new GenerateFinalBySortTask(loader, table);
        tableTasks.generateFinal(task);
        tasks.put(table, tableTasks);
    }

    @Override
    public void generateTasksForTableNotContainingHKeyColumns(
            BulkLoader loader, Join join,
            IdentityHashMap<UserTable, TableTasks> tasks) throws Exception
    {
        // Generate parent's $parent
        UserTable parent = join.getParent();
        TableTasks parentTasks = tableTasks(tasks, parent);
        if (parentTasks.generateParent() == null) {
            parentTasks.generateParent(new GenerateParentBySortTask(loader, parent));
        }
        // Generate child's $child
        UserTable child = join.getChild();
        TableTasks childTasks = tableTasks(tasks, child);
        if (childTasks.generateChild() == null) {
            childTasks.generateChild(new GenerateChildTask(loader, child));
        }
        // Generate child's $parent
        if (childTasks.generateParent() == null) {
            childTasks.generateParent(new GenerateParentByMergeTask(loader,
                                                                    child,
                                                                    parentTasks.generateParent(),
                                                                    childTasks.generateChild(),
                                                                    ais));
        }
        // Generate parent's $final
        if (parentTasks.generateFinal() == null) {
            parentTasks.generateFinal(new GenerateFinalByMergeTask(loader,
                                                                   parent,
                                                                   parentTasks.generateParent(),
                                                                   ais));
        }
        // Generate child's $final
        if (childTasks.generateFinal() == null) {
            childTasks.generateFinal(new GenerateFinalByMergeTask(loader,
                                                                  child,
                                                                  childTasks.generateParent(),
                                                                  ais));
        }
    }

    // MySQLTaskGeneratorActions

    public MySQLTaskGeneratorActions(AkibaInformationSchema ais)
    {
        this.ais = ais;
    }

    // For use by this class

    private static TableTasks tableTasks(
            IdentityHashMap<UserTable, TableTasks> tasks, UserTable table)
    {
        TableTasks tableTasks = tasks.get(table);
        if (tableTasks == null) {
            tableTasks = new TableTasks();
            tasks.put(table, tableTasks);
        }
        return tableTasks;
    }

    // State

    private final AkibaInformationSchema ais;
}
