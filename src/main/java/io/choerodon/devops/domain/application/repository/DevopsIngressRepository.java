package io.choerodon.devops.domain.application.repository;

import java.util.List;

import io.choerodon.core.domain.Page;
import io.choerodon.devops.api.dto.DevopsIngressDTO;
import io.choerodon.devops.domain.application.entity.DevopsIngressE;
import io.choerodon.devops.domain.application.entity.DevopsIngressPathE;
import io.choerodon.devops.infra.dataobject.DevopsIngressDO;
import io.choerodon.devops.infra.dataobject.DevopsIngressPathDO;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * Creator: Runge
 * Date: 2018/4/20
 * Time: 16:06
 * Description:
 */
public interface DevopsIngressRepository {
    void createIngress(DevopsIngressDO devopsIngressDO, List<DevopsIngressPathDO> devopsIngressPathDOList);

    void updateIngress(DevopsIngressDO devopsIngressDO, List<DevopsIngressPathDO> devopsIngressPathDOList);

    Page<DevopsIngressDTO> getIngress(Long projectId, PageRequest pageRequest, String params);

    DevopsIngressDTO getIngress(Long projectId, Long ingressId);

    DevopsIngressDO getIngress(Long ingressId);

    void deleteIngress(Long ingressId);

    void setUsable(String name);

    List<String> queryIngressNameByServiceId(Long serviceId);

    Boolean checkIngressName(Long envId, String name);

    Boolean checkIngressAndPath(String domain, String path);

    DevopsIngressE selectByEnvAndName(Long envId, String name);

    DevopsIngressE insertIngress(DevopsIngressE devopsIngressE);

    void insertIngressPath(DevopsIngressPathE devopsIngressPathE);

    List<DevopsIngressPathE> selectByEnvIdAndServiceName(Long envId, String serviceName);

    List<DevopsIngressPathE> selectByEnvIdAndServiceId(Long envId, Long serviceId);

    List<DevopsIngressPathE> selectByIngressId(Long ingressId);

    void updateIngressPath(DevopsIngressPathE devopsIngressPathE);

    void deleteIngressPath(Long ingressId);
}
