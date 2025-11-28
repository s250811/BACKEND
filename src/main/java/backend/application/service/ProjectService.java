package backend.application.service;

import backend.application.port.in.project.ProjectUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.project.ProjectRepositoryPort;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.domain.folder.model.Folder;
import backend.domain.project.model.Project;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import backend.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService implements ProjectUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final FolderRepositoryPort folderRepository;
    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;

    @Override
    public Mono<Void> createProject(CreateProjectCommand command) {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId ->
                        getFolderMono(command).flatMap(folder ->
                                isWorkspaceMember(folder.getWorkspaceId(), userId)
                                        .flatMap(isMember -> {

                                            var project = Project.builder()
                                                    .folderId(folder.getId().value())
                                                    .projectName(command.projectName())
                                                    .description(command.description())
                                                    .build();

                                            return projectRepository.save(project);
                                        })
                        )
                )
                .then();
    }

    private Mono<Folder> getFolderMono(CreateProjectCommand command) {
        return folderRepository.findById(command.folderId())
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.FOLDER_NOT_FOUND)));
    }

    private Mono<Boolean> isWorkspaceMember(Long workspaceId, Long userId) {
        return workspaceMemberRepository.existsByUserIdAndWorkspaceId(workspaceId, userId)
                .flatMap(isMember -> {
                    if (!isMember) {
                        return Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
                    }
                    return Mono.just(true);
                });
    }
}
