package io.kylin.mdx.insight.core.model.acl;

import org.junit.Test;

import static org.junit.Assert.*;

public class AclProjectModelTest {

    @Test
    public void testIsCompatible() {
        AclProjectModel projectModel1 = new AclProjectModel("project", "user", "Dev");
        {
            AclTableModel tableModel1 = new AclTableModel("table_1");
            tableModel1.setAccess("column_1", false);
            projectModel1.setModel("table_1", tableModel1);
        }
        AclProjectModel projectModel2 = new AclProjectModel("project", "user", "Dev");
        {
            AclTableModel tableModel2 = new AclTableModel("table_2");
            tableModel2.setAccess("column_2", false);
            projectModel2.setModel("table_2", tableModel2);
        }
        AclProjectModel projectModel3 = new AclProjectModel("project", "user", "Dev");
        {
            AclTableModel tableModel3 = new AclTableModel("table_1");
            tableModel3.setInvisible(true);
            projectModel3.setModel("table_1", tableModel3);
        }
        AclProjectModel projectModel4 = new AclProjectModel("project", "user", "Dev");
        {
            AclTableModel tableModel4 = new AclTableModel("table_1");
            tableModel4.setAccess("column_1", false);
            tableModel4.setAccess("column_2", false);
            projectModel4.setModel("table_1", tableModel4);
        }
        assertTrue(projectModel1.isCompatible(projectModel1));
        assertFalse(projectModel1.isCompatible(projectModel2));
        assertFalse(projectModel1.isCompatible(projectModel3));
        assertFalse(projectModel1.isCompatible(projectModel4));
    }

}