package backend.adapter.in.web;

import backend.adapter.in.common.ApiResponseDto;
import backend.application.port.in.FolderUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FolderController {

    private final FolderUseCase folderUseCase;

    // 폴더 생성
    @PostMapping("/folders")
    public Mono<ApiResponseDto<Void>> createFolder(@RequestBody CreateFolderRequest request) {
        var folder = new FolderUseCase.CreateFolderCommand(
                request.workspaceId(),
                request.folderName()
        );

        return folderUseCase.createFolder(folder)
                .then(Mono.just(ApiResponseDto.createSuccessNoContent("폴더 생성 성공")));
    }

    // 폴더 조회
    @GetMapping
    public ApiResponseDto<FolderResponse> getFolder() {
        return ApiResponseDto.createSuccess(new FolderResponse(), "폴더 조회 성공");
    }


    // 폴더 복제


    record CreateFolderRequest(
            Long workspaceId,
            String folderName
    ) {}

    record FolderResponse(){}
}
