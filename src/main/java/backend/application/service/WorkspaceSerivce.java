package backend.application.service;

import backend.application.port.in.WorkspaceUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.project.ProjectRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.application.port.out.workspace.WorkspaceRepositoryPort;
import backend.application.service.event.workspace.WorkspaceEventService;
import backend.domain.folder.model.Folder;
import backend.domain.folder.model.FolderId;
import backend.domain.project.model.Project;
import backend.domain.project.model.ProjectId;
import backend.domain.task.model.Task;
import backend.domain.user.model.User;
import backend.domain.user.model.UserId;
import backend.domain.workspace.model.Workspace;
import backend.domain.workspaceMember.model.WorkspaceMember;
import backend.domain.workspaceMember.model.WorkspaceMemberRole;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import backend.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceSerivce implements WorkspaceUseCase {

    private final UserRepositoryPort userRepository;
    private final WorkspaceRepositoryPort workspaceRepository;
    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;
    private final FolderRepositoryPort folderRepository;
    private final ProjectRepositoryPort projectRepository;
    private final TaskRepositoryPort taskRepository;
    private final WorkspaceEventService workspaceEventService;

    /**
     * Workspace 생성 및 수정 Service layer
     * CreateWorkspaceCommand에서 workspaceId 여부를 통해서 Update인지 Craete인지 구분함
     * 서비스 레이어에서
     * @param command
     * @return
     */
    @Override
    public Mono<Void> createOrUpdateWorkspace(CreateWorkspaceCommand command) {
        return getCurrentUser()
                .flatMap(user -> {
                    if (command.isUpdateMode()) {
                        return updateWorkspace(command, user);
                    } else {
                        return createNewWorkspace(command, user);
                    }
                })
                .doOnSuccess(workspace -> log.info("워크스페이스 {}완료: id={}, name={}",
                        command.isUpdateMode() ? "수정 " : "생성 ", workspace.getId(), workspace.getWorkspaceName()))
                .doOnError(error -> log.error("워크스페이스 {} 실패: {}",
                        command.isUpdateMode() ? "수정" : "생성", error.getMessage(), error))
                .then();
    }

    public Mono<Workspace> createNewWorkspace(CreateWorkspaceCommand command, User user) {
        return isOwnerOfWorkspace(user.getId())
                .flatMap(isOwner -> {
                    if (isOwner) {
                        return Mono.error(new WorkspaceException(WorkspaceErrorCode.USER_ALREADY_OWNS_WORKSPACE));
                    }

                    Workspace newWorkspace = Workspace.builder()
                            .workspaceName(command.workspaceName())
                            .workspaceImgUrl(command.workspaceUrl())
                            .description(command.description())
                            .build();

                    return saveWorkspace(newWorkspace)
                            .flatMap(savedWorkspace -> {
                                WorkspaceMember ownerMember = WorkspaceMember.builder()
                                        .workspaceId(savedWorkspace.getId())
                                        .userId(user.getIdValue())
                                        .nickname(user.getNickname())
                                        .role(WorkspaceMemberRole.OWNER)
                                        .isDeleted(false)
                                        .build();

                                return registerWorkspaceOwner(ownerMember)
                                        .then(workspaceEventService.publishWorkspaceCreatedEvent(savedWorkspace)) // 이벤트 발행
                                        .thenReturn(savedWorkspace);
                            });
                });
    }

    private Mono<Workspace> updateWorkspace(CreateWorkspaceCommand command, User user) {
        return workspaceRepository.findById(command.workspaceId())
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND)))
                .flatMap(existingWorkspace ->
                        validateWorkspaceUpdatePermission(existingWorkspace.getIdValue(), user.getIdValue())
                                .then(Mono.fromCallable(() -> {
                                    if (command.workspaceName() != null && !command.workspaceName().isBlank()) {
                                        existingWorkspace.updateWorkspaceName(command.workspaceName());
                                    }
                                    if (command.workspaceUrl() != null && !command.workspaceUrl().isBlank()) {
                                        existingWorkspace.updateWorkspaceImgUrl(command.workspaceUrl());
                                    }
                                    if (command.description() != null && !command.description().isBlank()) {
                                        existingWorkspace.updateDescription(command.description());
                                    }
                                    return existingWorkspace;
                                }))
                                .flatMap(this::saveWorkspace)
                                .flatMap(saved -> workspaceEventService.publishWorkspaceUpdatedEvent(saved) // 이벤트 발행
                                        .thenReturn(saved))
                );
    }

    private Mono<Void> validateWorkspaceUpdatePermission(Long workspaceId, Long userId) {

        return workspaceMemberRepository.findAllByWorkspaceId(workspaceId)
                .doOnNext(member -> log.debug("멤버: userId={}, role={}", member.getUserId(), member.getRole()))
                .filter(member -> {
                    boolean matches = member.getUserId().equals(userId);
                    return matches;
                })
                .next()
                .switchIfEmpty(
                        Mono.defer(() -> {
                            return Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND));
                        })
                )
                .flatMap(member -> {
                    if (member.getRole() == WorkspaceMemberRole.OWNER) {
                        return Mono.empty();
                    } else {
                        return Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED));
                    }
                });
    }


    private Mono<WorkspaceMember> registerWorkspaceOwner(WorkspaceMember ownerMember) {
        return workspaceMemberRepository.save(ownerMember)
                .doOnSuccess(saved -> log.debug("워크스페이스 오너 등록 완료: workspaceId={}, userId={}",
                        saved.getWorkspaceId(), saved.getUserId()))
                .doOnError(error -> log.error("워크스페이스 오너 등록 실패", error));
    }

    // 워크스페이스 저장
    private Mono<Workspace> saveWorkspace(Workspace workspace) {
        return workspaceRepository.save(workspace);
    }

    // 유저 조회
    private Mono<User> getCurrentUser() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> {
                    return userRepository.findById(UserId.of(userId))
                            .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)));
                });
    }

    /**
     * 워크스페이시를 생성할 때 소유자가 되면 OWNER 권한을 얻음
     * 워크스페이스를 OWNER로 소유하고 있으면 워크스페이스를 생성할 수 없음
     * @param userId
     * @return
     */
    private Mono<Boolean> isOwnerOfWorkspace(UserId userId) {
        return workspaceMemberRepository.findByUserId(userId.getValue())
                .map(member -> member.getRole() == WorkspaceMemberRole.OWNER)
                .defaultIfEmpty(false);
    }

    /**
     * 워크스페이스 ID로 워크스페이스 정보 조회
     * @param workspaceId
     * @return
     */
    @Override
    @Transactional
    public Mono<GetWorkspaceResult> getWorkspaceById(Long workspaceId) {
        // 현재 사용자 정보 조회
        return getCurrentUser()
                .flatMap(currentUser -> {
                    Long currentUserId = currentUser.getId().getValue();

                    // 워크스페이스 기본 정보 조회
                    Mono<Workspace> workspaceMono = workspaceRepository.findById(workspaceId)
                            .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND)));

                    // 현재 사용자가 워크스페이스 멤버인지 확인
                    Mono<WorkspaceMember> membershipMono = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                            .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER)));

                    // 워크스페이스의 모든 멤버 정보를 한 번에 조회하여 처리
                    Mono<List<MemberInfo>> membersInfoMono = membershipMono.then(
                            workspaceMemberRepository.findAllByWorkspaceId(workspaceId)
                                    .collectList()
                                    .flatMap(members -> {
                                        // 워크스페이스 멤버 모두 조회
                                        List<Long> userIds = members.stream()
                                                .map(WorkspaceMember::getUserId)
                                                .distinct()
                                                .collect(Collectors.toList());

                                        return userRepository.findAllById(userIds)
                                                .collectMap(user -> user.getId().getValue(), Function.identity())
                                                .map(userMap ->
                                                        members.stream()
                                                                .map(member -> {
                                                                    User user = userMap.get(member.getUserId());

                                                                    return new MemberInfo(
                                                                            user.getId().getValue(),
                                                                            user.getNickname(),
                                                                            user.getProfileImageUrl(),
                                                                            member.getRole().name()
                                                                    );
                                                                })
                                                                .collect(Collectors.toList())
                                                );
                                    })
                    );

                    // 계층구조로 Workspace 관련 데이터 조회
                    Mono<List<FolderInfo>> foldersInfoMono = membershipMono.then(buildHierarchicalStructure(workspaceId));

                    // 모든 정보를 조합하여 최종 결과 생성
                    return Mono.zip(workspaceMono, membersInfoMono, foldersInfoMono)
                            .map(tuple -> {
                                Workspace workspace = tuple.getT1();
                                List<MemberInfo> members = tuple.getT2();
                                List<FolderInfo> folders = tuple.getT3();

                                // 소유자 찾기 (Optional 활용으로 더 안전하게)
                                MemberInfo owner = members.stream()
                                        .filter(member -> "OWNER".equals(member.role()))
                                        .findFirst()
                                        .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_OWNER_NOT_FOUND));

                                return new GetWorkspaceResult(
                                        workspace.getId().getValue(),
                                        workspace.getWorkspaceName(),
                                        workspace.getWorkspaceImgUrl(),
                                        workspace.getDescription(),
                                        owner,
                                        members,
                                        folders,
                                        workspace.getCreatedAt()
                                );
                            });
                });
    }



    /**
     * N+1 쿼리 문제를 해결하기 위한 계층 구조 조회 메서드
     */
    private Mono<List<FolderInfo>> buildHierarchicalStructure(Long workspaceId) {
        // 모든 폴더를 한 번에 조회
        Mono<List<Folder>> foldersMono = folderRepository.findAllByWorkspaceId(workspaceId)
                .collectList();

        // 모든 프로젝트를 한 번에 조회
        Mono<Map<Long, List<Project>>> projectsByFolderMono = foldersMono
                .flatMap(folders -> {
                    List<FolderId> folderIds = folders.stream()
                            .map(Folder::getId)
                            .collect(Collectors.toList());

                    return projectRepository.findAllByFolderIdIn(folderIds)
                            .collectList()
                            .map(projects -> projects.stream()
                                    .collect(Collectors.groupingBy(
                                            Project::getFolderId
                                    )));
                });

        // 모든 태스크를 한 번에 조회
        Mono<Map<Long, List<Task>>> tasksByProjectMono = projectsByFolderMono
                .flatMap(projectsByFolder -> {
                    List<ProjectId> projectIds = projectsByFolder.values().stream()
                            .flatMap(List::stream)
                            .map(Project::getId)
                            .collect(Collectors.toList());

                    if (projectIds.isEmpty()) {
                        return Mono.just(Collections.emptyMap());
                    }

                    return taskRepository.findAllByProjectIdIn(projectIds)
                            .collectList()
                            .map(tasks -> tasks.stream()
                                    .collect(Collectors.groupingBy(
                                            Task::getProjectId
                                    )));
                });

        // Mono.zip에 모든 정보를 조합하여 계층 구조 생성
        return Mono.zip(foldersMono, projectsByFolderMono, tasksByProjectMono)
                .map(tuple -> {
                    List<Folder> folders = tuple.getT1();
                    Map<Long, List<Project>> projectsByFolder = tuple.getT2();
                    Map<Long, List<Task>> tasksByProject = tuple.getT3();

                    return folders.stream()
                            .map(folder -> {
                                List<ProjectInfo> projectInfos = projectsByFolder
                                        .getOrDefault(folder.getId().getValue(), Collections.emptyList())
                                        .stream()
                                        .map(project -> {
                                            List<TaskInfo> taskInfos = tasksByProject
                                                    .getOrDefault(project.getId().getValue(), Collections.emptyList())
                                                    .stream()
                                                    .map(task -> new TaskInfo(
                                                            task.getId().getValue(),
                                                            task.getParentId() != null ? task.getParentId() : null,
                                                            task.getTaskName(),
                                                            task.getTaskStatus().name()
                                                    ))
                                                    .collect(Collectors.toList());

                                            return new ProjectInfo(
                                                    project.getId().getValue(),
                                                    project.getProjectName(),
                                                    taskInfos
                                            );
                                        })
                                        .collect(Collectors.toList());

                                return new FolderInfo(
                                        folder.getId().getValue(),
                                        folder.getFolderName(),
                                        projectInfos
                                );
                            })
                            .collect(Collectors.toList());
                });
    }

//    /**
//     * 워크스페이스 삭제
//     * @param workspaceId
//     * @return
//     */
//    @Override
//    public Mono<Void> deleteWorkspace(Long workspaceId) {
//        // 워크스페이스 권한이 있는지 확인하고 권한이 있을 시 소프트 삭제, Trash 생성
//        return getCurrentUser()
//                .flatMap(user -> this.isWorkspaceMember(workspaceId, user)
//                        .flatMap(isMember -> {
//                            if (!isMember) {
//                                return Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_ACCESS_DENIED));
//                            }
//                            return cascadeDeleteWorkspace(workspaceId, user.getIdValue());
//                        }))
//                .then();
//    }
//
//    private Mono<Void> cascadeDeleteWorkspace(Long workspaceId, Long userId) {
//        return null;
//    }
//
//    private Mono<Void> cascadeDeleteFolder(Long folderId, Long userId) {
//        return folderRepository.findById(folderId)
//                .flatMap(folder -> {
//                    // 1. 폴더 하위의 모든 프로젝트 삭제
//                    return projectRepository.findAllByFolderId(folderId)
//                            .flatMap(project -> cascadeDeleteProject(project.getId(), userId))
//                            .then(
//                                    // 2. 하위 폴더들 재귀적 삭제
//                                    folderRepository.findByParentFolderId(folderId)
//                                            .flatMap(subFolder -> cascadeDeleteFolder(subFolder.getId(), userId))
//                                            .then(
//                                                    // 3. 현재 폴더 소프트 삭제
//                                                    folderRepository.softDeleteById(folderId)
//                                                            .then(
//                                                                    // 4. 폴더 Trash 생성
//                                                                    trashRepository.save(
//                                                                            Trash.createTrash(userId, folderId, TrashType.FOLDER)
//                                                                    )
//                                                            )
//                                            )
//                            );
//                })
//                .then();
//    }
//
//    private Mono<Void> cascadeDeleteProject(Long projectId, Long userId) {
//        return projectRepository.findById(projectId)
//                .flatMap(project -> {
//                    // 1. 프로젝트 하위의 모든 태스크 삭제
//                    return taskRepository.findByProjectId(projectId)
//                            .flatMap(task -> softDeleteTask(task.getId(), userId))
//                            .then(
//                                    // 2. 프로젝트 소프트 삭제
//                                    projectRepository.softDeleteById(projectId)
//                                            .then(
//                                                    // 3. 프로젝트 Trash 생성
//                                                    trashRepository.save(
//                                                            Trash.createTrash(userId, projectId, TrashType.PROJECT)
//                                                    )
//                                            )
//                            );
//                })
//                .then();
//    }
//
//    private Mono<Void> softDeleteTask(Long taskId, Long userId) {
//        return null;
////        return taskRepository.softDeleteById(taskId)
////                .then(
////                        // Task Trash 생성
////                        trashRepository.save(
////                                Trash.createTrash(userId, taskId, TrashType.TASK)
////                        )
////                )
////                .then();
//    }


    private Mono<Boolean> isWorkspaceMember(Long workspaceId, User user) {
        return workspaceMemberRepository.existsByUserIdAndWorkspaceId(user.getId().getValue(), workspaceId)
                .defaultIfEmpty(false);
    }

    /**
     * 멤버 초대
     * @param command
     * @return
     */
    @Override
    public Mono<Void> inviteMember(InviteMemberCommand command) {

        return Mono.empty();
    }
}
