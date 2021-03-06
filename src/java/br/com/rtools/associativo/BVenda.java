package br.com.rtools.associativo;

import br.com.rtools.pessoa.Pessoa;
import br.com.rtools.seguranca.Usuario;
import br.com.rtools.utilitarios.DataHoje;
import br.com.rtools.utilitarios.Moeda;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "eve_venda")
@NamedQuery(name = "BVenda.pesquisaID", query = "select s from BVenda s where s.id=:pid")
public class BVenda implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @JoinColumn(name = "id_evento", referencedColumnName = "id", nullable = true)
    @OneToOne
    private AEvento evento;
    @JoinColumn(name = "id_pessoa", referencedColumnName = "id", nullable = false)
    @OneToOne
    private Pessoa pessoa;
    @JoinColumn(name = "id_responsavel", referencedColumnName = "id", nullable = false)
    @OneToOne
    private Pessoa responsavel;
    @JoinColumn(name = "id_usuario", referencedColumnName = "id", nullable = false)
    @OneToOne
    private Usuario usuario;
    @Column(name = "ds_obs", nullable = true)
    private String obs;
    @JoinColumn(name = "id_evento_servico", referencedColumnName = "id")
    @OneToOne
    private EventoServico eventoServico;
    @Column(name = "nr_valor_unitario")
    private double valorUnitario;
    @Column(name = "nr_desconto_unitario")
    private double descontoUnitario;
    @Temporal(TemporalType.DATE)
    @Column(name = "dt_emissao", nullable = false)
    private Date dtEmissao;
    @JoinColumn(name = "id_status", referencedColumnName = "id")
    @ManyToOne
    private AStatus status;

    public BVenda() {
        this.id = -1;
        this.evento = new AEvento();
        this.pessoa = new Pessoa();
        this.responsavel = new Pessoa();
        this.usuario = new Usuario();
        this.obs = "";
        this.eventoServico = null;
        this.dtEmissao = DataHoje.dataHoje();
        this.status = new AStatus();
    }

    public BVenda(int id, AEvento evento, Pessoa pessoa, Pessoa responsavel, Usuario usuario, String obs, EventoServico eventoServico, Date dtEmissao, AStatus status) {
        this.id = id;
        this.evento = evento;
        this.pessoa = pessoa;
        this.responsavel = responsavel;
        this.usuario = usuario;
        this.obs = obs;
        this.eventoServico = eventoServico;
        this.dtEmissao = dtEmissao;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AEvento getEvento() {
        return evento;
    }

    public void setEvento(AEvento evento) {
        this.evento = evento;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public Pessoa getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Pessoa responsavel) {
        this.responsavel = responsavel;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getObs() {
        return obs;
    }

    public void setObs(String obs) {
        this.obs = obs;
    }

    public EventoServico getEventoServico() {
        return eventoServico;
    }

    public void setEventoServico(EventoServico eventoServico) {
        this.eventoServico = eventoServico;
    }

    public double getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(double valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public String getValorUnitarioString() {
        return Moeda.converteR$Double(valorUnitario);
    }

    public void setValorUnitarioString(String valorUnitarioString) {
        this.valorUnitario = Moeda.converteUS$(valorUnitarioString);
    }

    public double getDescontoUnitario() {
        return descontoUnitario;
    }

    public void setDescontoUnitario(double descontoUnitario) {
        this.descontoUnitario = descontoUnitario;
    }

    public String getDescontoUnitarioString() {
        return Moeda.converteR$Double(descontoUnitario);
    }

    public void setDescontoUnitarioString(String descontoUnitarioString) {
        this.descontoUnitario = Moeda.converteUS$(descontoUnitarioString);
    }

    public String getDtEmissaoString() {
        return DataHoje.converteData(dtEmissao);
    }

    public void setDtEmissaoString(String dtEmissaoString) {
        this.dtEmissao = DataHoje.converte(dtEmissaoString);
    }

    public Date getDtEmissao() {
        return dtEmissao;
    }

    public void setDtEmissao(Date dtEmissao) {
        this.dtEmissao = dtEmissao;
    }

    public AStatus getStatus() {
        return status;
    }

    public void setStatus(AStatus status) {
        this.status = status;
    }
}
