package mondrian.olap4j;

import org.olap4j.mdx.ParseRegion;
import org.olap4j.mdx.ParseTreeNode;
import org.olap4j.mdx.ParseTreeVisitor;
import org.olap4j.mdx.ParseTreeWriter;
import org.olap4j.type.Type;

/**
 * Created by Chunen Ni on 2017/7/19.
 */
public class MondrianOlap4jParseTreeNode implements ParseTreeNode {
    private String exp = "";
    private String format = "";

    public <T> T accept(ParseTreeVisitor<T> parseTreeVisitor) {
        return null;
    }

    public Type getType() {
        return null;
    }

    public void unparse(ParseTreeWriter parseTreeWriter) {

    }

    public ParseRegion getRegion() {
        return null;
    }

    public ParseTreeNode deepCopy() {
        return null;
    }

    public String getExp() {
        return exp;
    }

    public void setExp(String exp) {
        this.exp = exp;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return exp + "___" + format;
    }

}
