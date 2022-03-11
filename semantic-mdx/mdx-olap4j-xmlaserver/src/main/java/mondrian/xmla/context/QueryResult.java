package mondrian.xmla.context;

import mondrian.xmla.SaxWriter;
import org.olap4j.OlapException;
import org.xml.sax.SAXException;

import java.sql.SQLException;

public interface QueryResult {

    void unparse(SaxWriter res) throws SAXException, OlapException;

    void close() throws SQLException;

    void metadata(SaxWriter writer);

    default void metadata_PowerBI(SaxWriter writer) {
        metadata(writer);
    }

}
