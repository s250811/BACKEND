package backend.infrastructure.adapter.in.web.rest.workspace;

import backend.domain.workspace.dto.request.InviteMemberRequest;
import backend.domain.workspace.dto.request.UpdateWorkspaceRequest;
import backend.domain.workspace.dto.response.WorkspaceDetailResponse;
import backend.infrastructure.adapter.in.web.rest.dto.ApiResponseDto;
import backend.application.port.in.workspace.WorkspaceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceUseCase workspaceUseCase;

    @Operation(summary = "워크스페이스 생성, 수정")
    @PostMapping
    public Mono<ApiResponseDto<Void>> createWorkspace(@RequestBody UpdateWorkspaceRequest request) {
        return workspaceUseCase.createOrUpdateWorkspace(request)
                .then(Mono.just(ApiResponseDto.createSuccessNoContent(null)));
    }

    @Operation(summary = "워크스페이스 멤버 초대")
    @PostMapping("/invite")
    public Mono<ApiResponseDto<Void>> inviteMember(@RequestBody InviteMemberRequest request) {
        return workspaceUseCase.inviteMember(request)
                .then(Mono.just(ApiResponseDto.createSuccessNoContent(null)));
    }

    @Operation(summary = "워크스페이스 상세 조회")
    @GetMapping("/{workspaceId}")
    public Mono<ApiResponseDto<WorkspaceDetailResponse>> getWorkspaceById(@PathVariable Long workspaceId) {
        return workspaceUseCase.getWorkspaceById(workspaceId)
                .map(result -> ApiResponseDto.createSuccess(
                        result,
                        "워크스페이스 조회가 완료되었습니다."
                ));
    }
}
