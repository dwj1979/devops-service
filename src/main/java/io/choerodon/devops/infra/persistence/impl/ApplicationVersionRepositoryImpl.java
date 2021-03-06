package io.choerodon.devops.infra.persistence.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.kubernetes.client.JSON;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.convertor.ConvertPageHelper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.domain.application.entity.ApplicationVersionE;
import io.choerodon.devops.domain.application.repository.ApplicationVersionRepository;
import io.choerodon.devops.infra.common.util.TypeUtil;
import io.choerodon.devops.infra.dataobject.ApplicationLatestVersionDO;
import io.choerodon.devops.infra.dataobject.ApplicationVersionDO;
import io.choerodon.devops.infra.mapper.ApplicationVersionMapper;
import io.choerodon.mybatis.pagehelper.PageHelper;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;

/**
 * Created by Zenger on 2018/4/3.
 */
@Service
public class ApplicationVersionRepositoryImpl implements ApplicationVersionRepository {

    private static final String APPCODE = "appCode";
    private static final String APPNAME = "appName";
    private static JSON json = new JSON();
    private ApplicationVersionMapper applicationVersionMapper;

    public ApplicationVersionRepositoryImpl(ApplicationVersionMapper applicationVersionMapper) {
        this.applicationVersionMapper = applicationVersionMapper;
    }

    @Override
    public Page<ApplicationVersionE> listApplicationVersion(Long projectId, PageRequest pageRequest, String searchParam) {
        if (pageRequest.getSort() != null) {
            Map<String, String> map = new HashMap<>();
            map.put("version", "dav.version");
            map.put(APPCODE, APPCODE);
            map.put(APPNAME, APPNAME);
            map.put("creationDate", "dav.creation_date");
            pageRequest.resetOrder("dav", map);
        }

        Page<ApplicationVersionDO> applicationVersionQueryDOPage;
        if (!StringUtils.isEmpty(searchParam)) {
            Map<String, Object> searchParamMap = json.deserialize(searchParam, Map.class);
            applicationVersionQueryDOPage = PageHelper.doPageAndSort(
                    pageRequest, () -> applicationVersionMapper.listApplicationVersion(
                            projectId,
                            null,
                            TypeUtil.cast(searchParamMap.get(TypeUtil.SEARCH_PARAM)),
                            TypeUtil.cast(searchParamMap.get(TypeUtil.PARAM))));
        } else {
            applicationVersionQueryDOPage = PageHelper.doPageAndSort(
                    pageRequest, () -> applicationVersionMapper.listApplicationVersion(projectId, null, null, null));
        }
        return ConvertPageHelper.convertPage(applicationVersionQueryDOPage, ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationLatestVersionDO> listAppLatestVersion(Long projectId) {
        return applicationVersionMapper.listAppLatestVersion(projectId);
    }

    @Override
    public ApplicationVersionE create(ApplicationVersionE applicationVersionE) {
        ApplicationVersionDO applicationVersionDO =
                ConvertHelper.convert(applicationVersionE, ApplicationVersionDO.class);
        if (applicationVersionMapper.insert(applicationVersionDO) != 1) {
            throw new CommonException("error.application.insert");
        }
        return ConvertHelper.convert(applicationVersionDO, ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationVersionE> listByAppId(Long appId) {
        List<ApplicationVersionDO> applicationVersionDOS = applicationVersionMapper.selectByAppId(appId);
        if (applicationVersionDOS.isEmpty()) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(applicationVersionDOS, ApplicationVersionE.class);
    }

    @Override
    public ApplicationVersionE query(Long appVersionId) {
        return ConvertHelper.convert(
                applicationVersionMapper.selectByPrimaryKey(appVersionId), ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationVersionE> listByAppIdAndEnvId(Long projectId, Long appId, Long envId) {
        return ConvertHelper.convertList(
                applicationVersionMapper.listByAppIdAndEnvId(projectId, appId, envId), ApplicationVersionE.class);
    }

    @Override
    public String queryValue(Long versionId) {
        return applicationVersionMapper.queryValue(versionId);
    }

    @Override
    public ApplicationVersionE queryByAppAndVersion(Long appId, String version) {
        ApplicationVersionDO applicationVersionDO = new ApplicationVersionDO();
        applicationVersionDO.setAppId(appId);
        applicationVersionDO.setVersion(version);
        List<ApplicationVersionDO> applicationVersionDOS = applicationVersionMapper.select(applicationVersionDO);
        if (applicationVersionDOS.isEmpty()) {
            return null;
        }
        return ConvertHelper.convert(applicationVersionDOS.get(0), ApplicationVersionE.class);
    }

    @Override
    public void updatePublishLevelByIds(List<Long> appVersionIds, Long level) {
        ApplicationVersionDO applicationVersionDO = new ApplicationVersionDO();
        applicationVersionDO.setIsPublish(level);
        for (Long id : appVersionIds) {
            applicationVersionDO.setId(id);
            applicationVersionMapper.updateByPrimaryKeySelective(applicationVersionDO);
        }
    }

    @Override
    public Page<ApplicationVersionE> listApplicationVersionInApp(Long projectId, Long appId, PageRequest pageRequest, String searchParam) {
        if (pageRequest.getSort() != null) {
            Map<String, String> map = new HashMap<>();
            map.put("version", "dav.version");
            map.put(APPCODE, APPCODE);
            map.put(APPNAME, APPNAME);
            map.put("creationDate", "dav.creation_date");
            pageRequest.resetOrder("dav", map);
        }

        Page<ApplicationVersionDO> applicationVersionQueryDOPage;
        if (!StringUtils.isEmpty(searchParam)) {
            Map<String, Object> searchParamMap = json.deserialize(searchParam, Map.class);
            applicationVersionQueryDOPage = PageHelper.doPageAndSort(
                    pageRequest, () -> applicationVersionMapper.listApplicationVersion(
                            projectId, appId,
                            TypeUtil.cast(searchParamMap.get(TypeUtil.SEARCH_PARAM)),
                            TypeUtil.cast(searchParamMap.get(TypeUtil.PARAM))));
        } else {
            applicationVersionQueryDOPage = PageHelper.doPageAndSort(
                    pageRequest, () -> applicationVersionMapper.listApplicationVersion(projectId, appId, null, null));
        }
        return ConvertPageHelper.convertPage(applicationVersionQueryDOPage, ApplicationVersionE.class);
    }

    @Override
    public List<ApplicationVersionE> listAllPublishedVersion(Long applicationId) {
        List<ApplicationVersionDO> applicationVersionDOList = applicationVersionMapper
                .getAllPublishedVersion(applicationId);
        return ConvertHelper.convertList(applicationVersionDOList, ApplicationVersionE.class);
    }

}
