package ru.IrinaTik.diploma.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IndexingResponse {

    private boolean result;
    private String error;

}
