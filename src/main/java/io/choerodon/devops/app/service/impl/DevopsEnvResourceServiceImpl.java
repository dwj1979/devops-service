package io.choerodon.devops.app.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import io.kubernetes.client.JSON;
import io.kubernetes.client.models.*;
import org.springframework.stereotype.Service;

import io.choerodon.devops.api.dto.*;
import io.choerodon.devops.app.service.DevopsEnvResourceService;
import io.choerodon.devops.domain.application.entity.*;
import io.choerodon.devops.domain.application.repository.*;
import io.choerodon.devops.infra.common.util.K8sUtil;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.common.util.enums.ObjectType;
import io.choerodon.devops.infra.common.util.enums.ResourceType;

/**
 * Created by younger on 2018/4/25.
 */
@Service
public class DevopsEnvResourceServiceImpl implements DevopsEnvResourceService {

    private static JSON json = new JSON();

    private DevopsEnvResourceRepository devopsEnvResourceRepository;
    private DevopsEnvResourceDetailRepository devopsEnvResourceDetailRepository;
    private DevopsEnvCommandLogRepository devopsEnvCommandLogRepository;
    private DevopsServiceRepository devopsServiceRepository;
    private DevopsEnvCommandRepository devopsEnvCommandRepository;
    private DevopsIngressRepository devopsIngressRepository;

    /**
     * 构造函数
     */
    public DevopsEnvResourceServiceImpl(DevopsEnvResourceRepository devopsEnvResourceRepository,
                                        DevopsEnvResourceDetailRepository devopsEnvResourceDetailRepository,
                                        DevopsEnvCommandLogRepository devopsEnvCommandLogRepository,
                                        DevopsServiceRepository devopsServiceRepository,
                                        DevopsIngressRepository devopsIngressRepository,
                                        DevopsEnvCommandRepository devopsEnvCommandRepository) {
        this.devopsEnvCommandLogRepository = devopsEnvCommandLogRepository;
        this.devopsEnvResourceDetailRepository = devopsEnvResourceDetailRepository;
        this.devopsEnvResourceRepository = devopsEnvResourceRepository;
        this.devopsServiceRepository = devopsServiceRepository;
        this.devopsEnvCommandRepository = devopsEnvCommandRepository;
        this.devopsIngressRepository = devopsIngressRepository;
    }

    @Override
    public DevopsEnvResourceDTO listResources(Long instanceId) {
        List<DevopsEnvResourceE> devopsEnvResourceES =
                devopsEnvResourceRepository.listByInstanceId(instanceId);
        DevopsEnvResourceDTO devopsEnvResourceDTO = new DevopsEnvResourceDTO();
        if (devopsEnvResourceES == null) {
            return devopsEnvResourceDTO;
        }
        devopsEnvResourceES.parallelStream().forEach(devopsInstanceResourceE -> {
            DevopsEnvResourceDetailE devopsEnvResourceDetailE =
                    devopsEnvResourceDetailRepository.query(
                            devopsInstanceResourceE.getDevopsEnvResourceDetailE().getId());
            ResourceType resourceType = ResourceType.forString(devopsInstanceResourceE.getKind());
            switch (resourceType) {
                case POD:
                    V1Pod v1Pod = json.deserialize(devopsEnvResourceDetailE.getMessage(), V1Pod.class);
                    addPodToResource(devopsEnvResourceDTO, v1Pod);
                    break;
                case DEPLOYMENT:
                    V1beta2Deployment v1beta2Deployment = json.deserialize(
                            devopsEnvResourceDetailE.getMessage(),
                            V1beta2Deployment.class);
                    addDeploymentToResource(devopsEnvResourceDTO, v1beta2Deployment);
                    break;
                case SERVICE:
                    V1Service v1Service = json.deserialize(devopsEnvResourceDetailE.getMessage(),
                            V1Service.class);
                    DevopsServiceE devopsServiceE = devopsServiceRepository.selectByNameAndNamespace(
                            devopsInstanceResourceE.getName(), v1Service.getMetadata().getNamespace());
                    if (devopsServiceE != null) {
                        List<String> domainNames =
                                devopsIngressRepository.queryIngressNameByServiceId(
                                        devopsServiceE.getId());
                        domainNames.parallelStream().forEach(domainName -> {
                            DevopsEnvResourceE devopsEnvResourceE1 =
                                    devopsEnvResourceRepository.queryByInstanceIdAndKindAndName(
                                            null,
                                            "Ingress",
                                            domainName);
                            if (devopsEnvResourceE1 != null) {
                                DevopsEnvResourceDetailE devopsEnvResourceDetailE1 =
                                        devopsEnvResourceDetailRepository.query(
                                                devopsEnvResourceE1.getDevopsEnvResourceDetailE().getId());
                                V1beta1Ingress v1beta1Ingress = json.deserialize(
                                        devopsEnvResourceDetailE1.getMessage(),
                                        V1beta1Ingress.class);
                                addIngressToResource(devopsEnvResourceDTO, v1beta1Ingress);
                            }
                        });
                    }
                    addServiceToResource(devopsEnvResourceDTO, v1Service);
                    break;
                case REPLICASET:
                    V1beta2ReplicaSet v1beta2ReplicaSet = json.deserialize(
                            devopsEnvResourceDetailE.getMessage(),
                            V1beta2ReplicaSet.class);
                    addReplicaSetToResource(devopsEnvResourceDTO, v1beta2ReplicaSet);
                    break;
                default:
                    break;
            }
        });
        return devopsEnvResourceDTO;
    }

    @Override
    public List<InstanceStageDTO> listStages(Long instanceId) {
        DevopsEnvCommandE devopsEnvCommandE = devopsEnvCommandRepository
                .queryByObject(ObjectType.INSTANCE.getObjectType(), instanceId);
        List<DevopsEnvResourceE> devopsEnvResourceES =
                devopsEnvResourceRepository.listJobByInstanceId(instanceId);
        List<InstanceStageDTO> instanceStageDTOS = new ArrayList<>();
        devopsEnvResourceES.stream().forEach(devopsInstanceResourceE -> {
            if (devopsInstanceResourceE.getKind().equals(ResourceType.JOB.getType())) {
                DevopsEnvResourceDetailE devopsEnvResourceDetailE =
                        devopsEnvResourceDetailRepository.query(
                                devopsInstanceResourceE.getDevopsEnvResourceDetailE().getId());
                V1Job v1Job = json.deserialize(devopsEnvResourceDetailE.getMessage(), V1Job.class);
                InstanceStageDTO instanceStageDTO = new InstanceStageDTO();
                instanceStageDTO.setStageName(v1Job.getMetadata().getName());
                instanceStageDTO.setWeight(devopsInstanceResourceE.getWeight());
                if (v1Job.getStatus() != null) {
                    if (v1Job.getStatus().getSucceeded() != null) {
                        if (v1Job.getStatus().getSucceeded() == 1) {
                            instanceStageDTO.setStatus("success");
                            instanceStageDTO.setStageTime(
                                    getStageTime(new Timestamp(v1Job.getStatus().getStartTime().toDate().getTime()),
                                            new Timestamp(v1Job.getStatus().getCompletionTime().toDate().getTime())));
                        } else {
                            instanceStageDTO.setStatus("fail");
                        }
                    }
                }
                instanceStageDTOS.add(instanceStageDTO);
            }
        });
        List<DevopsEnvCommandLogE> devopsEnvCommandLogES = devopsEnvCommandLogRepository
                .queryByDeployId(devopsEnvCommandE.getId());
        for (int i = 0; i < devopsEnvCommandLogES.size(); i++) {
            DevopsEnvCommandLogE devopsEnvCommandLogE = devopsEnvCommandLogES.get(i);
            InstanceStageDTO instanceStageDTO = instanceStageDTOS.get(i);
            instanceStageDTO.setLog(devopsEnvCommandLogE.getLog());
        }
        return instanceStageDTOS;
    }

    /**
     * 增加pod资源
     *
     * @param devopsEnvResourceDTO 实例资源参数
     * @param v1Pod                pod对象
     */
    public void addPodToResource(DevopsEnvResourceDTO devopsEnvResourceDTO, V1Pod v1Pod) {
        PodDTO podDTO = new PodDTO();
        podDTO.setName(v1Pod.getMetadata().getName());
        podDTO.setDesire(TypeUtil.objToLong(v1Pod.getSpec().getContainers().size()));
        Long ready = 0L;
        Long restart = 0L;
        for (V1ContainerStatus v1ContainerStatus : v1Pod.getStatus().getContainerStatuses()) {
            if (v1ContainerStatus.isReady() && v1ContainerStatus.getState().getRunning().getStartedAt() != null) {
                ready = ready + 1;
            }
            restart = restart + v1ContainerStatus.getRestartCount();
        }
        podDTO.setReady(ready);
        podDTO.setStatus(K8sUtil.changePodStatus(v1Pod));
        podDTO.setRestarts(restart);
        podDTO.setAge(v1Pod.getMetadata().getCreationTimestamp().toString());
        devopsEnvResourceDTO.getPodDTOS().add(podDTO);
    }

    /**
     * 增加deployment资源
     *
     * @param devopsEnvResourceDTO 实例资源参数
     * @param v1beta2Deployment    deployment对象
     */
    public void addDeploymentToResource(DevopsEnvResourceDTO devopsEnvResourceDTO, V1beta2Deployment v1beta2Deployment) {
        DeploymentDTO deploymentDTO = new DeploymentDTO();
        deploymentDTO.setName(v1beta2Deployment.getMetadata().getName());
        deploymentDTO.setDesired(TypeUtil.objToLong(v1beta2Deployment.getSpec().getReplicas()));
        deploymentDTO.setCurrent(TypeUtil.objToLong(v1beta2Deployment.getStatus().getReplicas()));
        deploymentDTO.setUpToDate(TypeUtil.objToLong(v1beta2Deployment.getStatus().getUpdatedReplicas()));
        deploymentDTO.setAvailable(TypeUtil.objToLong(v1beta2Deployment.getStatus().getAvailableReplicas()));
        deploymentDTO.setAge(v1beta2Deployment.getMetadata().getCreationTimestamp().toString());
        devopsEnvResourceDTO.getDeploymentDTOS().add(deploymentDTO);
    }

    /**
     * 增加service资源
     *
     * @param devopsEnvResourceDTO 实例资源参数
     * @param v1Service            service对象
     */
    public void addServiceToResource(DevopsEnvResourceDTO devopsEnvResourceDTO, V1Service v1Service) {
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setName(v1Service.getMetadata().getName());
        serviceDTO.setType(v1Service.getSpec().getType());
        if (v1Service.getSpec().getClusterIP().length() == 0) {
            serviceDTO.setClusterIp("<none>");
        } else {
            serviceDTO.setClusterIp(v1Service.getSpec().getClusterIP());
        }
        serviceDTO.setExternalIp(K8sUtil.getServiceExternalIp(v1Service));
        String port = K8sUtil.makePortString(v1Service.getSpec().getPorts());
        if (port.length() == 0) {
            port = "<none>";
        }
        serviceDTO.setPort(port);
        serviceDTO.setAge(v1Service.getMetadata().getCreationTimestamp().toString());
        devopsEnvResourceDTO.getServiceDTOS().add(serviceDTO);
    }

    /**
     * 增加ingress资源
     *
     * @param devopsEnvResourceDTO 实例资源参数
     * @param v1beta1Ingress       ingress对象
     */
    public void addIngressToResource(DevopsEnvResourceDTO devopsEnvResourceDTO, V1beta1Ingress v1beta1Ingress) {
        IngressDTO ingressDTO = new IngressDTO();
        ingressDTO.setName(v1beta1Ingress.getMetadata().getName());
        ingressDTO.setHosts(K8sUtil.formatHosts(v1beta1Ingress.getSpec().getRules()));
        ingressDTO.setPorts(K8sUtil.formatPorts(v1beta1Ingress.getSpec().getTls()));
        ingressDTO.setAddress(K8sUtil.loadBalancerStatusStringer(v1beta1Ingress.getStatus().getLoadBalancer()));
        ingressDTO.setAge(v1beta1Ingress.getMetadata().getCreationTimestamp().toString());
        devopsEnvResourceDTO.getIngressDTOS().add(ingressDTO);
    }

    /**
     * 增加replicaSet资源
     *
     * @param devopsEnvResourceDTO 实例资源参数
     * @param v1beta2ReplicaSet    replicaSet对象
     */
    public void addReplicaSetToResource(DevopsEnvResourceDTO devopsEnvResourceDTO, V1beta2ReplicaSet v1beta2ReplicaSet) {
        if (v1beta2ReplicaSet.getSpec().getReplicas() == 0) {
            return;
        }
        ReplicaSetDTO replicaSetDTO = new ReplicaSetDTO();
        replicaSetDTO.setName(v1beta2ReplicaSet.getMetadata().getName());
        replicaSetDTO.setCurrent(TypeUtil.objToLong(v1beta2ReplicaSet.getStatus().getReplicas()));
        replicaSetDTO.setDesired(TypeUtil.objToLong(v1beta2ReplicaSet.getSpec().getReplicas()));
        replicaSetDTO.setReady(TypeUtil.objToLong(v1beta2ReplicaSet.getStatus().getReadyReplicas()));
        replicaSetDTO.setAge(v1beta2ReplicaSet.getMetadata().getCreationTimestamp().toString());
        devopsEnvResourceDTO.getReplicaSetDTOS().add(replicaSetDTO);
    }

    /**
     * 获取时间间隔
     *
     * @param ttime1 起始时间
     * @param ttime2 结束时间
     * @return long[]
     */
    public Long[] getStageTime(Timestamp ttime1, Timestamp ttime2) {
        long day = 0;
        long hour = 0;
        long min = 0;
        long sec = 0;
        long time1 = ttime1.getTime();
        long time2 = ttime2.getTime();
        long diff;
        if (time1 < time2) {
            diff = time2 - time1;
        } else {
            diff = time1 - time2;
        }
        day = diff / (24 * 60 * 60 * 1000);
        hour = (diff / (60 * 60 * 1000) - day * 24);
        min = ((diff / (60 * 1000)) - day * 24 * 60 - hour * 60);
        sec = (diff / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
        Long[] times = {day, hour, min, sec};
        return times;
    }
}
