package mondrian.olap;

import lombok.Getter;
import lombok.Setter;
import mondrian.server.Statement;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Statement containing sub-query.
 * such as:
 * # [WITH members,sets...]
 * # SELECT query-axis-clause
 * # FROM [NON VISUAL]
 * #   ( SELECT query-axis-clause FROM cube )
 * # [WHERE slicer-axis-clause]
 */
public class SubQuery extends Query {

    @Getter
    @Setter
    private boolean nonVisual;

    @Getter
    @Setter
    private Query query;

    public SubQuery(
            Statement statement,
            Formula[] formulas,
            QueryAxis[] axes,
            boolean nonVisual,
            Query query,
            QueryAxis slicerAxis,
            QueryPart[] cellProps,
            boolean strictValidation) {
        super(
                statement,
                null,
                null,
                formulas,
                axes,
                slicerAxis,
                cellProps,
                new Parameter[0],
                strictValidation,
                true);
        this.nonVisual = nonVisual;
        this.query = Objects.requireNonNull(query);
    }

    @Override
    public void unparseSubcube(PrintWriter pw) {
        // 用于当 SubQuery 改写为 Query 后生成语句
        if (super.getCubeName() != null) {
            super.unparseSubcube(pw);
            return;
        }
        pw.print("FROM ");
        if (isNonVisual()) {
            pw.print("NON VISUAL ");
        }
        pw.println("(");
        String result = fillWhitespace(Util.unparse(query));
        pw.println(result);
        pw.println(")");
    }

    // TODO: Try to merge SubQuery and Query, Not override method.

    @Override
    public String getCubeName() {
        return getNonSubquery().getCubeName();
    }

    @Override
    public Cube getCube() {
        return getNonSubquery().getCube();
    }

    @Override
    public SchemaReader getSchemaReader(boolean accessControlled) {
        return getNonSubquery().getSchemaReader(accessControlled);
    }

    /**
     * Gets the last non subquery.
     *
     * @return Query, Not Subquery.
     */
    public Query getNonSubquery() {
        Query query = this.query;
        while (query instanceof SubQuery) {
            query = ((SubQuery) query).getQuery();
        }
        return query;
    }

    /**
     * Add a space at the beginning of each line
     */
    public static String fillWhitespace(String str) {
        String[] array = str.split("\n");
        List<String> list = Stream.of(array).
                map(x -> StringUtils.repeat(' ', 2) + x)
                .collect(Collectors.toList());
        return StringUtils.join(list, "\n");
    }

}
