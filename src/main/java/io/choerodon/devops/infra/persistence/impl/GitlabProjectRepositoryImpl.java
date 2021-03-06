package io.choerodon.devops.infra.persistence.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.devops.domain.application.entity.gitlab.BranchE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabCommitE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabJobE;
import io.choerodon.devops.domain.application.entity.gitlab.GitlabPipelineE;
import io.choerodon.devops.domain.application.repository.GitlabProjectRepository;
import io.choerodon.devops.infra.dataobject.gitlab.BranchDO;
import io.choerodon.devops.infra.dataobject.gitlab.CommitDO;
import io.choerodon.devops.infra.dataobject.gitlab.JobDO;
import io.choerodon.devops.infra.dataobject.gitlab.PipelineDO;
import io.choerodon.devops.infra.feign.GitlabServiceClient;

/**
 * Created by Zenger on 2018/4/9.
 */
@Component
public class GitlabProjectRepositoryImpl implements GitlabProjectRepository {

    private GitlabServiceClient gitlabServiceClient;

    public GitlabProjectRepositoryImpl(GitlabServiceClient gitlabServiceClient) {
        this.gitlabServiceClient = gitlabServiceClient;
    }

    @Override
    public List<GitlabPipelineE> listPipeline(Integer projectId) {
        ResponseEntity<List<PipelineDO>> responseEntity = gitlabServiceClient.listPipeline(projectId);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            return new ArrayList<>();
        }
        return ConvertHelper.convertList(responseEntity.getBody(), GitlabPipelineE.class);
    }

    @Override
    public List<GitlabPipelineE> listPipelines(Integer projectId, Integer page, Integer size) {
        ResponseEntity<List<PipelineDO>> responseEntity = gitlabServiceClient.listPipelines(projectId, page, size);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            return new ArrayList<>();
        }
        return ConvertHelper.convertList(responseEntity.getBody(), GitlabPipelineE.class);
    }

    @Override
    public GitlabPipelineE getPipeline(Integer projectId, Integer pipelineId, String userName) {
        ResponseEntity<PipelineDO> responseEntity = gitlabServiceClient.getPipeline(projectId, pipelineId, userName);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            return null;
        }
        return ConvertHelper.convert(responseEntity.getBody(), GitlabPipelineE.class);
    }

    @Override
    public GitlabCommitE getCommit(Integer projectId, String sha, String userName) {
        ResponseEntity<CommitDO> responseEntity = gitlabServiceClient.getCommit(projectId, sha, userName);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            return null;
        }
        return ConvertHelper.convert(responseEntity.getBody(), GitlabCommitE.class);
    }

    @Override
    public List<GitlabJobE> listJobs(Integer projectId, Integer pipelineId, String userName) {
        ResponseEntity<List<JobDO>> responseEntity = gitlabServiceClient.listJobs(projectId, pipelineId, userName);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            return new ArrayList<>();
        }
        return ConvertHelper.convertList(responseEntity.getBody(), GitlabJobE.class);
    }

    @Override
    public Boolean retry(Integer projectId, Integer pipelineId, String userName) {
        ResponseEntity<PipelineDO> responseEntity = gitlabServiceClient.retry(projectId, pipelineId, userName);
        return HttpStatus.OK.equals(responseEntity.getStatusCode()) ? true : false;
    }

    @Override
    public Boolean cancel(Integer projectId, Integer pipelineId, String userName) {
        ResponseEntity<PipelineDO> responseEntity = gitlabServiceClient.cancel(projectId, pipelineId, userName);
        return HttpStatus.OK.equals(responseEntity.getStatusCode()) ? true : false;
    }

    @Override
    public List<BranchE> listBranches(Integer projectId) {
        ResponseEntity<List<BranchDO>> responseEntity = gitlabServiceClient.listBranches(projectId);
        if (!HttpStatus.OK.equals(responseEntity.getStatusCode())) {
            return Collections.emptyList();
        }
        return ConvertHelper.convertList(responseEntity.getBody(), BranchE.class);
    }
}
