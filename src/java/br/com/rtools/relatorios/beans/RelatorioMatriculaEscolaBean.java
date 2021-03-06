package br.com.rtools.relatorios.beans;

import br.com.rtools.associativo.Midia;
import br.com.rtools.escola.EscStatus;
import br.com.rtools.escola.Professor;
import br.com.rtools.escola.Turma;
import br.com.rtools.escola.Vendedor;
import br.com.rtools.escola.dao.MatriculaEscolaDao;
import br.com.rtools.financeiro.Servicos;
import br.com.rtools.homologacao.Status;
import br.com.rtools.pessoa.Filial;
import br.com.rtools.pessoa.Fisica;
import br.com.rtools.pessoa.Pessoa;
import br.com.rtools.relatorios.RelatorioOrdem;
import br.com.rtools.relatorios.Relatorios;
import br.com.rtools.relatorios.dao.RelatorioMatriculaEscolaDao;
import br.com.rtools.relatorios.dao.RelatorioOrdemDao;
import br.com.rtools.relatorios.dao.RelatorioDao;
import br.com.rtools.seguranca.Usuario;
import br.com.rtools.sistema.Mes;
import br.com.rtools.utilitarios.Dao;
import br.com.rtools.utilitarios.DataHoje;
import br.com.rtools.utilitarios.GenericaMensagem;
import br.com.rtools.utilitarios.GenericaSessao;
import br.com.rtools.utilitarios.Jasper;
import br.com.rtools.utilitarios.PF;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;
import org.primefaces.component.accordionpanel.AccordionPanel;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

@ManagedBean
@SessionScoped
public class RelatorioMatriculaEscolaBean implements Serializable {

    private Fisica aluno;
    private Pessoa responsavel;
    private Turma turma;
    private List<Turma> listTurma;
    private Usuario operador;
    private List<SelectItem>[] listSelectItem;
    private Boolean[] filtro;
    private Boolean[] disabled;
    private Date dataMatriculaInicial;
    private Date dataMatriculaFinal;
    private Date dataInicial;
    private Date dataFinal;
    private Integer idadeInicial;
    private Integer idadeFinal;
    private String tipoIdade;
    private Integer[] index;
    private String tipoRelatorio;
    private String tipo;
    private String indexAccordion;
    private String order;
    private String[] horario;
    private String sexo;
    private Boolean printHeader;
    private Boolean socios;
    private Boolean tipoMatricula;
    private String title;
    private Map<String, String> listMeses;
    private List selectedMesesAniversario;
    private String ano;
    private String filtrarStatusPor;

    @PostConstruct
    public void init() {
        filtrarStatusPor = "ano";
        horario = new String[2];
        horario[0] = "";
        horario[1] = "";
        disabled = new Boolean[2];
        disabled[0] = false;
        disabled[1] = false;
        filtro = new Boolean[18];
        filtro[0] = false; // FILIAL
        filtro[1] = false; // PERÍODO MATRÍCULA
        // filtro[2] = false; // PERÍODO
        filtro[3] = false; // IDADE
        filtro[4] = false; // STATUS
        filtro[5] = false; // MÍDIA
        filtro[6] = false; // PROFESSOR
        filtro[7] = false; // VENDEDOR
        filtro[8] = false; // ALUNO
        filtro[9] = false; // SEXO
        filtro[10] = false; // RESPONSÁVEL
        filtro[11] = false; // TIPO MATRÍCULA
        filtro[12] = false; // TURMA
        filtro[13] = false; // CURSO
        filtro[14] = false; // HORÁRIO
        filtro[15] = false; // ORDER
        filtro[16] = false; // 
        filtro[17] = false; // ANIVERSÁRIANTES 
        listSelectItem = new ArrayList[7];
        listSelectItem[0] = new ArrayList<>();
        listSelectItem[1] = new ArrayList<>();
        listSelectItem[2] = new ArrayList<>();
        listSelectItem[3] = new ArrayList<>();
        listSelectItem[4] = new ArrayList<>();
        listSelectItem[5] = new ArrayList<>();
        listSelectItem[6] = new ArrayList<>();
        dataInicial = null;
        dataFinal = null;
        dataMatriculaInicial = DataHoje.dataHoje();
        dataMatriculaFinal = null;
        idadeInicial = 0;
        idadeFinal = 0;
        tipoIdade = "apartir";
        index = new Integer[7];
        index[0] = null;
        index[1] = null;
        index[2] = null;
        index[3] = null;
        index[4] = null;
        index[5] = null;
        index[6] = null;
        tipoRelatorio = "Simples";
        indexAccordion = "Simples";
        order = "";
        aluno = new Fisica();
        responsavel = new Pessoa();
        turma = new Turma();
        listTurma = new ArrayList<>();
        sexo = "";
        tipo = "todos";
        printHeader = false;
        socios = false;
        tipoMatricula = false;
        title = "Turma";
        if (GenericaSessao.exists("title")) {
            title = GenericaSessao.getString("title", true);
        }
        if (GenericaSessao.exists("tipoMatricula")) {
            tipoMatricula = GenericaSessao.getBoolean("tipoMatricula", true);
        }
        ano = "";
        loadMeses();
    }

    @PreDestroy
    public void destroy() {
        GenericaSessao.remove("relatorioMatriculaEscolaBean");
        GenericaSessao.remove("fisicaPesquisa");
        GenericaSessao.remove("juridicaPesquisa");
        GenericaSessao.remove("pessoaPesquisa");
        GenericaSessao.remove("usuarioPesquisa");
        GenericaSessao.remove("tipoPesquisaPessoaJuridica");
    }

    public void print() {
        print(0);
    }

    public void print(int tcase) {
        Dao dao = new Dao();
        Relatorios relatorios;
        if (!getListTipoRelatorios().isEmpty()) {
            RelatorioDao rgdb = new RelatorioDao();
            relatorios = rgdb.pesquisaRelatorios(Integer.parseInt(listSelectItem[0].get(index[0]).getDescription()));
        } else {
            GenericaMensagem.info("Sistema", "Nenhum relatório encontrado!");
            return;
        }
        if (relatorios == null) {
            return;
        }
        String detalheRelatorio = "";
        Integer idResponsavel = null;
        Integer idAluno = null;
        Integer idFilial = null;
        Integer idStatus = null;
        Integer idMidia = null;
        Integer idProfessor = null;
        Integer idVendedor = null;
        String inIdCursoOuTurma = null;
        String dataMatricula[] = new String[]{"", ""};
        Integer idade[] = new Integer[]{0, 0};
        String sexoString = null;
        List listDetalhePesquisa = new ArrayList();
        if (filtro[1]) {
            dataMatricula[0] = DataHoje.converteData(dataMatriculaInicial);
            dataMatricula[1] = DataHoje.converteData(dataMatriculaFinal);
            listDetalhePesquisa.add(" Período de matrícula: " + dataMatricula[0] + " e " + dataMatricula[1]);
        }
        if (filtro[3]) {
            idade[0] = idadeInicial;
            idade[1] = idadeFinal;
            listDetalhePesquisa.add(" Idade: " + idade[0] + " e " + idade[1]);
        }
        if (filtro[9]) {
            if (sexo != null) {
                switch (sexo) {
                    case "M":
                        sexoString = "Masculino";
                        break;
                    case "F":
                        sexoString = "Feminino";
                        break;
                    default:
                        sexoString = "Todos";
                        break;
                }
            }
            listDetalhePesquisa.add("Sexo: " + sexoString + "");
        }
        if (responsavel.getId() != -1) {
            idResponsavel = responsavel.getId();
            listDetalhePesquisa.add("Empresa: " + responsavel.getDocumento() + " - " + responsavel.getNome());
        }
        if (aluno.getId() != -1) {
            idAluno = aluno.getPessoa().getId();
            listDetalhePesquisa.add("Funcionário: " + aluno.getPessoa().getDocumento() + " - " + aluno.getPessoa().getNome());
        }
        if (filtro[12]) {
            if (listTurma.isEmpty()) {
                if (turma.getId() != -1) {
                    inIdCursoOuTurma = "" + turma.getId();
                }
            } else {
                inIdCursoOuTurma = "";
                listDetalhePesquisa.add("Turma:");
                int y = 0;
                for (int i = 0; i < listTurma.size(); i++) {
                    if (listTurma.get(i).getSelected()) {
                        if (y == 0) {
                            inIdCursoOuTurma += "" + listTurma.get(i).getId();
                        } else {
                            inIdCursoOuTurma += "," + listTurma.get(i).getId();
                        }
                        y++;
                    }
                }
            }
        }
        if (index[1] != null) {
            idFilial = Integer.parseInt(listSelectItem[1].get(index[1]).getDescription());
            listDetalhePesquisa.add("Filial: " + ((Filial) dao.find(new Filial(), idFilial)).getFilial().getPessoa().getNome());
        }
        String periodo[] = new String[]{"", ""};
        if (filtro[4]) {
            if (index[2] != null) {
                listDetalhePesquisa.add("Status: " + ((Status) dao.find(new Status(), index[2])).getDescricao());
                idStatus = index[2];
            }
            if (index[2] != 1) {
                periodo[0] = DataHoje.converteData(dataInicial);
                periodo[1] = DataHoje.converteData(dataFinal);
                if (ano.isEmpty()) {
                    listDetalhePesquisa.add(" Período do curso: " + periodo[0] + " e " + periodo[1]);
                } else {
                    listDetalhePesquisa.add(" Ano de : " + ano);
                }
            }
        }
        if (index[3] != null) {
            idMidia = Integer.parseInt(listSelectItem[3].get(index[3]).getDescription());
            listDetalhePesquisa.add("Mídia: " + ((Midia) dao.find(new Midia(), idMidia)).getDescricao());
        }
        if (index[4] != null) {
            idProfessor = Integer.parseInt(listSelectItem[4].get(index[4]).getDescription());
            listDetalhePesquisa.add("Professor: " + ((Professor) dao.find(new Professor(), idProfessor)).getProfessor().getNome());
        }
        if (index[5] != null) {
            idVendedor = Integer.parseInt(listSelectItem[5].get(index[5]).getDescription());
            listDetalhePesquisa.add("Vendedor: " + ((Vendedor) dao.find(new Vendedor(), idVendedor)).getPessoa().getNome());
        }
        String meses_aniversario = null;
        if (filtro[17]) {
            meses_aniversario = inMesesAniversario();
        }
        if (tipoMatricula) {
            if (turma.getId() != -1) {
                inIdCursoOuTurma = "" + turma.getId();
            }
        } else if (index[6] != null) {
            inIdCursoOuTurma = "" + Integer.parseInt(listSelectItem[6].get(index[6]).getDescription());
            listDetalhePesquisa.add("Curso: " + ((Servicos) dao.find(new Servicos(), Integer.parseInt(inIdCursoOuTurma))).getDescricao());
        }
        if (order == null) {
            order = "";
        }
        RelatorioMatriculaEscolaDao relatorioMatriculaEscolaDao = new RelatorioMatriculaEscolaDao();
        relatorioMatriculaEscolaDao.setOrder(order);
        relatorioMatriculaEscolaDao.setRelatorios(relatorios);
        List list = relatorioMatriculaEscolaDao.find(idFilial, dataMatricula, periodo, ano, tipoIdade, idade, idStatus, idMidia, idProfessor, idVendedor, tipoMatricula, inIdCursoOuTurma, idAluno, sexo, idResponsavel, horario, meses_aniversario);
        if (list.isEmpty()) {
            GenericaMensagem.info("Sistema", "Não existem registros para o relatório selecionado");
            return;
        }

        if (listDetalhePesquisa.isEmpty()) {
            detalheRelatorio += "Pesquisar todos registros!";
        } else {
            detalheRelatorio += "";
            for (int i = 0; i < listDetalhePesquisa.size(); i++) {
                if (i == 0) {
                    detalheRelatorio += "Detalhes: " + listDetalhePesquisa.get(i).toString();
                } else {
                    detalheRelatorio += "; " + listDetalhePesquisa.get(i).toString();
                }
            }
        }
        List<RelatorioEscolaCadastral> pec = new ArrayList<>();
        for (Object list1 : list) {
            List o = (List) list1;
            pec.add(new RelatorioEscolaCadastral(
                    o.get(0), // ALUNO
                    o.get(1), // IDADE
                    o.get(2), // STATUS
                    o.get(4), // CURSO
                    o.get(3), // STATUS
                    o.get(5), // INICIO
                    o.get(6), // TÉRMINO
                    o.get(7), // SÓCIO CATEGORIA
                    o.get(8), // DATA STATUS
                    o.get(9) // [TURMA DESCRICAO]
            ));
        }
        Map map = new HashMap();
        map.put("detalhes_relatorio", detalheRelatorio);
        map.put("tipo_matricula", (" Matrícula " + title).toUpperCase());
        if (!pec.isEmpty()) {
            Jasper.TYPE = "paisagem";
            Jasper.IS_HEADER = printHeader;
            Jasper.printReports(relatorios.getJasper(), "matricula_escola", (Collection) pec, map);
        }
    }

    public List<SelectItem> getListTipoRelatorios() {
        if (listSelectItem[0].isEmpty()) {
            RelatorioDao db = new RelatorioDao();
            List<Relatorios> list;
            if (tipoMatricula) {
                list = (List<Relatorios>) db.pesquisaTipoRelatorio(282);
            } else {
                list = (List<Relatorios>) db.pesquisaTipoRelatorio(283);

            }
            for (int i = 0; i < list.size(); i++) {
                if (index[0] == null) {
                    if (i == 0) {
                        index[0] = i;
                    }
                }
                listSelectItem[0].add(new SelectItem(i, list.get(i).getNome(), "" + list.get(i).getId()));
            }
            if (listSelectItem[0].isEmpty()) {
                listSelectItem[0] = new ArrayList<>();
            }
        }
        return listSelectItem[0];
    }

    public List<SelectItem> getListRelatorioOrdem() {
        listSelectItem[1].clear();
        if (index[0] != null) {
            RelatorioOrdemDao relatorioOrdemDao = new RelatorioOrdemDao();
            List<RelatorioOrdem> list = relatorioOrdemDao.findAllByRelatorio(Integer.parseInt(getListTipoRelatorios().get(index[0]).getDescription()));
            for (int i = 0; i < list.size(); i++) {
                listSelectItem[1].add(new SelectItem(i, list.get(i).getNome(), "" + list.get(i).getId()));
            }
        }
        return listSelectItem[1];
    }

    public String getTipoRelatorio() {
        return tipoRelatorio;
    }

    public void setTipoRelatorio(String tipoRelatorio) {
        this.tipoRelatorio = tipoRelatorio;
    }

    public void tipoRelatorioChange(TabChangeEvent event) {
        tipoRelatorio = event.getTab().getTitle();
        indexAccordion = ((AccordionPanel) event.getComponent()).getActiveIndex();
        if (tipoRelatorio.equals("Simples")) {
            clear();
        }
    }

    public void selecionaPeriodoMatriculaInicial(SelectEvent event) {
        SimpleDateFormat format = new SimpleDateFormat("d/M/yyyy");
        this.dataMatriculaInicial = DataHoje.converte(format.format(event.getObject()));
    }

    public void selecionaPeriodoMatriculaFinal(SelectEvent event) {
        SimpleDateFormat format = new SimpleDateFormat("d/M/yyyy");
        this.dataMatriculaFinal = DataHoje.converte(format.format(event.getObject()));
    }

    public void selecionaDataInicial(SelectEvent event) {
        ano = "";
        SimpleDateFormat format = new SimpleDateFormat("d/M/yyyy");
        this.dataInicial = DataHoje.converte(format.format(event.getObject()));
    }

    public void selecionaDataFinal(SelectEvent event) {
        ano = "";
        SimpleDateFormat format = new SimpleDateFormat("d/M/yyyy");
        this.dataFinal = DataHoje.converte(format.format(event.getObject()));
    }

    public void clear() {
        if (!filtro[0]) {
            listSelectItem[1] = new ArrayList();
            index[1] = null;
        }
        if (!filtro[1]) {
            dataMatriculaInicial = DataHoje.dataHoje();
            dataMatriculaFinal = null;
        }
//        if (!filtro[2]) {
//            dataInicial = DataHoje.dataHoje();
//            dataFinal = null;
//        }
        if (!filtro[3]) {
            idadeInicial = 0;
            idadeFinal = 0;
            tipoIdade = "apartir";
        }
        if (!filtro[4]) {
            listSelectItem[2] = new ArrayList();
            index[2] = null;
            dataInicial = null;
            dataFinal = null;
            ano = "";
            filtrarStatusPor = "ano";
        }
        if (!filtro[5]) {
            listSelectItem[3] = new ArrayList();
            index[3] = null;
        }
        if (!filtro[6]) {
            listSelectItem[4] = new ArrayList();
            index[4] = null;
        }
        if (!filtro[7]) {
            listSelectItem[5] = new ArrayList();
            index[5] = null;
        }
        if (!filtro[8]) {
            aluno = new Fisica();
            disabled[0] = false;
            disabled[1] = false;
        }
        if (!filtro[9]) {
            sexo = "";
        }
        if (!filtro[10]) {
            responsavel = new Pessoa();
            disabled[0] = false;
            disabled[1] = false;
        }
        if (!filtro[12]) {
            turma = new Turma();
            listTurma = new ArrayList<>();
        }
        if (!filtro[13]) {
            listSelectItem[5] = new ArrayList();
            index[5] = null;
        }
        if (!filtro[14]) {
            horario[0] = "";
            horario[1] = "";
        }
        if (!filtro[15]) {
            order = "";
        }
        if (!filtro[17]) {
            order = "";
        }
        if (filtro[8]) {
            disabled[0] = false;
            disabled[1] = true;
        } else if (filtro[10]) {
            disabled[0] = true;
            disabled[1] = false;
        }
    }

    public void add(String tcase) {
        switch (tcase) {
            case "turma":
                if (turma.getId() == -1) {
                    return;
                }
                turma.setSelected(true);
                listTurma.add(turma);
                turma = new Turma();
                break;
        }
    }

    public void remove(String tcase, Turma turma) {
        switch (tcase) {
            case "turma":
                listTurma.remove(turma);
                break;
        }
    }

    public void close(String close) {
        switch (close) {
            case "filial":
                filtro[0] = false;
                listSelectItem[1] = new ArrayList();
                index[1] = null;
                break;
            case "matricula":
                filtro[1] = false;
                dataMatriculaInicial = DataHoje.dataHoje();
                dataMatriculaFinal = null;
                break;
//            case "periodo":
//                filtro[2] = false;
//                dataInicial = DataHoje.dataHoje();
//                dataFinal = null;
//                ano = "";
//                break;
            case "idade":
                filtro[3] = false;
                idadeInicial = 0;
                idadeFinal = 0;
                tipoIdade = "apartir";
                break;
            case "status":
                filtro[4] = false;
                listSelectItem[2] = new ArrayList();
                index[2] = null;
                dataInicial = null;
                dataFinal = null;
                ano = "";
                filtrarStatusPor = "ano";
                break;
            case "midia":
                filtro[5] = false;
                listSelectItem[3] = new ArrayList();
                index[3] = null;
                break;
            case "professor":
                filtro[6] = false;
                listSelectItem[4] = new ArrayList();
                index[4] = null;
                break;
            case "vendedor":
                filtro[7] = false;
                listSelectItem[5] = new ArrayList();
                index[5] = null;
                break;
            case "aluno":
                filtro[8] = false;
                aluno = new Fisica();
                break;
            case "sexo":
                filtro[9] = false;
                sexo = "";
                break;
            case "responsavel":
                filtro[10] = false;
                responsavel = new Pessoa();
                break;
            case "turma":
                filtro[12] = false;
                turma = new Turma();
                disabled[0] = false;
                disabled[1] = false;
                listTurma = new ArrayList<>();
                break;
            case "curso":
                filtro[13] = false;
                listSelectItem[6] = new ArrayList();
                index[6] = null;
                disabled[0] = false;
                disabled[1] = false;
                break;
            case "horario":
                filtro[14] = false;
                horario[0] = "";
                horario[1] = "";
                break;
            case "aniversariantes":
                selectedMesesAniversario = null;
                filtro[17] = false;
                break;
            case "order":
                filtro[15] = false;
                order = "";
                break;
        }
        PF.update("form_relatorio:id_panel");
    }

    public String getIndexAccordion() {
        return indexAccordion;
    }

    public void setIndexAccordion(String indexAccordion) {
        this.indexAccordion = indexAccordion;
    }

    public Date getDataInicial() {
        return dataInicial;
    }

    public void setDataInicial(Date dataInicial) {
        this.dataInicial = dataInicial;
    }

    public Date getDataFinal() {
        return dataFinal;
    }

    public void setDataFinal(Date dataFinal) {
        this.dataFinal = dataFinal;
    }

    public List<SelectItem>[] getListSelectItem() {
        return listSelectItem;
    }

    public void setListSelectItem(List<SelectItem>[] listSelectItem) {
        this.listSelectItem = listSelectItem;
    }

    /**
     * <strong>Index</strong>
     * <ul>
     * <li>[0] Tipos de Relatórios</li>
     * <li>[1] Filial</li>
     * <li>[2] Status </li>
     * <li>[3] Mídia </li>
     * <li>[4] Professor </li>
     * <li>[5] Vendedor </li>
     * </ul>
     *
     * @return Integer
     */
    public Integer[] getIndex() {
        return index;
    }

    public void setIndex(Integer[] index) {
        this.index = index;
    }

    /**
     * <strong>Filtros</strong>
     * <ul>
     * <li>[0] FILIAL</li>
     * <li>[1] PERÍODO MATRÍCULA</li>
     * <li>[2] PERÍODO</li>
     * <li>[3] IDADE</li>
     * <li>[4] STATUS</li>
     * <li>[5] MÍDIA </li>
     * <li>[6] PROFESSOR </li>
     * <li>[7] VENDEDOR </li>
     * <li>[8] ALUNO </li>
     * <li>[9] SEXO </li>
     * <li>[10] RESPONSÁVEL </li>
     * <li>[14] HORÁRIO </li>
     * <li>[15] ORDER </li>
     * <li>[16] TURMA </li>
     * </ul>
     *
     * @return boolean
     */
    public Boolean[] getFiltro() {
        return filtro;
    }

    public void setFiltro(Boolean[] filtro) {
        this.filtro = filtro;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public Fisica getAluno() {
        if (GenericaSessao.exists("fisicaPesquisa")) {
            aluno = ((Fisica) GenericaSessao.getObject("fisicaPesquisa", true));
        }
        return aluno;
    }

    public void setAluno(Fisica aluno) {
        this.aluno = aluno;
    }

    public Pessoa getResponsavel() {
        if (GenericaSessao.exists("pesssoaPesquisa")) {
            responsavel = (Pessoa) GenericaSessao.getObject("pesssoaPesquisa", true);
        }
        return responsavel;
    }

    public void setResponsavel(Pessoa responsavel) {
        this.responsavel = responsavel;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getTipo() {
        return tipo;
    }

    public Usuario getOperador() {
        if (GenericaSessao.exists("usuarioPesquisa")) {
            operador = ((Usuario) GenericaSessao.getObject("usuarioPesquisa", true));
        }
        return operador;
    }

    public void setOperador(Usuario operador) {
        this.operador = operador;
    }

    public List<SelectItem> getListFiliais() {
        if (listSelectItem[1].isEmpty()) {
            Dao dao = new Dao();
            List<Filial> list = (List<Filial>) dao.list(new Filial(), true);
            for (int i = 0; i < list.size(); i++) {
                listSelectItem[1].add(new SelectItem(i,
                        list.get(i).getFilial().getPessoa().getDocumento() + " / " + list.get(i).getFilial().getPessoa().getNome(),
                        Integer.toString(list.get(i).getId())));
            }
        }
        return listSelectItem[1];
    }

    public List<SelectItem> getListStatus() {
        if (listSelectItem[2].isEmpty()) {
            Dao dao = new Dao();
            List<EscStatus> list = (List<EscStatus>) dao.list(new EscStatus());
            for (int i = 0; i < list.size(); i++) {
                if (i == 0) {
                    index[2] = list.get(i).getId();
                }
                listSelectItem[2].add(new SelectItem(list.get(i).getId(), list.get(i).getDescricao()));
            }
        }
        return listSelectItem[2];
    }

    public List<SelectItem> getListMidia() {
        if (listSelectItem[3].isEmpty()) {
            Dao dao = new Dao();
            List<Midia> list = (List<Midia>) dao.list(new Midia(), true);
            for (int i = 0; i < list.size(); i++) {
                listSelectItem[3].add(new SelectItem(i,
                        list.get(i).getDescricao(),
                        Integer.toString(list.get(i).getId())));
            }
        }
        return listSelectItem[3];
    }

    public List<SelectItem> getListProfessor() {
        if (listSelectItem[4].isEmpty()) {
            Dao dao = new Dao();
            List<Professor> list = (List<Professor>) dao.list(new Professor(), true);
            for (int i = 0; i < list.size(); i++) {
                listSelectItem[4].add(new SelectItem(i,
                        list.get(i).getProfessor().getNome(),
                        Integer.toString(list.get(i).getId())));
            }
        }
        return listSelectItem[4];
    }

    public List<SelectItem> getListVendedor() {
        if (listSelectItem[5].isEmpty()) {
            Dao di = new Dao();
            List<Vendedor> list = (List<Vendedor>) di.list(new Vendedor(), true);
            for (int i = 0; i < list.size(); i++) {
                listSelectItem[5].add(new SelectItem(i,
                        list.get(i).getPessoa().getNome(),
                        Integer.toString(list.get(i).getId())));
            }
        }
        return listSelectItem[5];
    }

    public List<SelectItem> getListCursos() {
        if (listSelectItem[6].isEmpty()) {
            MatriculaEscolaDao med = new MatriculaEscolaDao();
            List<Servicos> list = med.listServicosPorMatriculaIndividual();
            for (int i = 0; i < list.size(); i++) {
                listSelectItem[6].add(new SelectItem(i,
                        list.get(i).getDescricao(),
                        Integer.toString(list.get(i).getId())));
            }
        }
        return listSelectItem[6];
    }

    public Boolean getPrintHeader() {
        return printHeader;
    }

    public void setPrintHeader(Boolean printHeader) {
        this.printHeader = printHeader;
    }

    public Boolean[] getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean[] disabled) {
        this.disabled = disabled;
    }

    public Boolean getSocios() {
        return socios;
    }

    public void setSocios(Boolean socios) {
        this.socios = socios;
    }

    public void listener(String tCase) {
        switch (tCase) {
            case "matriculaTurma":
                title = "Turma";
                tipoMatricula = true;
                break;
            case "matriculaIndividual":
                title = "Individual";
                tipoMatricula = false;
                break;
        }
        GenericaSessao.put("title", title);
        GenericaSessao.put("tipoMatricula", tipoMatricula);
    }

    public Date getDataMatriculaInicial() {
        return dataMatriculaInicial;
    }

    public void setDataMatriculaInicial(Date dataMatriculaInicial) {
        this.dataMatriculaInicial = dataMatriculaInicial;
    }

    public Date getDataMatriculaFinal() {
        return dataMatriculaFinal;
    }

    public void setDataMatriculaFinal(Date dataMatriculaFinal) {
        this.dataMatriculaFinal = dataMatriculaFinal;
    }

    public String[] getHorario() {
        return horario;
    }

    public void setHorario(String[] horario) {
        this.horario = horario;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Turma getTurma() {
        if (GenericaSessao.exists("turmaPesquisa")) {
            turma = (Turma) GenericaSessao.getObject("turmaPesquisa", true);
        }
        return turma;
    }

    public void setTurma(Turma turma) {
        this.turma = turma;
    }

    public Boolean getTipoMatricula() {
        return tipoMatricula;
    }

    public void setTipoMatricula(Boolean tipoMatricula) {
        this.tipoMatricula = tipoMatricula;
    }

    public String getIdadeInicialString() {
        return Integer.toString(idadeInicial);
    }

    public void setIdadeInicialString(String idadeInicialString) {
        if (idadeInicialString.isEmpty()) {
            idadeInicialString = "0";
        }
        this.idadeInicial = Integer.parseInt(idadeInicialString);
    }

    public String getIdadeFinalString() {
        return Integer.toString(idadeFinal);
    }

    public void setIdadeFinalString(String idadeFinalString) {
        if (idadeFinalString.isEmpty()) {
            idadeFinalString = "0";
        }
        this.idadeFinal = Integer.parseInt(idadeFinalString);
    }

    public String getTipoIdade() {
        return tipoIdade;
    }

    public void setTipoIdade(String tipoIdade) {
        this.tipoIdade = tipoIdade;
    }

    public List<Turma> getListTurma() {
        return listTurma;
    }

    public void setListTurma(List<Turma> listTurma) {
        this.listTurma = listTurma;
    }

    public Map<String, String> getListMeses() {
        return listMeses;
    }

    public void setListMeses(Map<String, String> listMeses) {
        this.listMeses = listMeses;
    }

    public List getSelectedMesesAniversario() {
        return selectedMesesAniversario;
    }

    public void setSelectedMesesAniversario(List selectedMesesAniversario) {
        this.selectedMesesAniversario = selectedMesesAniversario;
    }

    public String getAno() {
        return ano;
    }

    public void setAno(String ano) {
        if (!ano.isEmpty()) {
            dataInicial = null;
            dataFinal = null;
        }
        this.ano = ano;
    }

    public String getFiltrarStatusPor() {
        return filtrarStatusPor;
    }

    public void setFiltrarStatusPor(String filtrarStatusPor) {
        this.filtrarStatusPor = filtrarStatusPor;
    }

    public class RelatorioEscolaCadastral {

        private Object aluno_nome;
        private Object aluno_idade;
        private Object aluno_sexo;
        private Object curso;
        private Object status;
        private Object data_inicio;
        private Object data_termino;
        private Object socio_categoria;
        private Object data_status;
        private Object turma_descricao;

        public RelatorioEscolaCadastral(Object aluno_nome, Object aluno_idade, Object aluno_sexo, Object curso, Object status, Object data_inicio, Object data_termino, Object socio_categoria, Object data_status, Object turma_descricao) {
            this.aluno_nome = aluno_nome;
            this.aluno_idade = aluno_idade;
            this.aluno_sexo = aluno_sexo;
            this.curso = curso;
            this.status = status;
            this.data_inicio = data_inicio;
            this.data_termino = data_termino;
            this.socio_categoria = socio_categoria;
            this.data_status = data_status;
            this.turma_descricao = turma_descricao;
        }

        public Object getAluno_nome() {
            return aluno_nome;
        }

        public void setAluno_nome(Object aluno_nome) {
            this.aluno_nome = aluno_nome;
        }

        public Object getAluno_idade() {
            return aluno_idade;
        }

        public void setAluno_idade(Object aluno_idade) {
            this.aluno_idade = aluno_idade;
        }

        public Object getAluno_sexo() {
            return aluno_sexo;
        }

        public void setAluno_sexo(Object aluno_sexo) {
            this.aluno_sexo = aluno_sexo;
        }

        public Object getCurso() {
            return curso;
        }

        public void setCurso(Object curso) {
            this.curso = curso;
        }

        public Object getStatus() {
            return status;
        }

        public void setStatus(Object status) {
            this.status = status;
        }

        public Object getData_inicio() {
            return data_inicio;
        }

        public void setData_inicio(Object data_inicio) {
            this.data_inicio = data_inicio;
        }

        public Object getData_termino() {
            return data_termino;
        }

        public void setData_termino(Object data_termino) {
            this.data_termino = data_termino;
        }

        public Object getSocio_categoria() {
            return socio_categoria;
        }

        public void setSocio_categoria(Object socio_categoria) {
            this.socio_categoria = socio_categoria;
        }

        public Object getData_status() {
            return data_status;
        }

        public void setData_status(Object data_status) {
            this.data_status = data_status;
        }

        public Object getTurma_descricao() {
            return turma_descricao;
        }

        public void setTurma_descricao(Object turma_descricao) {
            this.turma_descricao = turma_descricao;
        }
    }

    public void loadMeses() {
        List<Mes> list = new Dao().list(new Mes(), true);
        listMeses = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() < 10) {
                listMeses.put(list.get(i).getDescricao(), "0" + list.get(i).getId());
            } else {
                listMeses.put(list.get(i).getDescricao(), "" + list.get(i).getId());
            }
        }
    }

    public String inMesesAniversario() {
        String ids = null;
        if (selectedMesesAniversario != null) {
            for (int i = 0; i < selectedMesesAniversario.size(); i++) {
                if (selectedMesesAniversario.get(i) != null) {
                    Integer m = 0;
                    try {
                        m = Integer.parseInt(selectedMesesAniversario.get(i) + "");
                    } catch (Exception e) {
                    }
                    if (ids == null) {
                        ids = "'" + m + "'";
                    } else {
                        ids += ",'" + m + "'";
                    }
                }
            }
        }
        return ids;
    }

    public void loadFiltroStatus() {
        ano = "";
        dataInicial = null;
        dataFinal = null;
        if (index[2] == 1) {
            filtrarStatusPor = "ano";
        }
    }
}
