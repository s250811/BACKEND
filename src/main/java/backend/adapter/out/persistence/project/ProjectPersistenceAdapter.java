package backend.adapter.out.persistence.project;

import backend.application.port.out.project.ProjectRepositoryPort;
import backend.domain.folder.model.FolderId;
import backend.domain.project.model.Project;
import backend.domain.project.model.ProjectId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectRepositoryPort {

    private final ProjectR2dbcRepository projectR2dbcRepository;

    @Override
    public Mono<Project> save(Project project) {
        return projectR2dbcRepository.save(toEntity(project))
                .map(ProjectPersistenceAdapter::toDomain);
    }

    @Override
    public Mono<Project> findByFolderId(Long folderId) {
        return projectR2dbcRepository.findByFolderId(folderId)
                .map(ProjectPersistenceAdapter::toDomain);
    }

    @Override
    public Flux<Project> findAllByFolderId(Long folderId) {
        return projectR2dbcRepository.findAllByFolderId(folderId)
                .map(ProjectPersistenceAdapter::toDomain);
    }

    @Override
    public Flux<Project> findAllByFolderIdIn(List<FolderId> folderIds) {
        List<Long> ids = folderIds.stream().map(FolderId::getValue).toList();
        return projectR2dbcRepository.findAllByFolderIdIn(ids)
                .map(ProjectPersistenceAdapter::toDomain);
    }

    @Override
    public Mono<Project> findById(Long projectId) {
        return projectR2dbcRepository.findById(projectId)
                .map(ProjectPersistenceAdapter::toDomain);
    }

    public static ProjectEntity toEntity(Project project) {
        return ProjectEntity.builder()
                .id(project.getIdValue())
                .folderId(project.getFolderId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public static Project toDomain(ProjectEntity entity) {
        return Project.builder()
                .id(ProjectId.of(entity.getId()))
                .folderId(entity.getFolderId())
                .projectName(entity.getProjectName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
