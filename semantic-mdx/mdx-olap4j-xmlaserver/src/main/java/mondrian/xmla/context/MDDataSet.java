package mondrian.xmla.context;

import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.metadata.Datatype;
import org.olap4j.metadata.Property;
import org.olap4j.metadata.Property.StandardCellProperty;
import org.olap4j.metadata.Property.StandardMemberProperty;

import java.sql.SQLException;
import java.util.*;

public abstract class MDDataSet implements QueryResult {

    protected final CellSet cellSet;

    private final OlapConnection connection;

    public static final List<Property> cellProps = Arrays.asList(rename(StandardCellProperty.VALUE, "Value"),
            rename(StandardCellProperty.FORMATTED_VALUE, "FmtValue"),
            rename(StandardCellProperty.FORMAT_STRING, "FormatString"));

    public static final List<StandardCellProperty> cellPropLongs = Arrays.asList(StandardCellProperty.VALUE,
            StandardCellProperty.FORMATTED_VALUE, StandardCellProperty.FORMAT_STRING);

    protected static final List<Property> defaultProps = Arrays.asList(
            rename(StandardMemberProperty.MEMBER_UNIQUE_NAME, "UName"),
            rename(StandardMemberProperty.MEMBER_CAPTION, "Caption"),
            rename(StandardMemberProperty.LEVEL_UNIQUE_NAME, "LName"),
            rename(StandardMemberProperty.LEVEL_NUMBER, "LNum"),
            rename(StandardMemberProperty.DISPLAY_INFO, "DisplayInfo"));

    protected static final Map<String, StandardMemberProperty> longProps = new HashMap<>();

    static {
        longProps.put("UName", StandardMemberProperty.MEMBER_UNIQUE_NAME);
        longProps.put("Caption", StandardMemberProperty.MEMBER_CAPTION);
        longProps.put("LName", StandardMemberProperty.LEVEL_UNIQUE_NAME);
        longProps.put("LNum", StandardMemberProperty.LEVEL_NUMBER);
        longProps.put("DisplayInfo", StandardMemberProperty.DISPLAY_INFO);
    }

    protected MDDataSet(CellSet cellSet, OlapConnection connection) {
        this.cellSet = cellSet;
        this.connection = connection;
    }

    /**
     * https://github.com/olap4j/olap4j-xmlaserver/issues/9 it closes nothing
     */
    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        } else {
            cellSet.getStatement().getConnection().close();
        }
    }

    private static Property rename(final Property property, final String name) {
        return new Property() {
            public Datatype getDatatype() {
                return property.getDatatype();
            }

            public Set<TypeFlag> getType() {
                return property.getType();
            }

            public ContentType getContentType() {
                return property.getContentType();
            }

            public String getName() {
                return name;
            }

            public String getUniqueName() {
                return property.getUniqueName();
            }

            public String getCaption() {
                return property.getCaption();
            }

            public String getDescription() {
                return property.getDescription();
            }

            public boolean isVisible() {
                return property.isVisible();
            }
        };
    }
}
