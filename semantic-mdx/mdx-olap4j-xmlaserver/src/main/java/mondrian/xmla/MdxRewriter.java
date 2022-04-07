package mondrian.xmla;

import org.olap4j.xmla.server.impl.Pair;

/**
 * rewrite MDX statement to support sub-query.
 */
public interface MdxRewriter {

    Pair<Boolean, String> rewrite(String mdx);

}
