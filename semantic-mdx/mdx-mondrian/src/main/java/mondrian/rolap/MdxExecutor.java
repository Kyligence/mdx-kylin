package mondrian.rolap;

import mondrian.olap.Result;
import mondrian.server.Execution;

public interface MdxExecutor {

    Result execute(Execution execution) throws Exception;

}
