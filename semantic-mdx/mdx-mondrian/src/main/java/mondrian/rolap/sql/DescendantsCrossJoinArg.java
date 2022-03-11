/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 TONBELLER AG
// Copyright (C) 2006-2013 Pentaho and others
// All Rights Reserved.
*/
package mondrian.rolap.sql;

import mondrian.rolap.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents one of:
 * <ul>
 * <li>Level.Members:  member == null and level != null</li>
 * <li>Member.Children: member != null and level =
 *     member.getLevel().getChildLevel()</li>
 * <li>Member.Descendants: member != null and level == some level below
 *     member.getLevel()</li>
 * </ul>
 */
public class DescendantsCrossJoinArg implements CrossJoinArg {
    RolapMember member;
    /**
     * Will be parent of {@link DescendantsCrossJoinArg#member}
     * if {@link DescendantsCrossJoinArg#pullUpMember()} is called.
     */
    RolapMember originalMember;
    final RolapCubeLevel level;
    final boolean leaves;
    private boolean memberPulledup = false;

    public DescendantsCrossJoinArg(RolapCubeLevel level, RolapMember member, boolean leaves) {
        this.level = level;
        this.originalMember = member;
        this.member = member;
        this.leaves = leaves;
    }

    public RolapCubeLevel getLevel() {
        return level;
    }

    public List<RolapMember> getMembers() {
        if (member == null) {
            return null;
        }
        final List<RolapMember> list = new ArrayList<RolapMember>();
        list.add(member);
        return list;
    }

    public List<RolapMember> getOriginalMembers() {
        return Collections.singletonList(originalMember);
    }

    public boolean isLeaves() {
        return leaves;
    }

    /**
     * Keep the level and pull the member onto its parent level.
     * This method is used only on Descendants(&lt;CurrentMember&rt;,,LEAVES) situations.
     */
    public void pullUpMember() {
        if (!memberPulledup && isLeaves()) {
            memberPulledup = true;
            originalMember = member;
            member = member.getParentMember();
        }
    }

    public void addConstraint(
        SqlQueryBuilder queryBuilder,
        RolapStarSet starSet)
    {
        if (member != null) {
            SqlConstraintUtils.addMemberConstraint(
                queryBuilder, starSet, member, true);
        }
    }

    public boolean isPreferInterpreter(boolean joinArg) {
        return false;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DescendantsCrossJoinArg)) {
            return false;
        }

        DescendantsCrossJoinArg that = (DescendantsCrossJoinArg) obj;
        if (this.leaves != that.leaves || !Objects.equals(this.level, that.level)) {
            return false;
        }

        if (this.member == null && that.member == null) {
            return Objects.equals(this.originalMember, that.originalMember);
        }
        return Objects.equals(this.member, that.member);
    }

    public int hashCode() {
        int c = 1;
        if (level != null) {
            c = level.hashCode();
        }
        if (member != null) {
            c = 31 * c + member.hashCode();
        } else if (originalMember != null) {
            c = 31 * c + originalMember.hashCode();
        }
        c += Boolean.hashCode(leaves);
        return c;
    }
}

// End DescendantsCrossJoinArg.java
