package vn.edu.bookstore.legacy.adapter.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.bookstore.legacy.adapter.soap.LegacyCatalogException;


/** Chuyển lỗi → Problem Details (RFC 9457). */
@RestControllerAdvice
public class LegacyProblemHandler {

    // TODO(TH1-7): thêm @ExceptionHandler(LegacyCatalogException.class) ánh xạ:
    //   NOT_FOUND → 404 book-not-found; INVALID_REQUEST → 400 validation-failed;
    //   UPSTREAM_ERROR → 502, UPSTREAM_TIMEOUT → 504, UPSTREAM_UNAVAILABLE → 503 (upstream-unavailable);
    //   thêm thuộc tính "legacyCode" và instance = đường dẫn yêu cầu.

    @ExceptionHandler(LegacyCatalogException.class)
    public ProblemDetail onLegacy(
            LegacyCatalogException e,
            HttpServletRequest request) {

        HttpStatus status;
        URI type;
        String title;

        switch (e.kind()) {
            case NOT_FOUND -> {
                status = HttpStatus.NOT_FOUND;
                type = ProblemTypes.BOOK_NOT_FOUND;
                title = "Không tìm thấy sách";
            }

            case INVALID_REQUEST -> {
                status = HttpStatus.BAD_REQUEST;
                type = ProblemTypes.VALIDATION_FAILED;
                title = "Yêu cầu không hợp lệ";
            }

            case UPSTREAM_ERROR -> {
                status = HttpStatus.BAD_GATEWAY;
                type = ProblemTypes.UPSTREAM_ERROR;
                title = "Hệ thống danh mục kế thừa báo lỗi";
            }

            case UPSTREAM_TIMEOUT -> {
                status = HttpStatus.GATEWAY_TIMEOUT;
                type = ProblemTypes.UPSTREAM_TIMEOUT;
                title = "Hệ thống danh mục kế thừa phản hồi quá chậm";
            }

            case UPSTREAM_UNAVAILABLE -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                type = ProblemTypes.UPSTREAM_UNAVAILABLE;
                title = "Hệ thống danh mục kế thừa không truy cập được";
            }

            default -> throw new IllegalStateException(
                    "Loại lỗi chưa được xử lý: " + e.kind());
        }

        ProblemDetail p = problem(
                status,
                type,
                title,
                e.getMessage());

        // Đường dẫn của request gây ra lỗi.
        p.setInstance(URI.create(request.getRequestURI()));

        // Mã lỗi từ hệ thống SOAP kế thừa, nếu có.
        if (e.legacyCode() != null) {
            p.setProperty("legacyCode", e.legacyCode());
        }

        return p;
    }
    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail onInvalid(InvalidRequestException e, HttpServletRequest request) {
        ProblemDetail p = problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION_FAILED,
                "Yêu cầu không hợp lệ", e.getMessage());
        p.setInstance(URI.create(request.getRequestURI()));
        return p;
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setType(type);
        p.setTitle(title);
        return p;
    }
}
