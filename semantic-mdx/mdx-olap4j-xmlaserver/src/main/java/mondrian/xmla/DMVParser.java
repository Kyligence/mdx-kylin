package mondrian.xmla;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mondrian.xmla.XmlaConstants.*;


public class DMVParser {
    /**
     * The enum and regular expression which is used to match a DMV
     */
    public enum DMVElements {
        SelectColumns(1), RequestType(2), Restrictions(3);

        private int groupId;

        private int getGroupId() {
            return groupId;
        }

        DMVElements(int groupId) {
            this.groupId = groupId;
        }
    }

    /**
     * Can only deal with single-line DMVs
     */
    private static String dmvRegex =
            String.format("select\\s*(?<%s>\\[.*])\\s*from\\s+\\$system\\.(?<%s>\\S+)(?:\\s+where\\s+(?<%s>.*))?",
                    DMVElements.SelectColumns.name(),
                    DMVElements.RequestType.name(),
                    DMVElements.Restrictions.name());
    private static Pattern dmvPattern = Pattern.compile(dmvRegex, Pattern.CASE_INSENSITIVE);

    /**
     * The regular expression which is used to parse "select" clause
     */
    private static String dmvSelectRegex = "\\s*\\[(?<SelectColumn>.+)?]\\s*";
    private static Pattern dmvSelectPattern = Pattern.compile(dmvSelectRegex);

    /**
     * The regular expression which is used to parse "where" clause
     */
    private static String dmvRestrictionRegex = "\\s*\\[(?<RestrictionField>.+?)?](?:\\s*(?<RestrictionType>=|(?:<>))\\s*(?<RestrictionValue>.+))?\\s*";
    private static Pattern dmvRestrictionPattern = Pattern.compile(dmvRestrictionRegex);
    private static String dmvQuotedRestrictionRegex = "'(?<RestrictionUnquotedValue>.+)'";
    private static Pattern dmvQuotedRestrictionPattern = Pattern.compile(dmvQuotedRestrictionRegex);

    private Matcher dmvMatcher;

    /**
     * Create a new {@link DMVParser} with a statement, care that the statement is not validated so far.
     *
     * @param statement The input statement.
     */
    public DMVParser(String statement) {
        this.dmvMatcher = dmvPattern.matcher(statement);
    }


    private String selectColumnsString;
    private String requestTypeSting;
    private String selectRestrictionsString;

    /**
     * Check if the input statement is a valid DMV, if it is, parse its "select", "from" and "where" clauses.
     *
     * @return True if the statement is a valid DMV, false if not.
     */
    public boolean find() {
        boolean found = dmvMatcher.find();
        if (found) {
            selectColumnsString = dmvMatcher.group(DMVElements.SelectColumns.name());
            requestTypeSting = dmvMatcher.group(DMVElements.RequestType.name());
            selectRestrictionsString = dmvMatcher.group(DMVElements.Restrictions.name());
        }
        return found;
    }


    /**
     * Get the column names defined in the DMV "select" clause.
     *
     * @return A {@link Set} which contains all select column names.
     */
    public Set<String> getSelectColumns() {
        return parseSelectColumns(selectColumnsString);
    }

    /**
     * Parse a DMV "select" clause into a set of column names.
     *
     * @param selectColumnsString "SelectColumns" group found in {@link mondrian.xmla.DMVParser#dmvPattern}.
     * @return A {@link Set} which contains all select column names.
     */
    private static Set<String> parseSelectColumns(String selectColumnsString) {
        Set<String> selectColumnStringSet = new HashSet<String>();
        String[] selectColumnStrings = selectColumnsString.split(",");

        for (String selectColumnString : selectColumnStrings) {
            Matcher columnMatcher = dmvSelectPattern.matcher(selectColumnString);

            if (columnMatcher.find()) {
                selectColumnString = columnMatcher.group("SelectColumn");
            }
            selectColumnStringSet.add(selectColumnString);
        }
        return selectColumnStringSet;
    }


    /**
     * Get the query target which is specified in the "from" clause. The returned value should always be
     * a {@link RowsetDefinition} name.
     *
     * @return The query target rowset name.
     */
    public String getRequestType() {
        return requestTypeSting;
    }


    /**
     * The class which indicates a DMV "where" restriction.
     *
     * @param <V> The restriction type. Only supports {@link String} and {@link Boolean} so far.
     */
    public abstract static class DMVRestriction<V> {
        public abstract boolean check(V value);

        @Override
        public abstract boolean equals(Object obj);
    }

    /**
     * Indicates a {@link String} "=" restriction.
     */
    public static class DMVEqualsRestriction extends DMVRestriction<String> {
        private String equalsString;

        public DMVEqualsRestriction(String equalsString) {
            this.equalsString = equalsString;
        }

        public String getEqualsString() {
            return equalsString;
        }

        @Override
        public boolean check(String value) {
            return value.equals(equalsString);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DMVEqualsRestriction &&
                    getEqualsString().equals(((DMVEqualsRestriction) obj).getEqualsString());
        }
    }

    /**
     * Indicates a {@link String} <>" restriction.
     */
    public static class DMVNotEqualsRestriction extends DMVRestriction<String> {
        private String notEqualsString;

        public DMVNotEqualsRestriction(String notEqualsString) {
            this.notEqualsString = notEqualsString;
        }

        public String getNotEqualsString() {
            return notEqualsString;
        }

        @Override
        public boolean check(String value) {
            return !value.equals(notEqualsString);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DMVNotEqualsRestriction &&
                    getNotEqualsString().equals(((DMVNotEqualsRestriction) obj).getNotEqualsString());
        }
    }

    /**
     * Indicated a {@link Boolean} restriction.
     */
    public static class DMVIfRestriction extends DMVRestriction<Boolean> {
        private DMVIfRestriction() {
        }

        private static class DMVIfRestrictionInner {
            private static DMVIfRestriction singleton = new DMVIfRestriction();
        }

        public static DMVIfRestriction getInstance() {
            return DMVIfRestrictionInner.singleton;
        }

        @Override
        public boolean check(Boolean value) {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof DMVIfRestriction;
        }
    }

    /**
     * Get the restrictions defined in the DMV "where" clause.
     *
     * @param parameters The {@link Map} of provided parameters, it will be used when the "where" clause contains
     *                   *            something like "[CUBE_NAME] = @CubeName".
     * @return A {@link Map} whose keys are column names, and whose values are the corresponding {@link DMVRestriction}s.
     */
    public Map<String, DMVRestriction<?>> getSelectRestrictions(Map<String, String> parameters) {
        return parseSelectRestrictions(selectRestrictionsString, parameters);
    }

    /**
     * Parse a DMV "where" clause into a map of column names and restriction types.
     *
     * @param selectRestrictionsString "Restrictions" group found in {@link mondrian.xmla.DMVParser#dmvPattern}.
     * @param parameters               The {@link Map} of provided parameters, it will be used when the "where" clause contains
     *                                 something like "[CUBE_NAME] = @CubeName".
     * @return A {@link Map} whose keys are column names, and whose values are the corresponding {@link DMVRestriction}s.
     */
    private static Map<String, DMVRestriction<?>> parseSelectRestrictions(String selectRestrictionsString, Map<String, String> parameters) {
        Map<String, DMVRestriction<?>> restrictionMap = new HashMap<>();
        if (selectRestrictionsString != null) {
            String[] selectRestrictionStrings = selectRestrictionsString.split("\\sand\\s");

            for (String selectRestrictionString : selectRestrictionStrings) {
                Matcher restrictionMatcher = dmvRestrictionPattern.matcher(selectRestrictionString);

                if (restrictionMatcher.find()) {
                    DMVRestriction<?> restriction;
                    String restrictionField = restrictionMatcher.group("RestrictionField");
                    String restrictionType = restrictionMatcher.group("RestrictionType");
                    String restrictionValue = restrictionMatcher.group("RestrictionValue");

                    if (restrictionValue != null) {
                        Matcher quotedRestrictionMatcher = dmvQuotedRestrictionPattern.matcher(restrictionValue);
                        if (quotedRestrictionMatcher.find()) {
                            restrictionValue = quotedRestrictionMatcher.group("RestrictionUnquotedValue");
                        } else if (restrictionValue.startsWith("@") && parameters != null) {
                            restrictionValue = parameters.get(restrictionValue.substring(1));
                        }
                    }

                    if (restrictionType != null && restrictionValue != null) {
                        switch (restrictionType) {
                            case "=":
                                restriction = new DMVEqualsRestriction(restrictionValue);
                                break;
                            case "<>":
                                restriction = new DMVNotEqualsRestriction(restrictionValue);
                                break;
                            default:
                                throw new XmlaException(SERVER_FAULT_FC, HSB_PARSE_DMV_CODE, HSB_PARSE_DMV_FAULT_FS,
                                        new IllegalArgumentException("Unsupported \"where\" clause"));
                        }
                    } else {
                        restriction = DMVIfRestriction.getInstance();
                    }
                    restrictionMap.put(restrictionField, restriction);
                }
            }
        }
        return restrictionMap;
    }
}
