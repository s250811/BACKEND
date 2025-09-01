package backend.application.service;

import backend.application.port.in.FolderUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.application.port.out.workspace.WorkspaceRepositoryPort;
import backend.domain.folder.model.Folder;
import backend.domain.user.model.User;
import backend.domain.user.model.UserId;
import backend.domain.workspace.model.Workspace;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FolderService implements FolderUseCase {

    private final WorkspaceRepositoryPort workspaceRepository;
    private final FolderRepositoryPort folderRepository;
    private final UserRepositoryPort userRepository;
    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;

    @Override
    public Mono<Void> createFolder(CreateFolderCommand command) {
        return getCurrentUser()
                .flatMap(user -> {
                    Mono<Workspace> workspaceMono = getWorkspace(command.workspaceId());
                    Mono<Boolean> isMemberMono = isWorkspaceMember(command.workspaceId(), user);

                    return Mono.zip(workspaceMono, isMemberMono)
                            .flatMap(zip -> {
                                Boolean isMember = zip.getT2();

                                if (!isMember) {
                                    return Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
                                }

                                Folder folder = Folder.builder()
                                        .workspaceId(command.workspaceId())
                                        .folderName(command.name())
                                        .isDeleted(false)
                                        .build();

                                return folderRepository.save(folder);
                            });
                })
                .then();
    }

    @Override
    public Mono<Void> duplicateFolder(DuplicateFolderCommand command) {

        return getCurrentUser()
                .flatMap(user -> {
                    return getWorkspace(command.workspaceId())
                            .flatMap(workspace -> {
                                return isWorkspaceMember(command.workspaceId(), user)
                                        .flatMap(isMember -> {
                                            if (!isMember) {
                                                return Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
                                            }
                                            return getFolderMono(command)
                                                    .flatMap(existingFolder -> {
                                                        Folder duplicatedFolder = Folder.builder()
                                                                .workspaceId(command.workspaceId())
                                                                .folderName(existingFolder.getFolderName() + " - 복제본")
                                                                .isDeleted(false)
                                                                .build();
                                                        return folderRepository.save(duplicatedFolder);
                                                    });
                                        });
                            });
                })
                .then();
    }

    private Mono<Folder> getFolderMono(DuplicateFolderCommand command) {
        return folderRepository.findById(command.folderId())
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.FOLDER_NOT_FOUND)));
    }

    private Mono<Boolean> isWorkspaceMember(Long workspaceId, User user) {
        return workspaceMemberRepository.existsByUserIdAndWorkspaceId(user.getId().getValue(), workspaceId)
                .defaultIfEmpty(false);
    }

    private Mono<Workspace> getWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND)));
    }

    private Mono<User> getCurrentUser() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userIdStr -> {
                    Long userId = Long.valueOf(userIdStr);
                    return userRepository.findById(UserId.of(userId))
                            .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)));

                });
    }
}
