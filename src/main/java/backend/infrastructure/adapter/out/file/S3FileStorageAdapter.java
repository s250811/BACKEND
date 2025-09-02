package backend.infrastructure.adapter.out.file;

import backend.application.port.out.FileStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3AsyncClient s3AsyncClient;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.base-url}")
    private String baseUrl;

    @Override
    public Mono<String> uploadFile(FilePart file, String directory) {
        return Mono.fromCallable(() -> {
                    String originalFilename = file.filename();
                    String extension = getFileExtension(originalFilename);
                    validateImageFile(extension);

                    String key = directory + "/" + UUID.randomUUID() + "." + extension;
                    return key;
                })
                .flatMap(key -> uploadToS3(file, key))
                .map(key -> baseUrl + "/" + key)
                .doOnSuccess(url -> log.info("File uploaded successfully: {}", url))
                .doOnError(error -> log.error("File upload failed", error));
    }

    private Mono<String> uploadToS3(FilePart file, String key) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);

                        PutObjectRequest putRequest = PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(getContentType(key))
                                .contentLength((long) bytes.length)
                                .build();

                        return Mono.fromFuture(
                                s3AsyncClient.putObject(putRequest,
                                        AsyncRequestBody.fromBytes(bytes))
                        ).map(response -> key);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Failed to upload to S3", e));
                    }
                });
    }

    @Override
    public Mono<Void> deleteFile(String fileUrl) {
        return Mono.fromCallable(() -> {
                    String key = fileUrl.replace(baseUrl + "/", "");
                    return key;
                })
                .flatMap(key -> Mono.fromFuture(
                        s3AsyncClient.deleteObject(DeleteObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build())
                ))
                .then()
                .doOnSuccess(v -> log.info("File deleted successfully: {}", fileUrl))
                .doOnError(error -> log.error("File deletion failed: {}", fileUrl, error));
    }

    private String getContentType(String key) {
        String extension = getFileExtension(key);
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("유효하지 않은 파일명입니다.");
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void validateImageFile(String extension) {
        if (!extension.matches("jpg|jpeg|png|gif")) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif만 허용)");
        }
    }
}

