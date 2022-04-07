

package mondrian.rolap;

import mondrian.olap.Larder;
import mondrian.spi.MemberFormatter;
import org.olap4j.metadata.Level;

import java.util.List;

public class RolapDimensionAttribute extends RolapAttributeImpl {

    private RolapDimension dimension;

    public String subfolder;

    public RolapDimensionAttribute(String name,
                                   boolean visible,
                                   List<RolapSchema.PhysColumn> keyList,
                                   RolapSchema.PhysColumn nameExp,
                                   RolapSchema.PhysColumn captionExp,
                                   List<RolapSchema.PhysColumn> orderByList,
                                   RolapSchema.PhysColumn valueExp,
                                   MemberFormatter memberFormatter,
                                   Level.Type levelType,
                                   int approxRowCount,
                                   Larder larder,
                                   RolapDimension dimension, String subfolder) {
        super(name, visible, keyList, nameExp, captionExp, orderByList, valueExp, memberFormatter, levelType, approxRowCount, larder);
        this.dimension = dimension;
        this.subfolder = subfolder;
    }

    @Override
    public RolapDimension getDimension() {
        return this.dimension;
    }

    @Override
    public RolapAttribute cloneWithNewKeyCols(List<RolapSchema.PhysColumn> keyList, List<RolapSchema.PhysColumn> orderByList) {
        RolapDimensionAttribute rolapDimensionAttribute = new RolapDimensionAttribute(this.name, this.visible, keyList, this.nameExp, this.captionExp, orderByList,
                valueExp, this.memberFormatter, this.levelType, this.getApproxRowCount(), this.getLarder(), this.dimension, this.subfolder);
        rolapDimensionAttribute.getExplicitProperties().addAll(this.getExplicitProperties());
        return rolapDimensionAttribute;
    }
}
