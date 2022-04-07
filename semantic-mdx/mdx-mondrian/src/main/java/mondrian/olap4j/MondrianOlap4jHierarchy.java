/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2007-2013 Pentaho
// All Rights Reserved.
*/
package mondrian.olap4j;

import mondrian.olap.LocalizedProperty;
import mondrian.olap.OlapElement;

import mondrian.rolap.RolapHierarchy;
import org.olap4j.OlapException;
import org.olap4j.impl.*;
import org.olap4j.metadata.*;

import java.util.List;

/**
 * Implementation of {@link org.olap4j.metadata.Hierarchy}
 * for the Mondrian OLAP engine.
 *
 * @author jhyde
 * @since May 25, 2007
 */
class MondrianOlap4jHierarchy
    extends MondrianOlap4jMetadataElement
    implements Hierarchy, Named
{
    final MondrianOlap4jSchema olap4jSchema;
    final mondrian.olap.Hierarchy hierarchy;

    MondrianOlap4jHierarchy(
        MondrianOlap4jSchema olap4jSchema,
        mondrian.olap.Hierarchy hierarchy)
    {
        this.olap4jSchema = olap4jSchema;
        this.hierarchy = hierarchy;
    }

    public boolean equals(Object obj) {
        return obj instanceof MondrianOlap4jHierarchy
            && hierarchy.equals(((MondrianOlap4jHierarchy) obj).hierarchy);
    }

    public int hashCode() {
        return hierarchy.hashCode();
    }

    public Dimension getDimension() {
        return new MondrianOlap4jDimension(
            olap4jSchema, hierarchy.getDimension());
    }

    public NamedList<Level> getLevels() {
        final NamedList<MondrianOlap4jLevel> list =
            new NamedListImpl<MondrianOlap4jLevel>();
        final MondrianOlap4jConnection olap4jConnection =
            olap4jSchema.olap4jCatalog.olap4jDatabaseMetaData.olap4jConnection;
        final mondrian.olap.SchemaReader schemaReader =
            olap4jConnection.getMondrianConnection2().getSchemaReader()
                .withLocus();
        for (mondrian.olap.Level level
            : hierarchy.getLevelList())
        {
            list.add(olap4jConnection.toOlap4j(level));
        }
        return Olap4jUtil.cast(list);
    }

    public boolean hasAll() {
        return hierarchy.hasAll();
    }

    public Member getDefaultMember() throws OlapException {
        final MondrianOlap4jConnection olap4jConnection =
            olap4jSchema.olap4jCatalog.olap4jDatabaseMetaData.olap4jConnection;
        final mondrian.olap.SchemaReader schemaReader =
            olap4jConnection.getMondrianConnection()
                .getSchemaReader().withLocus();
        return
            olap4jConnection.toOlap4j(
                schemaReader.getHierarchyDefaultMember(hierarchy));
    }

    public NamedList<Member> getRootMembers() throws OlapException {
        final List<Member> levelMembers =
            getLevels().get(0).getMembers();

        return new AbstractNamedList<Member>() {
            public String getName(Object member) {
                return ((Member)member).getName();
            }

            public Member get(int index) {
                return levelMembers.get(index);
            }

            public int size() {
                return levelMembers.size();
            }
        };
    }

    public String getName() {
        return hierarchy.getName();
    }

    public String getUniqueName() {
        return hierarchy.getUniqueName();
    }

    public String getCaption() {
        return hierarchy.getLocalized(
            LocalizedProperty.CAPTION,
            olap4jSchema.getLocale());
    }

    public String getDescription() {
        if (hierarchy instanceof RolapHierarchy) {
            String subfolder = ((RolapHierarchy) hierarchy).getSubfolder();
            if (subfolder != null && !subfolder.isEmpty()) {
                return subfolder;
            }
        }
        return hierarchy.getLocalized(
                LocalizedProperty.DESCRIPTION, olap4jSchema.getLocale());
    }

    public boolean isVisible() {
        return hierarchy.isVisible();
    }

    protected OlapElement getOlapElement() {
        return hierarchy;
    }
}

// End MondrianOlap4jHierarchy.java
