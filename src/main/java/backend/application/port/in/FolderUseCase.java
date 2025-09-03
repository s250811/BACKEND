package backend.application.port.in;

import reactor.core.publisher.Mono;

public interface FolderUseCase {
    record CreateFolderCommand(Long workspaceId,String name) { }
    record DuplicateFolderCommand(Long folderId, Long workspaceId) { }

    Mono<Void> createFolder(CreateFolderCommand command);
    Mono<Void> duplicateFolder(DuplicateFolderCommand command);
}
