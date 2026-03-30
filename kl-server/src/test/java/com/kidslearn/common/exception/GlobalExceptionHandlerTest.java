package com.kidslearn.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("星星不足");
        var pd = handler.handleIllegalArgument(ex);

        assertEquals(400, pd.getStatus());
        assertEquals("星星不足", pd.getDetail());
        assertEquals("about:blank", pd.getType().toString());
    }

    @Test
    void handleBusiness_returnsCorrectStatusAndCode() {
        BusinessException ex = new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN_ACTION", "当前状态不允许此操作");
        var pd = handler.handleBusiness(ex);

        assertEquals(403, pd.getStatus());
        assertEquals("当前状态不允许此操作", pd.getDetail());
        assertEquals("FORBIDDEN_ACTION", pd.getProperties().get("code"));
    }

    @Test
    void handleNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Question", UUID.randomUUID());
        var pd = handler.handleNotFound(ex);

        assertEquals(404, pd.getStatus());
    }
}
