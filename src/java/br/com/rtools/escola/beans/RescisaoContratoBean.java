package br.com.rtools.escola.beans;

import br.com.rtools.associativo.MatriculaSocios;
import br.com.rtools.escola.EscStatus;
import br.com.rtools.escola.MatriculaEscola;
import br.com.rtools.escola.MatriculaIndividual;
import br.com.rtools.escola.MatriculaTurma;
import br.com.rtools.escola.dao.MatriculaEscolaDao;
import br.com.rtools.escola.dao.RescisaoContratoDao;
import br.com.rtools.financeiro.CondicaoPagamento;
import br.com.rtools.financeiro.FStatus;
import br.com.rtools.financeiro.FTipoDocumento;
import br.com.rtools.financeiro.Lote;
import br.com.rtools.financeiro.Movimento;
import br.com.rtools.financeiro.Servicos;
import br.com.rtools.financeiro.TipoServico;
import br.com.rtools.logSistema.NovoLog;
import br.com.rtools.movimento.GerarMovimento;
import br.com.rtools.pessoa.Filial;
import br.com.rtools.pessoa.Pessoa;
import br.com.rtools.pessoa.PessoaComplemento;
import br.com.rtools.pessoa.dao.PessoaDao;
import br.com.rtools.seguranca.FilialRotina;
import br.com.rtools.seguranca.MacFilial;
import br.com.rtools.seguranca.Registro;
import br.com.rtools.seguranca.Rotina;
import br.com.rtools.seguranca.Usuario;
import br.com.rtools.seguranca.controleUsuario.ControleAcessoBean;
import br.com.rtools.seguranca.dao.FilialRotinaDao;
import br.com.rtools.utilitarios.Dao;
import br.com.rtools.utilitarios.DataHoje;
import br.com.rtools.utilitarios.GenericaMensagem;
import br.com.rtools.utilitarios.GenericaSessao;
import br.com.rtools.utilitarios.Moeda;
import br.com.rtools.utilitarios.PF;
import br.com.rtools.utilitarios.dao.FunctionsDao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;

@ManagedBean
@SessionScoped
public class RescisaoContratoBean implements Serializable {

    private MatriculaEscola matriculaEscola;
    private Servicos servicos;
    private Pessoa titular;
    private Pessoa pessoa;
    private Pessoa beneficiario;
    private Registro registro;
    private List<ListaMovimentoCheck> listaMovimento;
    private List<Movimento> movimentosRescisao;
    private List<SelectItem> listaMesVencimento;
    private String descricaoServico;
    private String tipoRescisaoContrato;
    private String valorMulta;
    private String valorTotal;
    private String valor;
    private String dataGeracao;
    private int id;
    private int idEvt;
    private int idMesVencimento;
    private int diaVencimento;
    private int numeroParcelas;
    private Integer parcelasRestantes;

    // BLOQUEIO
    private List<SelectItem> listFiliais;
    private Integer filial_id;
    private Boolean liberaAcessaFilial;

    private String motivoInativacao;
    private Boolean chkSeleciona;

    @PostConstruct
    public void init() {
        matriculaEscola = new MatriculaEscola();
        servicos = new Servicos();
        titular = new Pessoa();
        pessoa = new Pessoa();
        beneficiario = new Pessoa();
        registro = new Registro();
        listaMovimento = new ArrayList();
        movimentosRescisao = new ArrayList();
        listaMesVencimento = new ArrayList();
        descricaoServico = "";
        tipoRescisaoContrato = "";
        valorMulta = "";
        valorTotal = "";
        valor = "";
        dataGeracao = "";
        id = -1;
        idEvt = -1;
        idMesVencimento = 0;
        diaVencimento = 0;
        numeroParcelas = 1;
        parcelasRestantes = 0;
        liberaAcessaFilial = false;
        filial_id = 0;
        listFiliais = new ArrayList();
        motivoInativacao = "";
        chkSeleciona = true;
        loadLiberaAcessaFilial();
    }

    @PreDestroy
    public void destroy() {
        /* chamado quando outra view for chamada através do UIViewRoot.setViewId(String viewId) */
    }

    public void inativarMovimentos() {
        if (motivoInativacao.isEmpty()) {
            GenericaMensagem.warn("Atenção", "Digite um motivo para exclusão!");
            return;
        } else if (motivoInativacao.length() < 6) {
            GenericaMensagem.warn("Atenção", "Motivo de exclusão inválido!");
            return;
        }

        List<Movimento> listam = new ArrayList();

        for (ListaMovimentoCheck lmc : listaMovimento) {
            if (lmc.getCheck()) {
                //int id_movimento = ((Movimento) dh.getArgumento1()).getId();
                Movimento mov = (Movimento) new Dao().find(lmc.getMovimento());
                listam.add(mov);
            }
        }

        if (listam.isEmpty()) {
            GenericaMensagem.warn("Atenção", "Nenhum boletos foi selecionado!");
            return;
        }

        Dao dao = new Dao();
        dao.openTransaction();

        if (!GerarMovimento.inativarArrayMovimento(listam, motivoInativacao, dao).isEmpty()) {
            GenericaMensagem.error("Atenção", "Ocorreu um erro em uma das exclusões, verifique o log!");
            dao.rollback();
            return;
        } else {
            GenericaMensagem.info("Sucesso", "Boletos foram excluídos!");
        }

        dao.commit();

        listaMovimento.clear();
        pesquisaMovimentosPorMatricula();
    }

    public void marcarTodos() {
        for (ListaMovimentoCheck lmc : listaMovimento) {
            lmc.setCheck(chkSeleciona);
        }
    }

    public void loadLiberaAcessaFilial() {
        if (new ControleAcessoBean().permissaoValida("libera_acesso_filiais", 4)) {
            liberaAcessaFilial = true;
        }
    }

    public String getLoad() {
        if (getTipoRescisaoContrato().equals("matriculaEscola")) {
            getMatriculaEscola();
        }
        return "";
    }

    public void novo() {
        GenericaSessao.remove("rescisaoContratoBean");
    }

    public void clickRescindirContrato() {
        if (idEvt == -1) {
            GenericaMensagem.warn("Atenção", "Não existem Movimentos gerados!");
            PF.update("form_rescisao");
            return;
        }

        if (matriculaEscola.getEscStatus().getId() != 1) {
            GenericaMensagem.warn("Atenção", "Matricula não pode ser rescindida, aluno não é frequente!");
            PF.update("form_rescisao");
            return;
        }

        for (ListaMovimentoCheck lmc : listaMovimento) {
            if (DataHoje.menorData(lmc.getMovimento().getDtVencimento(), DataHoje.dataHoje())) {
                GenericaMensagem.warn("Atenção", "Existem movimentos atrasados, rescisão não permitida!");
                PF.update("form_rescisao");
                return;
            }
        }
        PF.openDialog("dlg_salvar");
    }

    public MatriculaEscola getMatriculaEscola() {
        if (GenericaSessao.exists("matriculaEscolaPesquisa")) {
            matriculaEscola = (MatriculaEscola) GenericaSessao.getObject("matriculaEscolaPesquisa", true);

            listaMovimento.clear();

            if (matriculaEscola.getServicoPessoa().getEvt() != null) {
                this.idEvt = matriculaEscola.getServicoPessoa().getEvt().getId();
                titular = matriculaEscola.getServicoPessoa().getCobranca();
                beneficiario = matriculaEscola.getServicoPessoa().getPessoa();
                pesquisaMovimentosPorMatricula();
                MatriculaEscolaDao matriculaEscolaDao = new MatriculaEscolaDao();
                MatriculaIndividual mi = matriculaEscolaDao.pesquisaCodigoMIndividual(matriculaEscola.getId());
                MatriculaTurma mt = matriculaEscolaDao.pesquisaCodigoMTurma(matriculaEscola.getId());
                if (mi.getId() != -1) {
                    servicos = mi.getCurso();
                    descricaoServico = "Contrato nº " + matriculaEscola.getId() + " - Serviço: " + servicos.getDescricao();
                } else if (mt.getId() != -1) {
                    servicos = mt.getTurma().getCursos();
                    descricaoServico = "Contrato nº " + matriculaEscola.getId() + " - Serviço: " + servicos.getDescricao() + " - Descrição: " + mt.getTurma().getDescricao();
                }
                PessoaDao pessoaDB = new PessoaDao();
                PessoaComplemento pc = pessoaDB.pesquisaPessoaComplementoPorPessoa(matriculaEscola.getServicoPessoa().getCobranca().getId());
                if (pc.getId() != -1) {
                    diaVencimento = pc.getNrDiaVencimento();
                }
            }
        }
        return matriculaEscola;
    }

    public void setMatriculaEscola(MatriculaEscola matriculaEscola) {
        this.matriculaEscola = matriculaEscola;
    }

    public Servicos getServicos() {
        return servicos;
    }

    public void setServicos(Servicos servicos) {
        this.servicos = servicos;
    }

    public void pesquisaMovimentosPorMatricula() {
        if (listaMovimento.isEmpty()) {
            dataGeracao = "";
            RescisaoContratoDao dao = new RescisaoContratoDao();
            List<Lote> lista_lote = dao.listaLoteEVT(idEvt);
            if (!lista_lote.isEmpty()) {
                List<Movimento> list = dao.listaMovimentoEVT(idEvt);
                if (!list.isEmpty()) {
                    dataGeracao = list.get(0).getLote().getEmissao();
                    if (matriculaEscola.getEscStatus().getId() != 3) {
                        for (Movimento list1 : list) {
                            listaMovimento.add(new ListaMovimentoCheck(true, list1));
                        }
                    } else {
                        for (Movimento list1 : list) {
                            if (list1.getTipoServico().getId() == 6) {
                                listaMovimento.add(new ListaMovimentoCheck(true, list1));
                            }
                        }
                    }
                }
            }

            // PARCELAS RESTANTES
            parcelasRestantes = DataHoje.quantidadeMeses("01/" + DataHoje.data().substring(3), "01/" + matriculaEscola.getServicoPessoa().getReferenciaValidade()) + 1;
            List<Movimento> l = new RescisaoContratoDao().listaMovimentoPagos(idEvt);
            Integer parcelas_pagas = l.size();
            parcelasRestantes = parcelasRestantes - parcelas_pagas;
            // ------------------

            // VALOR RESTANTE
            double valor_parcela;
            if (matriculaEscola.getServicoPessoa().getNrDesconto() == 0) {
                if (matriculaEscola.getServicoPessoa().getPessoa().getSocios().getId() != -1) {
                    valor_parcela = new FunctionsDao().valorServico(matriculaEscola.getServicoPessoa().getPessoa().getId(), matriculaEscola.getServicoPessoa().getServicos().getId(), DataHoje.dataHoje(), 0, matriculaEscola.getServicoPessoa().getPessoa().getSocios().getMatriculaSocios().getCategoria().getId());
                } else {
                    valor_parcela = new FunctionsDao().valorServico(matriculaEscola.getServicoPessoa().getPessoa().getId(), matriculaEscola.getServicoPessoa().getServicos().getId(), DataHoje.dataHoje(), 0, null);
                }
            } else {
                valor_parcela = new FunctionsDao().valorServicoCheio(matriculaEscola.getServicoPessoa().getPessoa().getId(), matriculaEscola.getServicoPessoa().getServicos().getId(), DataHoje.dataHoje());
                valor_parcela = valor_parcela - (matriculaEscola.getServicoPessoa().getNrDesconto() * valor_parcela) / 100;
            }

            valor = Moeda.converteR$Double(parcelasRestantes * valor_parcela);
        }
    }

    public int getIdEvt() {
        return idEvt;
    }

    public void setIdEvt(int idEvt) {
        this.idEvt = idEvt;
    }

    public void rescindirMatriculaEscola() {
        if (idEvt == -1) {
            GenericaMensagem.warn("Atenção", "Não existem Movimentos gerados!");
            return;
        }

        if (matriculaEscola.getEscStatus().getId() != 1) {
            GenericaMensagem.warn("Atenção", "Matricula não pode ser rescindida, aluno não é frequente!");
            return;
        }

        for (ListaMovimentoCheck lmc : listaMovimento) {
            if (DataHoje.menorData(lmc.getMovimento().getDtVencimento(), DataHoje.dataHoje())) {
                GenericaMensagem.warn("Atenção", "Existem movimentos atrasados, rescisão não permitida!");
                return;
            }
        }

        Dao dao = new Dao();
        dao.openTransaction();
        if (!listaMovimento.isEmpty()) {
            for (ListaMovimentoCheck lmc : listaMovimento) {
                if (lmc.getMovimento().getBaixa() == null && lmc.getMovimento().isAtivo()) {
                    lmc.getMovimento().setAtivo(false);
                    if (!dao.update(lmc.getMovimento())) {
                        dao.rollback();
                        GenericaMensagem.error("Erro", "Não foi possível inativar movimentos!");
                        return;
                    }
                }
            }
        }
        matriculaEscola.setEscStatus((EscStatus) dao.find(new EscStatus(), 3));
        matriculaEscola.setStatus(DataHoje.dataHoje());
        //matriculaEscola.setHabilitado(false);
        if (!dao.update(matriculaEscola)) {
            dao.rollback();
            GenericaMensagem.error("Erro", "Não foi possível inativar matrícula!");
            return;
        }

        matriculaEscola.getServicoPessoa().setAtivo(false);
        if (!dao.update(matriculaEscola.getServicoPessoa())) {
            dao.rollback();
            GenericaMensagem.error("Erro", "Não foi possível Alterar Serviço Pessoa!");
            return;
        }

        if (Moeda.converteUS$(valorMulta) <= 0) {
            GenericaMensagem.info("Sucesso", "Matrícula Rescindida!");
            dao.commit();
            pesquisaMovimentosPorMatricula();
            NovoLog novoLog = new NovoLog();
            novoLog.setTabela("matr_escola");
            novoLog.setCodigo(matriculaEscola.getId());
            String cobrancaMovimento = "";
            try {
                cobrancaMovimento = matriculaEscola.getServicoPessoa().getCobrancaMovimento().getNome();
            } catch (Exception e) {

            }
            novoLog.update("",
                    "ID: " + matriculaEscola.getId()
                    + " - Aluno: (" + matriculaEscola.getServicoPessoa().getPessoa().getId() + ") " + matriculaEscola.getServicoPessoa().getPessoa().getNome()
                    + " - Responsável: (" + matriculaEscola.getServicoPessoa().getCobranca().getId() + ") " + matriculaEscola.getServicoPessoa().getCobranca().getNome()
                    + " - Cobrança: " + cobrancaMovimento
                    + " - Serviço: " + matriculaEscola.getServicoPessoa().getServicos().getDescricao()
            );
            return;
        }

        if (gerarMovimentoMulta(dao, servicos, matriculaEscola.getServicoPessoa().getPessoa())) {
            listaMovimento.clear();
            GenericaMensagem.info("Sucesso", "Matrícula rescindida!");
            dao.commit();
            pesquisaMovimentosPorMatricula();
        } else {
            GenericaMensagem.error("Erro", "Não foi possível inativar matrícula!");
            dao.rollback();
        }
    }

    public boolean gerarMovimentoMulta(Dao dao, Servicos s, Pessoa p) {
        try {
            String mesPrimeiraParcela = listaMesVencimento.get(idMesVencimento).getDescription();
            String mes = mesPrimeiraParcela.substring(0, 2);
            String ano = mesPrimeiraParcela.substring(3, 7);
            String referencia = mes + "/" + ano;
            String vencimento = DataHoje.DataToArray(DataHoje.data())[0] + "/" + referencia;
            Movimento m = null;
//            if (DataHoje.qtdeDiasDoMes(Integer.parseInt(mes), Integer.parseInt(ano)) >= diaVencimento) {
//                if (diaVencimento < 10) {
//                    vencimento = "0" + diaVencimento + "/" + mes + "/" + ano;
//                } else {
//                    vencimento = diaVencimento + "/" + mes + "/" + ano;
//                }
//            } else {
//                String diaSwap = Integer.toString(DataHoje.qtdeDiasDoMes(Integer.parseInt(mes), Integer.parseInt(ano)));
//                if (diaSwap.length() < 2) {
//                    diaSwap = "0" + diaSwap;
//                }
//                vencimento = diaSwap + "/" + mes + "/" + ano;
//            }

            double valorParcelaF;
            String nrCtrBoletoResp = "";
            String vencimentoString = "";
            TipoServico tipoServico = (TipoServico) dao.find(new TipoServico(), 6);
            FTipoDocumento fTipoDocumento = (FTipoDocumento) dao.find(new FTipoDocumento(), 2);
            for (int x = 0; x < (Integer.toString(matriculaEscola.getServicoPessoa().getCobranca().getId())).length(); x++) {
                nrCtrBoletoResp += 0;
            }
            nrCtrBoletoResp += matriculaEscola.getServicoPessoa().getCobranca().getId();
            if (numeroParcelas > 0) {
                valorParcelaF = Moeda.substituiVirgulaDouble(valorMulta) / numeroParcelas;
            } else {
                valorParcelaF = Moeda.substituiVirgulaDouble(valorMulta);
            }
            int nr = numeroParcelas;
            if (numeroParcelas < 1) {
                nr = 1;
            }
            int idCondicaoPagto;
            if (numeroParcelas == 1) {
                idCondicaoPagto = 1;
            } else {
                idCondicaoPagto = 2;
            }

            Lote l = new Lote(
                    -1,
                    (Rotina) new Dao().find(new Rotina(), 151),
                    "R",
                    DataHoje.data(),
                    matriculaEscola.getServicoPessoa().getCobranca(),
                    matriculaEscola.getServicoPessoa().getServicos().getPlano5(),
                    false,
                    "",
                    Moeda.substituiVirgulaDouble(valorMulta),
                    matriculaEscola.getServicoPessoa().getServicos().getFilial(),
                    null,
                    matriculaEscola.getServicoPessoa().getEvt(),
                    "",
                    fTipoDocumento,
                    (CondicaoPagamento) dao.find(new CondicaoPagamento(), idCondicaoPagto),
                    (FStatus) new Dao().find(new FStatus(), 1),
                    null,
                    false,
                    0,
                    null,
                    null,
                    null,
                    false,
                    "",
                    null,
                    ""
            );

            if (!dao.save(l)) {
                dao.rollback();
                return false;
            }

            for (int i = 0; i < nr; i++) {
                if (i > 0) {
                    referencia = mes + "/" + ano;
                    vencimentoString = (new DataHoje()).incrementarMeses(i, vencimento);
                    mes = vencimentoString.substring(3, 5);
                    ano = vencimentoString.substring(6, 10);
                } else {
                    vencimentoString = vencimento;
                }
                String nrCtrBoleto = nrCtrBoletoResp + Long.toString(DataHoje.calculoDosDias(DataHoje.converte("07/10/1997"), DataHoje.converte(vencimento)));
                m = new Movimento(
                        -1,
                        l,
                        servicos.getPlano5(),
                        p, // EMPRESA DO RESPONSÁVEL (SE DESCONTO FOLHA) OU RESPONSÁVEL (SE NÃO FOR DESCONTO FOLHA)
                        servicos,
                        null,
                        tipoServico,
                        null,
                        valorParcelaF,
                        referencia,
                        vencimentoString,
                        1,
                        true,
                        "E",
                        false,
                        titular, // TITULAR / RESPONSÁVEL
                        beneficiario, // BENEFICIÁRIO
                        "",
                        nrCtrBoleto,
                        vencimento,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        fTipoDocumento,
                        0,
                        new MatriculaSocios());
                if (!dao.save(m)) {
                    dao.rollback();
                    return false;
                }
            }
        } catch (Exception e) {
            dao.rollback();
            return false;
        }
        return true;
    }

    public String getTipoRescisaoContrato() {
        if (GenericaSessao.exists("tipoRescisaoContrato")) {
            tipoRescisaoContrato = GenericaSessao.getString("tipoRescisaoContrato");
        }
        return tipoRescisaoContrato;
    }

    public void setTipoRescisaoContrato(String tipoRescisaoContrato) {
        this.tipoRescisaoContrato = tipoRescisaoContrato;
    }

    public void defineTipoRescisaoContrato(String tipoRescisaoContrato) {
        GenericaSessao.put("tipoRescisaoContrato", tipoRescisaoContrato);
    }

    public Pessoa getBeneficiario() {
        return beneficiario;
    }

    public void setBeneficiario(Pessoa beneficiario) {
        this.beneficiario = beneficiario;
    }

    public Pessoa getTitular() {
        return titular;
    }

    public void setTitular(Pessoa titular) {
        this.titular = titular;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public String getValorMulta() {
        return valorMulta;
    }

    public void setValorMulta(String valorMulta) {
        this.valorMulta = valorMulta;
    }

    public List<Movimento> getMovimentosRescisao() {
        return movimentosRescisao;
    }

    public void setMovimentosRescisao(List<Movimento> movimentosRescisao) {
        this.movimentosRescisao = movimentosRescisao;
    }

    public String getDescricaoServico() {
        return descricaoServico;
    }

    public void setDescricaoServico(String descricaoServico) {
        this.descricaoServico = descricaoServico;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdMesVencimento() {
        return idMesVencimento;
    }

    public void setIdMesVencimento(int idMesVencimento) {
        this.idMesVencimento = idMesVencimento;
    }

    public List<SelectItem> getListaMesVencimento() {
        if (listaMesVencimento.isEmpty()) {
            DataHoje dh = new DataHoje();
            String data = DataHoje.data();
            String mesAno;
            int nr = numeroParcelas;
            if (nr < 2) {
                nr = 6;
            }
            for (int i = 0; i < nr; i++) {
                if (i > 0) {
                    data = dh.incrementarMeses(1, data);
                }
                mesAno = data.substring(3, 5) + "/" + data.substring(6, 10);
                listaMesVencimento.add(new SelectItem(i, mesAno, mesAno));
            }
        }
        return listaMesVencimento;
    }

    public void setListaMesVencimento(List<SelectItem> listaMesVencimento) {
        this.listaMesVencimento = listaMesVencimento;
    }

    public int getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(int diaVencimento) {
        this.diaVencimento = diaVencimento;
    }

    public Registro getRegistro() {
        if (registro.getId() == -1) {
            registro = (Registro) new Dao().find(new Registro(), 1);
        }
        return registro;
    }

    public void setRegistro(Registro registro) {
        this.registro = registro;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public int getNumeroParcelas() {
        return numeroParcelas;
    }

    public void setNumeroParcelas(int numeroParcelas) {
        this.numeroParcelas = numeroParcelas;
    }

    public String getValorTotal() {
        double valorT = 0;
        if (numeroParcelas > 0) {
            if (Moeda.converteUS$(valorMulta) > 0) {
                valorT = Moeda.converteUS$(valorMulta) / numeroParcelas;
            }
        }
        if (numeroParcelas < 2) {
            valorTotal = "O valor da multa será pago em parcela única no valor de R$" + valorMulta;
        } else {
            valorTotal = "O valor da multa será pago em " + numeroParcelas + " parcelas de R$" + Moeda.converteR$Double(valorT);
        }
        return valorTotal;
    }

    public void setValorTotal(String valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getDataGeracao() {
        return dataGeracao;
    }

    public void setDataGeracao(String dataGeracao) {
        this.dataGeracao = dataGeracao;
    }

    public Integer getParcelasRestantes() {
        return parcelasRestantes;
    }

    public void setParcelasRestantes(Integer parcelasRestantes) {
        this.parcelasRestantes = parcelasRestantes;
    }

    public void defineFilialPesquisa() {
        MatriculaEscolaBean matriculaEscolaBean = new MatriculaEscolaBean();
        matriculaEscolaBean.init();
        GenericaSessao.put("matriculaEscolaBean", matriculaEscolaBean);
        for (int i = 0; i < ((MatriculaEscolaBean) GenericaSessao.getObject("matriculaEscolaBean")).getListFiliais().size(); i++) {
            if (Integer.parseInt(((MatriculaEscolaBean) GenericaSessao.getObject("matriculaEscolaBean")).getListFiliais().get(i).getDescription()) == Integer.parseInt(getListFiliais().get(filial_id).getDescription())) {
                ((MatriculaEscolaBean) GenericaSessao.getObject("matriculaEscolaBean")).setFilial_id(i);
                break;
            }
        }
    }

    public Boolean getLiberaAcessaFilial() {
        return liberaAcessaFilial;
    }

    public void setLiberaAcessaFilial(Boolean liberaAcessaFilial) {
        this.liberaAcessaFilial = liberaAcessaFilial;
    }

    public List<SelectItem> getListFiliais() {
        if (listFiliais.isEmpty()) {
            Filial f = MacFilial.getAcessoFilial().getFilial();
            if (f.getId() != -1) {
                if (liberaAcessaFilial || Usuario.getUsuario().getId() == 1) {
                    liberaAcessaFilial = true;
                    // ROTINA MATRÍCULA ESCOLA
                    List<FilialRotina> list = new FilialRotinaDao().findByRotina(new Rotina().get().getId());
                    // ID DA FILIAL
                    if (!list.isEmpty()) {
                        for (int i = 0; i < list.size(); i++) {
                            if (i == 0) {
                                filial_id = i;
                            }
                            if (Objects.equals(f.getId(), list.get(i).getFilial().getId())) {
                                filial_id = i;
                            }
                            listFiliais.add(new SelectItem(i, list.get(i).getFilial().getFilial().getPessoa().getNome(), "" + list.get(i).getFilial().getId()));
                        }
                    } else {
                        filial_id = 0;
                        listFiliais.add(new SelectItem(0, f.getFilial().getPessoa().getNome(), "" + f.getId()));
                    }
                } else {
                    filial_id = 0;
                    listFiliais.add(new SelectItem(0, f.getFilial().getPessoa().getNome(), "" + f.getId()));
                }
            }
        }
        return listFiliais;
    }

    public void setListFiliais(List<SelectItem> listFiliais) {
        this.listFiliais = listFiliais;
    }

    public Integer getFilial_id() {
        return filial_id;
    }

    public void setFilial_id(Integer filial_id) {
        this.filial_id = filial_id;
    }

    public List<ListaMovimentoCheck> getListaMovimento() {
        return listaMovimento;
    }

    public void setListaMovimento(List<ListaMovimentoCheck> listaMovimento) {
        this.listaMovimento = listaMovimento;
    }

    public String getMotivoInativacao() {
        return motivoInativacao;
    }

    public void setMotivoInativacao(String motivoInativacao) {
        this.motivoInativacao = motivoInativacao;
    }

    public Boolean getChkSeleciona() {
        return chkSeleciona;
    }

    public void setChkSeleciona(Boolean chkSeleciona) {
        this.chkSeleciona = chkSeleciona;
    }

    public class ListaMovimentoCheck {

        private Boolean check;
        private Movimento movimento;

        public ListaMovimentoCheck() {
            this.check = true;
            this.movimento = new Movimento();
        }

        public ListaMovimentoCheck(Boolean check, Movimento movimento) {
            this.check = check;
            this.movimento = movimento;
        }

        public Boolean getCheck() {
            return check;
        }

        public void setCheck(Boolean check) {
            this.check = check;
        }

        public Movimento getMovimento() {
            return movimento;
        }

        public void setMovimento(Movimento movimento) {
            this.movimento = movimento;
        }

    }

}
