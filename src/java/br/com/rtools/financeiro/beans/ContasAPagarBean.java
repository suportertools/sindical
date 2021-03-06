/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.rtools.financeiro.beans;

import br.com.rtools.financeiro.Caixa;
import br.com.rtools.financeiro.FormaPagamento;
import br.com.rtools.financeiro.Movimento;
import br.com.rtools.financeiro.TipoRecibo;
import br.com.rtools.financeiro.dao.ContasAPagarDao;
import br.com.rtools.financeiro.dao.FinanceiroDao;
import br.com.rtools.financeiro.dao.MovimentoDao;
import br.com.rtools.logSistema.NovoLog;
import br.com.rtools.movimento.GerarMovimento;
import br.com.rtools.movimento.ImprimirRecibo;
import br.com.rtools.seguranca.MacFilial;
import br.com.rtools.seguranca.Usuario;
import br.com.rtools.seguranca.controleUsuario.ChamadaPaginaBean;
import br.com.rtools.utilitarios.Dao;
import br.com.rtools.utilitarios.DataHoje;
import br.com.rtools.utilitarios.GenericaMensagem;
import br.com.rtools.utilitarios.GenericaSessao;
import br.com.rtools.utilitarios.Moeda;
import br.com.rtools.utilitarios.PF;
import br.com.rtools.utilitarios.StatusRetornoMensagem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

/**
 *
 * @author Claudemir Rtools
 */
@ManagedBean
@SessionScoped
public class ContasAPagarBean implements Serializable {

    private List<ListaContas> listaContas = new ArrayList();
    private List<ListaContas> listaContasSelecionada = new ArrayList();
    private Filtros filtros = new Filtros();

    private String motivoEstorno = "";

    private List<Movimento> listaMovimentoEstorno = new ArrayList();

    private MovimentoEstornoSelecionado mes = new MovimentoEstornoSelecionado();

    public ContasAPagarBean() {
        loadListaContas();
    }

    public void loadListaMovimentoEstorno() {
        listaMovimentoEstorno.clear();
        motivoEstorno = "";

        if (!validaEstorno()) {
            PF.update("formContasAPagar");
            return;
        }

        MovimentoDao db = new MovimentoDao();

        listaMovimentoEstorno = db.movimentoIdbaixa(mes.getMovimento().getBaixa().getId());

        for (Movimento m : listaMovimentoEstorno) {
            mes.setTotalBaixa(Moeda.soma(mes.getTotalBaixa(), m.getValorBaixa()));
        }

        PF.update("formContasAPagar:dlg_estornar_conta");

        PF.openDialog("dlg_estornar_conta");
    }

    public Boolean validaEstorno() {
        mes = new MovimentoEstornoSelecionado();

        if (listaContasSelecionada.isEmpty()) {
            GenericaMensagem.error("Atenção", "Selecione uma Conta para ESTORNAR!");
            return false;
        }

        if (listaContasSelecionada.size() > 1) {
            GenericaMensagem.error("Atenção", "Apenas uma conta pode ser estornada por vez!");
            return false;
        }

        mes.setMovimento((Movimento) new Dao().find(new Movimento(), listaContasSelecionada.get(0).getMovimentoId()));

        GenericaSessao.remove("estorno_movimento_sucesso");

        if (mes.getMovimento().getBaixa() == null) {
            GenericaMensagem.warn("Erro", "Existem boletos que não foram pagos para estornar!");
            return false;
        }

        if (mes.getMovimento().getBaixa().getFechamentoCaixa() != null) {
            GenericaMensagem.warn("Atenção", "Boletos COM CAIXA FECHADO não podem ser estornados!");
            return false;
        }

        if (!mes.getMovimento().isAtivo()) {
            GenericaMensagem.warn("Erro", "Boleto ID: " + mes.getMovimento().getId() + " esta inativo, não é possivel concluir estorno!");
            return false;
        }

        return true;
    }

    public void actNumeroBaixa() {
        filtros.setVencimento("");
        filtros.setVencimentoFinal("");
    }

    public String baixar() {
        if (listaContasSelecionada.isEmpty()) {
            GenericaMensagem.error("Atenção", "Selecione uma Conta para BAIXAR!");
            return null;
        }

        return telaBaixa("caixa");
    }

    public String estornar() {
        if (!validaEstorno()) {
            return null;
        }

        if (motivoEstorno.isEmpty() || motivoEstorno.length() <= 5) {
            GenericaMensagem.error("Atenção", "Motivo de Estorno INVÁLIDO!");
            return null;
        }

        if (mes.getMovimento().getLote().getRotina() != null && mes.getMovimento().getLote().getRotina().getId() == 132) {
            mes.getMovimento().setAtivo(false);
        }

        Integer id_baixa_estornada = mes.getMovimento().getBaixa().getId();

        StatusRetornoMensagem sr = GerarMovimento.estornarMovimento(mes.getMovimento(), motivoEstorno);

        if (!sr.getStatus()) {
            GenericaMensagem.warn("Erro", sr.getMensagem());
        } else {
            NovoLog novoLog = new NovoLog();
            novoLog.setCodigo(mes.getMovimento().getId());
            novoLog.setTabela("fin_movimento");
            novoLog.update("",
                    " Movimento - ID: " + mes.getMovimento().getId()
                    + " - Ref.: " + mes.getMovimento().getReferencia()
                    + " - Vencimento: " + mes.getMovimento().getVencimento()
                    + " - Valor: " + mes.getMovimento().getValor()
                    + " - Responsável: (" + mes.getMovimento().getPessoa().getId() + ") " + mes.getMovimento().getPessoa().getNome()
                    + " - Motivo: " + motivoEstorno
                    + " - Número da Baixa: " + id_baixa_estornada
            );
            GenericaMensagem.info("Sucesso", "Boletos estornados com sucesso!");
            GenericaSessao.put("baixa_sucesso", true);
        }
        motivoEstorno = "";
        loadListaContas();
        return null;
    }

    public String telaBaixa(String caixa_banco) {
        List lista = new ArrayList();
        MacFilial macFilial = (MacFilial) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("acessoFilial");

        if (macFilial == null) {
            GenericaMensagem.warn("Erro", "Não existe filial na sessão!");
            return null;
        }

        if (!macFilial.getCaixaOperador()) {
            if (macFilial.getCaixa() == null) {
                GenericaMensagem.warn("Erro", "Configurar Caixa nesta estação de trabalho!");
                return null;
            }
        } else {
            FinanceiroDao dao = new FinanceiroDao();
            Caixa caixax = dao.pesquisaCaixaUsuario(((Usuario) GenericaSessao.getObject("sessaoUsuario")).getId(), macFilial.getFilial().getId());

            if (caixax == null) {
                GenericaMensagem.warn("Erro", "Configurar Caixa para este Operador!");
                PF.closeDialog("dlg_caixa_banco");
                PF.update("formMovimentosReceber");
                return null;
            }
        }

        Dao dao = new Dao();

        for (ListaContas lc : listaContasSelecionada) {
            Movimento movimento = (Movimento) dao.find(new Movimento(), lc.getMovimentoId());

            // setando STRING porque na conversão de valores EX. "187774.04" para F L O A T direto esta colocando "187774.05" fazer este teste abaixo
            // System.out.println(F l o a t.parseF l o a t("187774.04"));
            // o ideal seria os campos F L O A T serem DOUBLE
            movimento.setCorrecaoString(lc.getAcrescimoEditadoString());
            movimento.setDescontoString(lc.getDescontoEditadoString());

            movimento.setValorBaixaString(Moeda.converteDoubleToString(Moeda.subtracao(Moeda.soma(lc.getValor(), lc.getAcrescimoEditado()), lc.getDescontoEditado())));

            lista.add(movimento);
        }

        GenericaSessao.put("listaMovimento", lista);

        GenericaSessao.put("caixa_banco", "caixa");

        GenericaSessao.put("tipo_recibo_imprimir", dao.find(new TipoRecibo(), 2));

        GenericaSessao.put("esMovimento", "S");

        return ((ChamadaPaginaBean) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("chamadaPaginaBean")).baixaGeral();
    }

    public void recibo() {
        List<Movimento> l_movimento = new ArrayList();

        Dao dao = new Dao();

        for (ListaContas lc : listaContasSelecionada) {
            Movimento mov = (Movimento) dao.find(new Movimento(), lc.getMovimentoId());
            l_movimento.add(mov);
        }

        ImprimirRecibo ir = new ImprimirRecibo();

        if (ir.gerar_recibo_generico(l_movimento, null)) {
            ir.imprimir();
        }
    }

    public void calculoAcrescimoDesconto(ListaContas lc) {
        lc.setValorPagamento(Moeda.soma(lc.getValor(), lc.getAcrescimoEditado()));
        lc.setValorPagamento(Moeda.subtracao(lc.getValorPagamento(), lc.getDescontoEditado()));
    }

    public final void loadListaContas() {
        listaContas.clear();
        listaContasSelecionada.clear();

        MovimentoDao db = new MovimentoDao();

        List<Object> result = new ContasAPagarDao().listaContasAPagar(filtros);
        for (Object ob : result) {
            List linha = (List) ob;

            List<FormaPagamento> list_fp = new ArrayList();

            if (linha.get(13) != null) {
                list_fp = db.pesquisaFormaPagamento((Integer) linha.get(13));
            }

            listaContas.add(
                    new ListaContas(
                            linha.get(0).toString(),
                            (Date) linha.get(1),
                            linha.get(2).toString(),
                            Double.valueOf(linha.get(3).toString()),
                            Double.valueOf(linha.get(4).toString()),
                            Double.valueOf(linha.get(5).toString()),
                            Double.valueOf(linha.get(6).toString()),
                            (Date) linha.get(7),
                            linha.get(8).toString(),
                            linha.get(9).toString(),
                            (Date) linha.get(10),
                            (Date) linha.get(11),
                            linha.get(12).toString(),
                            (Integer) linha.get(13),
                            (Integer) linha.get(14),
                            (String) linha.get(15),
                            (String) linha.get(16),
                            (String) linha.get(17),
                            (String) linha.get(18),
                            (String) linha.get(19),
                            (String) linha.get(20),
                            new Double(0),
                            new Double(0),
                            list_fp
                    )
            );
        }
    }

    public String getTotal() {
        Double valor = new Double(0);
        for (ListaContas lc : listaContas) {
            valor = Moeda.soma(valor, lc.getValor());
            valor = Moeda.subtracao(Moeda.soma(valor, lc.getAcrescimoEditado()), lc.getDescontoEditado());
        }
        return Moeda.converteDoubleToString(valor);
    }

    public String getTotalEmAberto() {
        Double valor = new Double(0);
        for (ListaContas lc : listaContas) {

            if (lc.getBaixaId() == null) {
                valor = Moeda.soma(valor, lc.getValor());
                valor = Moeda.subtracao(Moeda.soma(valor, lc.getAcrescimoEditado()), lc.getDescontoEditado());
            }
        }
        return Moeda.converteDoubleToString(valor);
    }

    public String getTotalPago() {
        Double valor = new Double(0);
        for (ListaContas lc : listaContas) {
            if (lc.getBaixaId() != null) {
                valor = Moeda.soma(valor, lc.getValorPagamento());
            }
        }
        return Moeda.converteDoubleToString(valor);
    }

    public String getTotalSelecionado() {
        Double valor = new Double(0);
        for (ListaContas lc : listaContasSelecionada) {
            valor = Moeda.soma(valor, lc.getValor());
            valor = Moeda.subtracao(Moeda.soma(valor, lc.getAcrescimoEditado()), lc.getDescontoEditado());
        }
        return Moeda.converteDoubleToString(valor);
    }

    public String getTotalEmAbertoSelecionado() {
        Double valor = new Double(0);
        for (ListaContas lc : listaContasSelecionada) {
            if (lc.getBaixaId() == null) {
                valor = Moeda.soma(valor, lc.getValor());
                valor = Moeda.subtracao(Moeda.soma(valor, lc.getAcrescimoEditado()), lc.getDescontoEditado());
            }
        }
        return Moeda.converteDoubleToString(valor);
    }

    public String getTotalPagoSelecionado() {
        Double valor = new Double(0);
        for (ListaContas lc : listaContasSelecionada) {
            if (lc.getBaixaId() != null) {
                valor = Moeda.soma(valor, lc.getValorPagamento());
            }
        }
        return Moeda.converteDoubleToString(valor);
    }

    public List<ListaContas> getListaContas() {
        return listaContas;
    }

    public void setListaContas(List<ListaContas> listaContas) {
        this.listaContas = listaContas;
    }

    public Filtros getFiltros() {
        return filtros;
    }

    public void setFiltros(Filtros filtros) {
        this.filtros = filtros;
    }

    public List<ListaContas> getListaContasSelecionada() {
        return listaContasSelecionada;
    }

    public void setListaContasSelecionada(List<ListaContas> listaContasSelecionada) {
        this.listaContasSelecionada = listaContasSelecionada;
    }

    public String getMotivoEstorno() {
        return motivoEstorno;
    }

    public void setMotivoEstorno(String motivoEstorno) {
        this.motivoEstorno = motivoEstorno;
    }

    public List<Movimento> getListaMovimentoEstorno() {
        return listaMovimentoEstorno;
    }

    public void setListaMovimentoEstorno(List<Movimento> listaMovimentoEstorno) {
        this.listaMovimentoEstorno = listaMovimentoEstorno;
    }

    public MovimentoEstornoSelecionado getMes() {
        return mes;
    }

    public void setMes(MovimentoEstornoSelecionado mes) {
        this.mes = mes;
    }

    public class Filtros {

        private String condicao;
        private String vencimento;
        private String pagamento;
        private String lancamento;
        private String emissao;
        private String vencimentoFinal;
        private String pagamentoFinal;
        private String lancamentoFinal;
        private String emissaoFinal;
        private String numeroBaixa;
        private String ordem;

        public Filtros() {
            this.condicao = "em_aberto";
            this.vencimento = "";
            this.pagamento = "";
            this.lancamento = "";
            this.emissao = "";
            this.vencimentoFinal = new DataHoje().incrementarDias(7, DataHoje.data());
            this.pagamentoFinal = "";
            this.lancamentoFinal = "";
            this.emissaoFinal = "";
            this.numeroBaixa = "";
            this.ordem = "vencimento";
        }

        public Filtros(String condicao, String vencimento, String pagamento, String lancamento, String emissao, String numeroBaixa, String ordem) {
            this.condicao = condicao;
            this.vencimento = vencimento;
            this.pagamento = pagamento;
            this.lancamento = lancamento;
            this.emissao = emissao;
            this.numeroBaixa = numeroBaixa;
            this.ordem = ordem;
        }

        public String getCondicao() {
            return condicao;
        }

        public void setCondicao(String condicao) {
            this.condicao = condicao;
        }

        public String getVencimento() {
            return vencimento;
        }

        public void setVencimento(String vencimento) {
            this.vencimento = vencimento;
        }

        public String getPagamento() {
            return pagamento;
        }

        public void setPagamento(String pagamento) {
            this.pagamento = pagamento;
        }

        public String getLancamento() {
            return lancamento;
        }

        public void setLancamento(String lancamento) {
            this.lancamento = lancamento;
        }

        public String getEmissao() {
            return emissao;
        }

        public void setEmissao(String emissao) {
            this.emissao = emissao;
        }

        public String getOrdem() {
            return ordem;
        }

        public void setOrdem(String ordem) {
            this.ordem = ordem;
        }

        public String getVencimentoFinal() {
            return vencimentoFinal;
        }

        public void setVencimentoFinal(String vencimentoFinal) {
            this.vencimentoFinal = vencimentoFinal;
        }

        public String getPagamentoFinal() {
            return pagamentoFinal;
        }

        public void setPagamentoFinal(String pagamentoFinal) {
            this.pagamentoFinal = pagamentoFinal;
        }

        public String getLancamentoFinal() {
            return lancamentoFinal;
        }

        public void setLancamentoFinal(String lancamentoFinal) {
            this.lancamentoFinal = lancamentoFinal;
        }

        public String getEmissaoFinal() {
            return emissaoFinal;
        }

        public void setEmissaoFinal(String emissaoFinal) {
            this.emissaoFinal = emissaoFinal;
        }

        public String getNumeroBaixa() {
            return numeroBaixa;
        }

        public void setNumeroBaixa(String numeroBaixa) {
            this.numeroBaixa = numeroBaixa;
        }

    }

    public class ListaContas {

        private String nome;
        private Date vencimento;
        private String referencia;
        private Double valor;
        private Double acrescimo;
        private Double desconto;
        private Double valorPagamento;
        private Date baixa;
        private String tipoDocumento;
        private String documento;
        // DETALHES ----
        private Date lancamento;
        private Date emissao;
        private String conta;
        private Integer baixaId;
        private Integer movimentoId;
        private String operador;
        private String caixa;
        private String tipoDocumentoLote;
        private String documentoLote;
        private String descricao;
        private String historico;

        // EDITADO
        private Double acrescimoEditado;
        private Double descontoEditado;

        private List<FormaPagamento> listaFormaPagamento;

        public ListaContas(String nome, Date vencimento, String referencia, Double valor, Double acrescimo, Double desconto, Double valorPagamento, Date baixa, String tipoDocumento, String documento, Date lancamento, Date emissao, String conta, Integer baixaId, Integer movimentoId, String operador, String caixa, String tipoDocumentoLote, String documentoLote, String descricao, String historico, Double acrescimoEditado, Double descontoEditado, List<FormaPagamento> listaFormaPagamento) {
            this.nome = nome;
            this.vencimento = vencimento;
            this.referencia = referencia;
            this.valor = valor;
            this.acrescimo = acrescimo;
            this.desconto = desconto;
            this.valorPagamento = valorPagamento;
            this.baixa = baixa;
            this.tipoDocumento = tipoDocumento;
            this.documento = documento;
            this.lancamento = lancamento;
            this.emissao = emissao;
            this.conta = conta;
            this.baixaId = baixaId;
            this.movimentoId = movimentoId;
            this.operador = operador;
            this.caixa = caixa;
            this.tipoDocumentoLote = tipoDocumentoLote;
            this.documentoLote = documentoLote;
            this.descricao = descricao;
            this.historico = historico;
            this.acrescimoEditado = acrescimoEditado;
            this.descontoEditado = descontoEditado;
            this.listaFormaPagamento = listaFormaPagamento;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public Date getVencimento() {
            return vencimento;
        }

        public void setVencimento(Date vencimento) {
            this.vencimento = vencimento;
        }

        public String getVencimentoString() {
            return DataHoje.converteData(vencimento);
        }

        public void setVencimentoString(String vencimentoString) {
            this.vencimento = DataHoje.converte(vencimentoString);
        }

        public String getReferencia() {
            return referencia;
        }

        public void setReferencia(String referencia) {
            this.referencia = referencia;
        }

        public Double getValor() {
            return valor;
        }

        public void setValor(Double valor) {
            this.valor = valor;
        }

        public String getValorString() {
            return Moeda.converteR$Double(valor);
        }

        public void setValorString(String valorString) {
            this.valor = Moeda.converteStringToDouble(valorString);
        }

        public Double getAcrescimo() {
            return acrescimo;
        }

        public void setAcrescimo(Double acrescimo) {
            this.acrescimo = acrescimo;
        }

        public String getAcrescimoString() {
            return Moeda.converteDoubleToString(acrescimo);
        }

        public void setAcrescimoString(String acrescimoString) {
            this.acrescimo = Moeda.converteStringToDouble(acrescimoString);
        }

        public Double getDesconto() {
            return desconto;
        }

        public void setDesconto(Double desconto) {
            this.desconto = desconto;
        }

        public String getDescontoString() {
            return Moeda.converteDoubleToString(desconto);
        }

        public void setDescontoString(String descontoString) {
            this.desconto = Moeda.converteStringToDouble(descontoString);
        }

        public Double getValorPagamento() {
            return valorPagamento;
        }

        public void setValorPagamento(Double valorPagamento) {
            this.valorPagamento = valorPagamento;
        }

        public String getValorPagamentoString() {
            return Moeda.converteDoubleToString(valorPagamento);
        }

        public void setValorPagamentoString(String valorPagamentoString) {
            this.valorPagamento = Moeda.converteStringToDouble(valorPagamentoString);
        }

        public Date getBaixa() {
            return baixa;
        }

        public void setBaixa(Date baixa) {
            this.baixa = baixa;
        }

        public String getBaixaString() {
            return DataHoje.converteData(baixa);
        }

        public void setBaixaString(String baixaString) {
            this.baixa = DataHoje.converte(baixaString);
        }

        public String getTipoDocumento() {
            return tipoDocumento;
        }

        public void setTipoDocumento(String tipoDocumento) {
            this.tipoDocumento = tipoDocumento;
        }

        public String getDocumento() {
            return documento;
        }

        public void setDocumento(String documento) {
            this.documento = documento;
        }

        public Date getLancamento() {
            return lancamento;
        }

        public void setLancamento(Date lancamento) {
            this.lancamento = lancamento;
        }

        public String getLancamentoString() {
            return DataHoje.converteData(lancamento);
        }

        public void setLancamentoString(String lancamentoString) {
            this.lancamento = DataHoje.converte(lancamentoString);
        }

        public Date getEmissao() {
            return emissao;
        }

        public void setEmissao(Date emissao) {
            this.emissao = emissao;
        }

        public String getEmissaoString() {
            return DataHoje.converteData(emissao);
        }

        public void setEmissaoString(String emissaoString) {
            this.emissao = DataHoje.converte(emissaoString);
        }

        public String getConta() {
            return conta;
        }

        public void setConta(String conta) {
            this.conta = conta;
        }

        public Integer getBaixaId() {
            return baixaId;
        }

        public void setBaixaId(Integer baixaId) {
            this.baixaId = baixaId;
        }

        public Integer getMovimentoId() {
            return movimentoId;
        }

        public void setMovimentoId(Integer movimentoId) {
            this.movimentoId = movimentoId;
        }

        public String getOperador() {
            return operador;
        }

        public void setOperador(String operador) {
            this.operador = operador;
        }

        public String getCaixa() {
            return caixa;
        }

        public void setCaixa(String caixa) {
            this.caixa = caixa;
        }

        public String getTipoDocumentoLote() {
            return tipoDocumentoLote;
        }

        public void setTipoDocumentoLote(String tipoDocumentoLote) {
            this.tipoDocumentoLote = tipoDocumentoLote;
        }

        public String getDocumentoLote() {
            return documentoLote;
        }

        public void setDocumentoLote(String documentoLote) {
            this.documentoLote = documentoLote;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public String getHistorico() {
            return historico;
        }

        public void setHistorico(String historico) {
            this.historico = historico;
        }

        public Double getAcrescimoEditado() {
            return acrescimoEditado;
        }

        public void setAcrescimoEditado(Double acrescimoEditado) {
            this.acrescimoEditado = acrescimoEditado;
        }

        public String getAcrescimoEditadoString() {
            return Moeda.converteDoubleToString(acrescimoEditado);
        }

        public void setAcrescimoEditadoString(String acrescimoEditadoString) {
            this.acrescimoEditado = Moeda.converteStringToDouble(acrescimoEditadoString);
        }

        public Double getDescontoEditado() {
            return descontoEditado;
        }

        public void setDescontoEditado(Double descontoEditado) {
            this.descontoEditado = descontoEditado;
        }

        public String getDescontoEditadoString() {
            return Moeda.converteDoubleToString(descontoEditado);
        }

        public void setDescontoEditadoString(String descontoEditadoString) {
            this.descontoEditado = Moeda.converteStringToDouble(descontoEditadoString);
        }

        public List<FormaPagamento> getListaFormaPagamento() {
            return listaFormaPagamento;
        }

        public void setListaFormaPagamento(List<FormaPagamento> listaFormaPagamento) {
            this.listaFormaPagamento = listaFormaPagamento;
        }

    }

    public class MovimentoEstornoSelecionado {

        private Movimento movimento;
        private Double totalBaixa;

        public MovimentoEstornoSelecionado() {
            this.movimento = new Movimento();
            this.totalBaixa = new Double(0);
        }

        public MovimentoEstornoSelecionado(Movimento movimento, Double totalBaixa) {
            this.movimento = movimento;
            this.totalBaixa = totalBaixa;
        }

        public Movimento getMovimento() {
            return movimento;
        }

        public void setMovimento(Movimento movimento) {
            this.movimento = movimento;
        }

        public Double getTotalBaixa() {
            return totalBaixa;
        }

        public void setTotalBaixa(Double totalBaixa) {
            this.totalBaixa = totalBaixa;
        }

        public String getTotalBaixaString() {
            return Moeda.converteR$Double(totalBaixa);
        }

        public void setTotalBaixa(String totalBaixaString) {
            this.totalBaixa = Moeda.converteUS$(totalBaixaString);
        }
    }
}
