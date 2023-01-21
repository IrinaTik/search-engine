package ru.IrinaTik.diploma.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "pages", indexes = @Index(name = "path_idx", columnList = "path"))
public class Page implements Comparable<Page>{

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

    @OneToMany(mappedBy = "page")
    private Set<SearchIndex> indexSet;

    @Transient
    private List<Page> childPages;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

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
        this.setIndexSet(new HashSet<>());
    }

    private String setRelPath() {
        String rel = getAbsPath();
        if (rel.contains("//")) {
            rel = rel.split("//")[1];
        }
        return rel.substring(rel.indexOf("/"));
    }

    public void setAbsPath(String absPath) {
        this.absPath = absPath;
        this.relPath = setRelPath();
    }

    public void setChildPages(List<String> childLinks) {
        this.childPages = new ArrayList<>();
        for (String link : childLinks) {
            Page child = new Page(link);
            childPages.add(child);
        }
    }

    public boolean isPageResponseOK() {
        return getCode() == 200;
    }

    private String escapeEnd(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    @Override
    public int compareTo(Page o) {
        return this.getAbsPath().compareTo(o.getAbsPath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return Objects.equals(relPath, page.relPath) && Objects.equals(site, page.site);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relPath, site);
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", path='" + relPath + '\'' +
                ", code='" + code + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
