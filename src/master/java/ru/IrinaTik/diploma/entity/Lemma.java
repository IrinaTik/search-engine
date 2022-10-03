package ru.IrinaTik.diploma.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "lemma")
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
        return Objects.equals(lemma, lemma1.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lemma);
    }

    @Override
    public String toString() {
        return "Lemma (id =  " + getId() + ") " + getLemma() + " with frequency " + getFrequency();
    }
}
