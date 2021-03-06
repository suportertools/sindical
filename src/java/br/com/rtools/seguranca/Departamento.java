package br.com.rtools.seguranca;

import br.com.rtools.utilitarios.BaseEntity;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.*;

@Entity
@Table(name = "seg_departamento")
@NamedQueries({
    @NamedQuery(name = "Departamento.pesquisaID", query = "SELECT DEP FROM Departamento DEP WHERE DEP.id = :pid"),
    @NamedQuery(name = "Departamento.findAll", query = "SELECT DEP FROM Departamento DEP ORDER BY DEP.descricao ASC "),
    @NamedQuery(name = "Departamento.findName", query = "SELECT DEP FROM Departamento DEP WHERE UPPER(DEP.descricao) LIKE :pdescricao ORDER BY DEP.descricao ASC ")
})
public class Departamento implements BaseEntity, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "ds_descricao", length = 50, nullable = false)
    private String descricao;

    public Departamento() {
        this.id = -1;
        this.descricao = "";
    }

    public Departamento(Integer id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.id;
        hash = 37 * hash + (this.descricao != null ? this.descricao.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Departamento other = (Departamento) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if ((this.descricao == null) ? (other.descricao != null) : !this.descricao.equals(other.descricao)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Departamento{" + "id=" + id + ", descricao=" + descricao + '}';
    }
}
