package backend.application.service;

import backend.application.port.in.ProjectUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.project.ProjectRepositoryPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.domain.folder.model.Folder;
import backend.domain.project.model.Project;
import backend.domain.user.model.User;
import backend.domain.user.model.UserId;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
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
    private final UserRepositoryPort userRepository;
    private final FolderRepositoryPort folderRepository;
    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;

    @Override
    public Mono<Void> createProject(CreateProjectCommand command) {
        return getCurrentUser()
                .flatMap(user ->
                        getFolderMono(command).flatMap(folder ->
                                isWorkspaceMember(folder.getWorkspaceId(), user)
                                        .flatMap(isMember -> {

                                            var project = Project.builder()
                                                    .folderId(folder.getId().getValue())
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

    private Mono<User> getCurrentUser() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> {
                    return userRepository.findById(UserId.of(userId))
                            .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)));
                });
    }

    private Mono<Boolean> isWorkspaceMember(Long workspaceId, User user) {
        return workspaceMemberRepository.existsByUserIdAndWorkspaceId(workspaceId, user.getId().getValue())
                .flatMap(isMember -> {
                    if (!isMember) {
                        return Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
                    }
                    return Mono.just(true);
                });
    }
}
