package io.choerodon.devops.infra.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import io.choerodon.devops.infra.dataobject.DevopsAppMarketDO;
import io.choerodon.mybatis.common.BaseMapper;

/**
 * Created by ernst on 2018/5/12.
 */
public interface ApplicationMarketMapper extends BaseMapper<DevopsAppMarketDO> {
    List<DevopsAppMarketDO> listMarketApplicationInProject(@Param("projectId") Long projectId,
                                                           @Param("searchParam") Map<String, Object> searchParam,
                                                           @Param("param") String param);

    List<DevopsAppMarketDO> listMarketApplication(@Param("projectIds") List projectIds,
                                                  @Param("searchParam") Map<String, Object> searchParam,
                                                  @Param("param") String param);
}
