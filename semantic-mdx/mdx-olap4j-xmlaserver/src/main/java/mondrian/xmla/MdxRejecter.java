package mondrian.xmla;

import org.olap4j.PreparedOlapStatement;

/**
 * 预测 MDX 查询是否需要被拒绝
 */
public interface MdxRejecter {

    /**
     * 获取新的 MDX 节点地址
     *
     * @param url 当前 url
     * @return 新的 url
     */
    String redirect();

    /**
     * @param statement MDX statement
     * @return 是否拒绝继续执行 MDX
     */
    boolean isReject(PreparedOlapStatement statement);

}
