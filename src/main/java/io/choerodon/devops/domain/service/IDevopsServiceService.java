package io.choerodon.devops.domain.service;

/**
 * Created by Zenger on 2018/4/20.
 */
public interface IDevopsServiceService {

    void deploy(String serviceYaml, String name, String namespace);

    void delete(String name, String namespace);
}
