package backend.infrastructure.adapter.out.file;

import backend.application.port.out.file.FileStoragePort;
import backend.exception.file.FileErrorCode;
import backend.exception.file.FileException;
import backend.infrastructure.config.S3Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class FileStorageAdapter implements FileStoragePort {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");

    private final Environment environment;
    private final S3AsyncClient s3AsyncClient;
    private final S3Config s3Config;

    public FileStorageAdapter(Environment environment,
                              @Autowired(required = false) S3AsyncClient s3AsyncClient,
                              @Autowired(required = false) S3Config s3Config) {
        this.environment = environment;
        this.s3AsyncClient = s3AsyncClient;
        this.s3Config = s3Config;
    }
    @Override
    public Mono<String> uploadFile(FilePart file, String directory) {
        String originalFilename = file.filename();
        String extension = getFileExtension(originalFilename);
        validateImageFile(extension);
        String filename = UUID.randomUUID() + "." + extension;

        return ("local".equals(getStorageType())
                ? uploadToLocal(file, directory, filename)
                : uploadToS3(file, directory, filename))
                .doOnSuccess(url -> log.info("File uploaded successfully: {}", url))
                .doOnError(error -> log.error("File upload failed", error));
    }

    @Override
    public Mono<Void> deleteFile(String fileUrl) {
        return ("local".equals(getStorageType())
                ? deleteFromLocal(fileUrl)
                : deleteFromS3(fileUrl))
                .doOnSuccess(v -> log.info("File deleted successfully: {}", fileUrl))
                .doOnError(error -> log.error("File deletion failed: {}", fileUrl, error));
    }

    private Mono<String> uploadToLocal(FilePart file, String directory, String filename) {
        Path filePath = Paths.get("./uploads", directory, filename);
        return Mono.fromCallable(() -> Files.createDirectories(filePath.getParent()))
                .then(DataBufferUtils.write(file.content(), filePath))
                .then(Mono.fromCallable(() -> "/files/" + directory + "/" + filename))
                .doOnSuccess(url -> log.debug("Local file saved at path: {}", filePath.toAbsolutePath()))
                .doOnError(error -> log.error("Local file upload failed", error))
                .onErrorMap(throwable -> new FileException(FileErrorCode.FILE_UPLOAD_FAILED));
    }

    private Mono<String> uploadToS3(FilePart file, String directory, String filename) {
        String key = directory + "/" + filename;
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer ->
                        Mono.using(
                                // 리소스 획득
                                () -> dataBuffer, buffer -> {
                                    byte[] bytes = new byte[buffer.readableByteCount()];
                                    buffer.read(bytes);

                                    return Mono.fromFuture(s3AsyncClient.putObject(
                                            PutObjectRequest.builder()
                                                    .bucket(s3Config.getBucket())
                                                    .key(key)
                                                    .contentType(getContentType(filename))
                                                    .build(),
                                            AsyncRequestBody.fromBytes(bytes)
                                    ));
                                },
                                // 리소스 해제
                                DataBufferUtils::release
                        )
                )
                .map(response -> s3Config.getBaseUrl() + "/" + key)
                .doOnSuccess(url -> log.debug("S3 file saved with key: {}", key))
                .doOnError(error -> log.error("S3 upload failed for key: {}", key, error))
                .onErrorMap(throwable -> new FileException(FileErrorCode.FILE_UPLOAD_FAILED));
    }

    private Mono<Void> deleteFromLocal(String fileUrl) {
        return Mono.fromRunnable(() -> {
            try {
                String relativePath = fileUrl.replace("/files/", "");
                Path filePath = Paths.get("./uploads", relativePath);
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    log.debug("Local file deleted from path: {}", filePath.toAbsolutePath());
                } else {
                    log.warn("Local file not found for deletion: {}", filePath.toAbsolutePath());
                }
            } catch (Exception e) {
                log.warn("Failed to delete local file: {}", fileUrl, e);
                throw new FileException(FileErrorCode.FILE_DELETE_FAILED);
            }
        });
    }

    private Mono<Void> deleteFromS3(String fileUrl) {
        String key = fileUrl.replace(s3Config.getBaseUrl() + "/", "");
        return Mono.fromFuture(s3AsyncClient.deleteObject(
                        DeleteObjectRequest.builder().bucket(s3Config.getBucket()).key(key).build()
                ))
                .then()
                .doOnSuccess(v -> log.debug("S3 file deleted with key: {}", key));
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new FileException(FileErrorCode.INVALID_FILENAME);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void validateImageFile(String extension) {
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new FileException(FileErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private String getContentType(String filename) {
        String extension = getFileExtension(filename);
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private String getStorageType() {
        return environment.acceptsProfiles(Profiles.of("prod")) ? "s3" : "local";
    }
}