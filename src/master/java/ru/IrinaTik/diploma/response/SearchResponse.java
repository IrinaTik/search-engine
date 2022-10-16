package ru.IrinaTik.diploma.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private String uri;
    private String title;
    private String snippet;
    private Float relevance;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResponse response = (SearchResponse) o;
        return Objects.equals(uri, response.uri) && Objects.equals(title, response.title) && Objects.equals(snippet, response.snippet) && Objects.equals(relevance, response.relevance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, title, snippet, relevance);
    }

    @Override
    public String toString() {
        return "uri = " + uri + "\n" +
                "title = '" + title + "'\n" +
                "snippet = '" + snippet + "'\n" +
                "relevance = " + relevance;
    }
}
