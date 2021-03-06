package io.choerodon.devops.api.dto;

import java.util.List;

/**
 * Created by ernst on 2018/5/12.
 */
public class ApplicationReleasingDTO {

    private Long id;
    private Long appId;
    private String name;
    private String code;
    private String publishLevel;
    private List<ApplicationVersionRepDTO> appVersions;
    private String imgUrl;
    private String description;
    private String contributor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPublishLevel() {
        return publishLevel;
    }

    public void setPublishLevel(String publishLevel) {
        this.publishLevel = publishLevel;
    }

    public List<ApplicationVersionRepDTO> getAppVersions() {
        return appVersions;
    }

    public void setAppVersions(List<ApplicationVersionRepDTO> appVersions) {
        this.appVersions = appVersions;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }
}
