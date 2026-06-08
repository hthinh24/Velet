package com.velet.identity.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class CursorResponse<T> {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long nextCursor;
    private Boolean hasMore;
    private T items;
}