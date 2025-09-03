package backend.exception;

import backend.exception.user.UserException;
import backend.exception.workspace.WorkspaceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@Order(-2)
public class GlobalWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        if (ex instanceof WorkspaceException) {
            return handleWorkspaceException(exchange, (WorkspaceException) ex);
        } else if (ex instanceof UserException) {
            return handleUserException(exchange, (UserException) ex);
        }

        return Mono.error(ex);
    }

    private Mono<Void> handleWorkspaceException(ServerWebExchange exchange, WorkspaceException ex) {
        ServerHttpResponse response = exchange.getResponse();

        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setStatusCode(ex.getErrorCode().getHttpStatus());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ex.getErrorCode().name())
                .message(ex.getErrorCode().getMessage())
                .build();

        log.warn("WorkspaceException: {} - Path: {}",
                ex.getErrorCode().getMessage(),
                exchange.getRequest().getPath().value());

        return writeResponse(response, errorResponse);
    }

    private Mono<Void> handleUserException(ServerWebExchange exchange, UserException ex) {
        ServerHttpResponse response = exchange.getResponse();

        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.setStatusCode(ex.getErrorCode().getHttpStatus());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ex.getErrorCode().name())
                .message(ex.getErrorCode().getMessage())
                .build();

        log.warn("UserException: {} - Path: {}",
                ex.getErrorCode().getMessage(),
                exchange.getRequest().getPath().value());

        return writeResponse(response, errorResponse);
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, ErrorResponse errorResponse) {
        try {
            String body = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error converting to JSON", e);
            // 간단한 fallback
            String fallbackJson = "{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}";
            DataBuffer buffer = response.bufferFactory().wrap(fallbackJson.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
    }
}
