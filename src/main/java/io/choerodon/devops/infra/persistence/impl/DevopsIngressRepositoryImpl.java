package io.choerodon.devops.infra.persistence.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.dto.DevopsIngressDTO;
import io.choerodon.devops.api.dto.DevopsIngressPathDTO;
import io.choerodon.devops.domain.application.entity.DevopsEnvironmentE;
import io.choerodon.devops.domain.application.entity.DevopsIngressE;
import io.choerodon.devops.domain.application.entity.DevopsIngressPathE;
import io.choerodon.devops.domain.application.entity.DevopsServiceE;
import io.choerodon.devops.domain.application.repository.DevopsEnvironmentRepository;
import io.choerodon.devops.domain.application.repository.DevopsIngressRepository;
import io.choerodon.devops.domain.application.repository.DevopsServiceRepository;
import io.choerodon.devops.domain.application.valueobject.DevopsServiceV;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.common.util.enums.ServiceStatus;
import io.choerodon.devops.infra.dataobject.DevopsIngressDO;
import io.choerodon.devops.infra.dataobject.DevopsIngressPathDO;
import io.choerodon.devops.infra.mapper.DevopsIngressMapper;
import io.choerodon.devops.infra.mapper.DevopsIngressPathMapper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.websocket.session.EnvListener;

/**
 * Creator: Runge
 * Date: 2018/4/20
 * Time: 16:07
 * Description:
 */
@Component
public class DevopsIngressRepositoryImpl implements DevopsIngressRepository {

    private static final Gson gson = new Gson();
    private DevopsIngressMapper devopsIngressMapper;
    private DevopsIngressPathMapper devopsIngressPathMapper;
    private DevopsEnvironmentRepository environmentRepository;
    private DevopsServiceRepository devopsServiceRepository;
    private EnvListener envListener;

    /**
     * 构造函数
     */
    public DevopsIngressRepositoryImpl(DevopsIngressMapper devopsIngressMapper,
                                       DevopsIngressPathMapper devopsIngressPathMapper,
                                       DevopsEnvironmentRepository environmentRepository,
                                       DevopsServiceRepository devopsServiceRepository,
                                       EnvListener envListener) {
        this.devopsIngressMapper = devopsIngressMapper;
        this.devopsIngressPathMapper = devopsIngressPathMapper;
        this.environmentRepository = environmentRepository;
        this.devopsServiceRepository = devopsServiceRepository;
        this.envListener = envListener;
    }

    @Override
    public void createIngress(DevopsIngressDO devopsIngressDO, List<DevopsIngressPathDO> devopsIngressPathDOList) {
        if (!checkIngressName(devopsIngressDO.getEnvId(), devopsIngressDO.getName())) {
            throw new CommonException("error.domain.name.exist");
        }
        if (!devopsIngressPathDOList.stream()
                .filter(t -> !checkIngressAndPath(devopsIngressDO.getDomain(), t.getPath()))
                .collect(Collectors.toList()).isEmpty()) {
            throw new CommonException("error.domain.path.exist");
        }
        devopsIngressMapper.insert(devopsIngressDO);
        devopsIngressPathDOList.forEach(t -> {
            t.setIngressId(devopsIngressDO.getId());
            devopsIngressPathMapper.insert(t);
        });
    }

    @Override
    public void updateIngress(DevopsIngressDO devopsIngressDO, List<DevopsIngressPathDO> devopsIngressPathDOList) {
        Long id = devopsIngressDO.getId();
        DevopsIngressDO ingressDO = devopsIngressMapper.selectByPrimaryKey(id);
        if (ingressDO == null) {
            throw new CommonException("domain.not.exist");
        }
        if (!devopsIngressDO.getName().equals(ingressDO.getName())
                && !checkIngressName(devopsIngressDO.getEnvId(), devopsIngressDO.getName())) {
            throw new CommonException("error.domain.name.exist");
        }
        if (!devopsIngressPathDOList.stream()
                .filter(t -> !id.equals(t.getIngressId())
                        && !checkIngressAndPath(devopsIngressDO.getDomain(), t.getPath()))
                .collect(Collectors.toList()).isEmpty()) {
            throw new CommonException("error.domain.path.exist");
        }
        if (!ingressDO.equals(devopsIngressDO)) {
            devopsIngressDO.setObjectVersionNumber(ingressDO.getObjectVersionNumber());
            devopsIngressMapper.updateByPrimaryKey(devopsIngressDO);
        }
        List<DevopsIngressPathDO> ingressPathList = devopsIngressPathMapper.select(new DevopsIngressPathDO(id));
        if (!devopsIngressPathDOList.equals(ingressPathList)) {
            devopsIngressPathMapper.delete(new DevopsIngressPathDO(id));
            devopsIngressPathDOList.forEach(t -> {
                t.setIngressId(id);
                devopsIngressPathMapper.insert(t);
            });
        }
    }

    @Override
    public Page<DevopsIngressDTO> getIngress(Long projectId, PageRequest pageRequest, String params) {
        List<DevopsIngressDTO> devopsIngressDTOS = new ArrayList<>();
        Map<String, Object> maps = gson.fromJson(params, Map.class);
        Map<String, Object> searchParamMap = TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM));
        String paramMap = TypeUtil.cast(maps.get(TypeUtil.PARAM));

        if (pageRequest.getSort() != null) {
            Map<String, String> map = new HashMap<>();
            map.put("envName", "de.name");
            map.put("path", "dda.path");
            pageRequest.resetOrder("dd", map);
        }

        Page<DevopsIngressDO> devopsIngressDOS =
                PageHelper.doPageAndSort(pageRequest,
                        () -> devopsIngressMapper.selectIngerss(projectId, searchParamMap, paramMap));
        Set<String> namespaces = envListener.connectedEnv();
        devopsIngressDOS.getContent().forEach(t -> {
            DevopsIngressDTO devopsIngressDTO =
                    new DevopsIngressDTO(t.getId(), t.getDomain(), t.getName(),
                            t.getEnvId(), t.getUsable(), t.getEnvName());

            for (String ns : namespaces) {
                if (ns.equals(t.getNamespace())) {
                    devopsIngressDTO.setEnvStatus(true);
                }
            }
            DevopsIngressPathDO devopsIngressPathDO = new DevopsIngressPathDO(t.getId());
            devopsIngressPathMapper.select(devopsIngressPathDO).forEach(e -> {
                getDevopsIngressDTO(devopsIngressDTO, e);
            });
            devopsIngressDTOS.add(devopsIngressDTO);
        });
        Page<DevopsIngressDTO> ingressDTOPage = new Page<>();
        BeanUtils.copyProperties(devopsIngressDOS, ingressDTOPage);
        ingressDTOPage.setContent(devopsIngressDTOS);
        return ingressDTOPage;
    }

    @Override
    public DevopsIngressDTO getIngress(Long projectId, Long ingressId) {
        DevopsIngressDO devopsIngressDO = devopsIngressMapper.selectByPrimaryKey(ingressId);
        if (devopsIngressDO != null) {
            DevopsEnvironmentE devopsEnvironmentE = environmentRepository.queryById(devopsIngressDO.getEnvId());
            DevopsIngressDTO devopsIngressDTO = new DevopsIngressDTO(
                    ingressId, devopsIngressDO.getDomain(), devopsIngressDO.getName(), devopsEnvironmentE.getId(),
                    devopsIngressDO.getUsable(), devopsEnvironmentE.getName());
            DevopsIngressPathDO devopsIngressPathDO = new DevopsIngressPathDO(ingressId);
            devopsIngressPathMapper.select(devopsIngressPathDO).forEach(e -> {
                getDevopsIngressDTO(devopsIngressDTO, e);
            });
            return devopsIngressDTO;
        }

        return null;
    }

    @Override
    public DevopsIngressDO getIngress(Long ingressId) {
        return devopsIngressMapper.selectByPrimaryKey(ingressId);
    }

    @Override
    public void deleteIngress(Long ingressId) {
        devopsIngressMapper.deleteByPrimaryKey(ingressId);
        devopsIngressPathMapper.delete(new DevopsIngressPathDO(ingressId));
    }

    @Override
    public void setUsable(String name) {
        DevopsIngressDO devopsIngressDO = devopsIngressMapper.select(new DevopsIngressDO(name)).get(0);
        devopsIngressDO.setUsable(true);
        devopsIngressMapper.updateByPrimaryKey(devopsIngressDO);
    }

    @Override
    public List<String> queryIngressNameByServiceId(Long serviceId) {
        return devopsIngressMapper.queryIngressNameByServiceId(serviceId);
    }

    @Override
    public Boolean checkIngressName(Long envId, String name) {
        DevopsIngressDO devopsIngressDO = new DevopsIngressDO(name);
        devopsIngressDO.setEnvId(envId);
        return devopsIngressMapper.select(devopsIngressDO).isEmpty();
    }

    @Override
    public Boolean checkIngressAndPath(String domain, String path) {
        return !devopsIngressPathMapper.checkDomainAndPath(domain, path);
    }

    @Override
    public DevopsIngressE selectByEnvAndName(Long envId, String name) {
        DevopsIngressDO domainDO = new DevopsIngressDO();
        domainDO.setEnvId(envId);
        domainDO.setName(name);
        return ConvertHelper.convert(devopsIngressMapper.selectOne(domainDO), DevopsIngressE.class);
    }

    @Override
    public DevopsIngressE insertIngress(DevopsIngressE devopsIngressE) {
        DevopsIngressDO ingressDO = ConvertHelper.convert(devopsIngressE, DevopsIngressDO.class);
        if (devopsIngressMapper.insert(ingressDO) != 1) {
            throw new CommonException("error.domain.insert");
        }
        return ConvertHelper.convert(ingressDO, DevopsIngressE.class);
    }

    @Override
    public void insertIngressPath(DevopsIngressPathE devopsIngressPathE) {
        if (devopsIngressPathMapper.insert(ConvertHelper.convert(devopsIngressPathE, DevopsIngressPathDO.class)) != 1) {
            throw new CommonException("error.domainAttr.insert");
        }
    }

    @Override
    public List<DevopsIngressPathE> selectByEnvIdAndServiceName(Long envId, String serviceName) {
        return ConvertHelper.convertList(
                devopsIngressPathMapper.selectByEnvIdAndServiceName(envId, serviceName),
                DevopsIngressPathE.class);
    }

    @Override
    public List<DevopsIngressPathE> selectByEnvIdAndServiceId(Long envId, Long serviceId) {
        return ConvertHelper.convertList(
                devopsIngressPathMapper.selectByEnvIdAndServiceId(envId, serviceId),
                DevopsIngressPathE.class);
    }

    @Override
    public List<DevopsIngressPathE> selectByIngressId(Long ingressId) {
        DevopsIngressPathDO devopsIngressPathDO = new DevopsIngressPathDO();
        devopsIngressPathDO.setIngressId(ingressId);
        return ConvertHelper.convertList(
                devopsIngressPathMapper.select(devopsIngressPathDO),
                DevopsIngressPathE.class);
    }

    @Override
    public void updateIngressPath(DevopsIngressPathE devopsIngressPathE) {
        if (devopsIngressPathMapper.updateByPrimaryKey(
                ConvertHelper.convert(devopsIngressPathE, DevopsIngressPathDO.class)) != 1) {
            throw new CommonException("error.domainAttr.update");
        }
    }

    @Override
    public void deleteIngressPath(Long ingressId) {
        DevopsIngressPathDO devopsIngressPathDO = new DevopsIngressPathDO();
        devopsIngressPathDO.setIngressId(ingressId);
        devopsIngressPathMapper.delete(devopsIngressPathDO);
    }

    public void getDevopsIngressDTO(DevopsIngressDTO devopsIngressDTO, DevopsIngressPathDO e) {
        DevopsServiceE devopsServiceE = devopsServiceRepository.query(e.getServiceId());
        if (devopsServiceE == null) {
            devopsIngressDTO.addDevopsIngressPathDTO(new DevopsIngressPathDTO(
                    e.getPath(), e.getServiceId(), e.getServiceName(), ServiceStatus.DELETED.getStatus()));
        } else {
            devopsIngressDTO.addDevopsIngressPathDTO(new DevopsIngressPathDTO(
                    e.getPath(), e.getServiceId(), e.getServiceName(), devopsServiceE.getStatus()));
        }
    }
}
