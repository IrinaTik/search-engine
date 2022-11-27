package ru.IrinaTik.diploma.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "lemma", uniqueConstraints = { @UniqueConstraint(columnNames = { "lemma", "site_id" }) })
//@SQLInsert(sql = "INSERT INTO lemma (id, lemma, frequency, site_id) VALUES (?,?,?,?) AS new(i,l,f,s) ON DUPLICATE KEY UPDATE 'frequency' = 'frequency' + new.f, id = LAST_INSERT_ID(id)")
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma")
    private Set<SearchIndex> indexSet;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site site;

    public Lemma() {
        lemmaInit();
    }

    public void addIndex(SearchIndex index) {
        indexSet.add(index);
    }

    private void lemmaInit() {
        indexSet = new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma lemma1 = (Lemma) o;
        return Objects.equals(lemma, lemma1.lemma) && Objects.equals(site, lemma1.site);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma, site);
    }

    @Override
    public String toString() {
        return "Lemma (id =  " + getId() + ") " + getLemma() + " with frequency " + getFrequency();
    }
}
