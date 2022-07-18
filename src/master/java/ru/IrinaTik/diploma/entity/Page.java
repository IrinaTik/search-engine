package ru.IrinaTik.diploma.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "pages", indexes = @Index(name = "path_idx", columnList = "path"))
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "path", nullable = false)
    private String relPath;

    @Transient
    private String absPath;

    @Column(nullable = false)
    private int code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Transient
    private List<Page> childPages;

    public Page() {
        pageInit();
    }

    public Page(String path) {
        this.absPath = escapeEnd(path);
        this.relPath = setRelPath();
        pageInit();
    }

    private void pageInit() {
        this.content = "";
    }

    private String setRelPath() {
        String rel = getAbsPath();
        if (rel.contains("//")) {
            rel = rel.split("//")[1];
        }
        return rel.substring(rel.indexOf("/"));
    }

    public void setChildPages(List<String> childLinks) {
        this.childPages = new ArrayList<>();
        for (String link : childLinks) {
            childPages.add(new Page(link));
        }
    }

    private String escapeEnd(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return absPath.equals(page.absPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(absPath);
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", path='" + absPath + '\'' +
                ", code='" + code + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
