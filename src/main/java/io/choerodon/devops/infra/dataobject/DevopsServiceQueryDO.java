package io.choerodon.devops.infra.dataobject;

import java.util.List;

/**
 * Created by Zenger on 2018/4/19.
 */
public class DevopsServiceQueryDO {

    private Long id;
    private String name;
    private String externalIp;
    private Long port;
    private String status;
    private Long envId;
    private String envName;
    private String namespace;
    private Long appId;
    private String appName;
    private List<ServiceVersionDO> appVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExternalIp() {
        return externalIp;
    }

    public void setExternalIp(String externalIp) {
        this.externalIp = externalIp;
    }

    public Long getPort() {
        return port;
    }

    public void setPort(Long port) {
        this.port = port;
    }

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<ServiceVersionDO> getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(List<ServiceVersionDO> appVersion) {
        this.appVersion = appVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
