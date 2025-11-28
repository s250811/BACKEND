package backend.application.service;

import backend.application.port.in.folder.FolderUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.application.port.out.workspace.WorkspaceRepositoryPort;
import backend.domain.folder.model.Folder;
import backend.domain.workspace.model.Workspace;
import backend.exception.folder.FolderErrorCode;
import backend.exception.folder.FolderException;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import backend.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FolderService implements FolderUseCase {

    private final WorkspaceRepositoryPort workspaceRepository;
    private final FolderRepositoryPort folderRepository;
    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;

    @Override
    public Mono<Void> createFolder(CreateFolderCommand command) {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> {
                    Mono<Workspace> workspaceMono = getWorkspace(command.workspaceId());
                    Mono<Boolean> isMemberMono = isWorkspaceMember(command.workspaceId(), userId);

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

        return SecurityUtils.getCurrentUserId()
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
                .switchIfEmpty(Mono.error(new FolderException(FolderErrorCode.FOLDER_NOT_FOUND)));
    }

    private Mono<Boolean> isWorkspaceMember(Long workspaceId, Long userId) {
        return workspaceMemberRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)
                .defaultIfEmpty(false);
    }

    private Mono<Workspace> getWorkspace(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND)));
    }
}
