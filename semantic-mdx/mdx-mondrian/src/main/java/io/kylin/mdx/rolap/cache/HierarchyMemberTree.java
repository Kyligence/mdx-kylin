package io.kylin.mdx.rolap.cache;

import mondrian.rolap.RolapMember;

import java.util.List;

/**
 * A tree-like structure which can find members of a hierarchy with unique names and node relationships.
 * @author rong.zheng
 */
public interface HierarchyMemberTree {
    /**
     * Returns a member of the current hierarchy.
     * @param uniqueName Unique name of the target member.
     * @return The target member instance.
     */
    RolapMember getMember(String uniqueName);

    /**
     * Returns the current hierarchy's "All" member.
     * @return The "All" member.
     */
    RolapMember getAllMember();

    /**
     * Returns the current hierarchy's default member. Currently it is always the "All" member.
     * @return The default member.
     */
    RolapMember getDefaultMember();

    /**
     * Returns all members of the input level depth as an unmodifiable list.
     * @param depth Depth of target level.
     * @return An unmodifiable list of target level's members.
     */
    List<RolapMember> getMembersByLevelDepth(int depth);

    /**
     * Returns the number of all members of the input level depth.
     * @param depth Depth of target level.
     * @return The number of target level's members.
     */
    int getMembersCountByLevelDepth(int depth);

    /**
     * Returns all members of all levels in the current hierarchy as an unmodifiable list.
     * @return An unmodifiable list of all members.
     */
    List<RolapMember> getAllMembers();

    /**
     * Returns all members between a start member and an end member, the start and end member must be of the same level.
     * @param startUniqueName Unique name of the start member.
     * @param includeStart Whether the result should include the start member.
     * @param endUniqueName Unique name of the end member.
     * @param includeEnd Whether the result should include the end member.
     * @return The list of members between the start and end member.
     */
    List<RolapMember> getMembersBetween(String startUniqueName, boolean includeStart, String endUniqueName, boolean includeEnd);

    /**
     * Returns the parent member of an input member.
     * @param uniqueName Unique name of the original member.
     * @return The parent member of the input member.
     */
    RolapMember getParentMember(String uniqueName);

    /**
     * Returns children members of an input member.
     * @param uniqueName Unique name of the original member.
     * @return The children members of the input member.
     */
    List<RolapMember> getChildrenMembers(String uniqueName);

    /**
     * Returns children members count of the input member.
     * @param uniqueName Unique name of the original member.
     * @return The children members count of the input member.
     */
    int getChildrenMembersCount(String uniqueName);

    /**
     * Returns descendant members of an input member between the input depths.
     * @param uniqueName Unique name of the original member.
     * @param startDepth Staring depth of descendant members, must not less than depth of the original member.
     * @param endDepth Ending depth of descendant members, must not less than depth of the original member.
     * @return An unmodifiable list of requesting descendant members.
     */
    List<RolapMember> getDescendantMembers(String uniqueName, int startDepth, int endDepth);

    /**
     * Returns the first child member of an input member.
     * @param uniqueName Unique name of the original member.
     * @return The first child member of the input member.
     */
    RolapMember getFirstChildMember(String uniqueName);

    /**
     * Returns the last child member of an input member.
     * @param uniqueName Unique name of the original member.
     * @return The last child member of the input member.
     */
    RolapMember getLastChildMember(String uniqueName);

    /**
     * Returns the first sibling member of an input member.
     * @param uniqueName Unique name of the original member.
     * @return The first sibling member of the input member.
     */
    RolapMember getFirstSiblingMember(String uniqueName);

    /**
     * Returns the last sibling member of an input member.
     * @param uniqueName Unique name of the original member.
     * @return The last sibling member of the input member.
     */
    RolapMember getLastSiblingMember(String uniqueName);

    /**
     * Returns the previous member of an input member.
     * @param uniqueName Unique name of the original member.
     * @return The previous member of an input member.
     */
    RolapMember getPrevMember(String uniqueName);

    /**
     * Returns the next member of an input member.
     * @param uniqueName Unique name of the original member.
     * @return The next member of an input member.
     */
    RolapMember getNextMember(String uniqueName);

    /**
     * Returns the {@code offset}'th previous member of an input member.
     * @param uniqueName Unique name of the original member.
     * @param offset Offset number to the target member.
     * @return The {@code offset}'th previous member of an input member.
     */
    RolapMember getLeadMember(String uniqueName, int offset);

    /**
     * Returns the {@code offset}'th next member of an input member.
     * @param uniqueName Unique name of the original member.
     * @param offset Offset number to the target member.
     * @return The {@code offset}'th next member of an input member.
     */
    RolapMember getLagMember(String uniqueName, int offset);

    /**
     * Returns a descendant member of an input member The descendant member is {@code depthDown} levels down of
     * the input member, and its all ascendants are the first member of their parents' children.
     * @param uniqueName Unique name of the original member.
     * @param depthDown Depth number down to the target member.
     * @return The target descendant member.
     */
    RolapMember getFirstDescendantLevelMember(String uniqueName, int depthDown);

    /**
     * Returns the member which has similar position as an input member. "Similar" means in the same level and
     * have almost the same child indices since the root node, but have an offset in a specific level.
     * @param uniqueName Unique name of the original member.
     * @param depthsUp The level amount that the level where the offset should be applied is higher than the level of the input member.
     * @param offset Offset between the ancestor of the input member and the ancestor of the target member at the level
     *               which is specified by {@code depthsUp}.
     * @return The target "similar" member.
     */
    RolapMember getSimilarPositionMemberWithDepthsUp(String uniqueName, int depthsUp, int offset);

    /**
     * Performs fuzzy search on the whole hierarchy and return all matched members.
     * @param searchTarget The searching target.
     * @param depth Depth of the searching level.
     * @return An unmodifiable list of all matching members.
     */
    List<RolapMember> fuzzySearchByName(String searchTarget, int depth);
}
