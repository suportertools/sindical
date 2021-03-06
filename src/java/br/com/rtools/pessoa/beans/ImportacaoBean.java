package br.com.rtools.pessoa.beans;

import br.com.rtools.associativo.Parentesco;
import br.com.rtools.associativo.dao.ParentescoDao;
import br.com.rtools.endereco.Bairro;
import br.com.rtools.endereco.Cidade;
import br.com.rtools.endereco.DescricaoEndereco;
import br.com.rtools.endereco.Endereco;
import br.com.rtools.endereco.Logradouro;
import br.com.rtools.endereco.dao.BairroDao;
import br.com.rtools.endereco.dao.CidadeDao;
import br.com.rtools.endereco.dao.DescricaoEnderecoDao;
import br.com.rtools.endereco.dao.EnderecoDao;
import br.com.rtools.endereco.dao.LogradouroDao;
import br.com.rtools.pessoa.Fisica;
import br.com.rtools.pessoa.FisicaImportacao;
import br.com.rtools.pessoa.Juridica;
import br.com.rtools.pessoa.JuridicaImportacao;
import br.com.rtools.pessoa.Pessoa;
import br.com.rtools.pessoa.PessoaComplemento;
import br.com.rtools.pessoa.PessoaEndereco;
import br.com.rtools.pessoa.PessoaProfissao;
import br.com.rtools.pessoa.Porte;
import br.com.rtools.pessoa.Profissao;
import br.com.rtools.pessoa.TipoDocumento;
import br.com.rtools.pessoa.TipoEndereco;
import br.com.rtools.pessoa.dao.FisicaDao;
import br.com.rtools.pessoa.dao.FisicaImportacaoDao;
import br.com.rtools.pessoa.dao.JuridicaImportacaoDao;
import br.com.rtools.pessoa.dao.ProfissaoDao;
import br.com.rtools.pessoa.utilitarios.PessoaUtilitarios;
import br.com.rtools.seguranca.Registro;
import br.com.rtools.seguranca.Rotina;
import br.com.rtools.sistema.ConfiguracaoUpload;
import br.com.rtools.sistema.Critica;
import br.com.rtools.sistema.SisProcesso;
import br.com.rtools.utilitarios.Dao;
import br.com.rtools.utilitarios.GenericaMensagem;
import br.com.rtools.utilitarios.GenericaSessao;
import br.com.rtools.utilitarios.Mask;
import br.com.rtools.utilitarios.Upload;
import br.com.rtools.utilitarios.ValidaDocumentos;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.primefaces.event.FileUploadEvent;

@ManagedBean(name = "importacao")
@SessionScoped
@ApplicationScoped
public class ImportacaoBean implements Serializable {

    private FisicaImportacao fisicaImportacao = new FisicaImportacao();
    private JuridicaImportacao juridicaImportacao = new JuridicaImportacao();
    private List<FisicaImportacao> listFisicaImportacao;
    private List<JuridicaImportacao> listJuridicaImportacao;
    private String type = "fisica";
    private Boolean run = false;
    private Boolean cancel = false;
    private Thread thread;
    private Integer total;

    public void loadListFisicaImportacao() {
        listFisicaImportacao = new ArrayList();
        listJuridicaImportacao = new ArrayList();
    }

    public void uploadPessoa(FileUploadEvent event) {
        ConfiguracaoUpload configuracaoUpload = new ConfiguracaoUpload();
        configuracaoUpload.setArquivo(event.getFile().getFileName());
        configuracaoUpload.setDiretorio("arquivos/importacao/" + type + "/");
        configuracaoUpload.setEvent(event);
        configuracaoUpload.setSubstituir(true);
        configuracaoUpload.setRenomear("importacao.json");
        configuracaoUpload.setResourceFolder(true);
        Upload.enviar(configuracaoUpload, true);
    }

    public void proccess() {
        if (!run) {
            if (type.equals("fisica")) {
                run = true;
                fisica();
                // thread = new Thread(execute);
                // thread.start();
            }
            if (type.equals("dependentes")) {
                run = true;
                fisica();
                // thread = new Thread(execute);
                // thread.start();
            }
            if (type.equals("juridica")) {
                run = true;
                juridica();
                // thread = new Thread(execute);
                // thread.start();
            }

            if (type.equals("contabilidade")) {
                run = true;
                juridica(true);
                // thread = new Thread(execute);
                // thread.start();
            }
        }
    }

    public void finish() {
        if (type.equals("fisica")) {
            finishFisica();
        }
        if (type.equals("dependentes")) {
            finishFisica();
        }
        if (type.equals("juridica") || type.equals("contabilidade")) {
            finishJuridica();
        }
    }

    public void finishFisica() {
        Dao dao = new Dao();
        FisicaDao fisicaDao = new FisicaDao();
        FisicaImportacaoDao fisicaImportacaoDao = new FisicaImportacaoDao();
        List<TipoEndereco> listTipoEndereco = dao.find("TipoEndereco", new int[]{1, 3, 4});
        if (type.equals("fisica")) {
            listFisicaImportacao = fisicaImportacaoDao.findAllTitulares();
        } else if (type.equals("dependentes")) {
            listFisicaImportacao = fisicaImportacaoDao.findAllDependentes();
        }
        dao.openTransaction();
        for (FisicaImportacao fi : listFisicaImportacao) {
            try {
                Boolean a = false;
                if (fi.getDtHomologacao() == null) {
                    PessoaProfissao pessoaProfissao = new PessoaProfissao();
                    TipoDocumento tipoDocumento = null;
                    String d = "";
                    if (fi.getDocumento().isEmpty() || fi.getDocumento().equals("0")) {
                        if (fi.getDtNascimento() != null) {
                            if (fisicaDao.pesquisaFisicaPorNomeNascimento(fi.getNome(), fi.getDtNascimento()) != null) {
                                continue;
                            }
                        }
                        d = fi.getDocumento();
                        tipoDocumento = (TipoDocumento) dao.find(new TipoDocumento(), 4);
                    } else {
                        if (fi.getDocumento().length() == 11) {
                            fi.reviseDocumento();
                            d = Mask.cpf(fi.getDocumento());
                        }
                        if (!fisicaDao.pesquisaFisicaPorDoc(d).isEmpty()) {
                            continue;
                        }
                        fi.setDocumento(d);
                        tipoDocumento = (TipoDocumento) dao.find(new TipoDocumento(), 1);

                    }
                    /**
                     * Pessoa
                     */
                    Pessoa pessoa = new Pessoa();
                    pessoa.setNome(fi.getNome());
                    pessoa.setDocumento(d);
                    pessoa.setTelefone1(fi.getTelefone1());
                    pessoa.setTelefone2(fi.getTelefone2());
                    pessoa.setTelefone3(fi.getTelefone3());
                    pessoa.setTelefone4(fi.getTelefone4());
                    pessoa.setEmail1(fi.getEmail1());
                    pessoa.setEmail2(fi.getEmail2());
                    pessoa.setEmail3(fi.getEmail3());
                    pessoa.setObs(fi.getObservacao());
                    pessoa.setSite(fi.getSite());
                    pessoa.setTipoDocumento(tipoDocumento);
                    if (!dao.save(pessoa)) {
                        dao.rollback();
                        GenericaMensagem.warn("ERRO", "Ao salvar pessoa. " + dao.EXCEPCION.getMessage());
                        return;
                    }
                    /**
                     * Física
                     */
                    Fisica fisica = new Fisica();
                    fisica.setPessoa(pessoa);
                    fisica.setRg(fi.getRg());
                    fisica.setPai(fi.getPai());
                    fisica.setMae(fi.getMae());
                    fisica.setPis(fi.getPis());
                    fisica.setNacionalidade(fi.getNacionalidade());
                    if (fi.getNaturalidadeObjeto() != null) {
                        fisica.setNaturalidade(fi.getNaturalidadeObjeto().getCidade() + " - " + fi.getNaturalidadeObjeto().getUf());
                    }
                    fisica.setSexo(fi.getSexo());
                    fisica.setEstadoCivil(fi.getEstado_civil());
                    fisica.setFoto(fi.getFoto());
                    fisica.setDtAposentadoria(fi.getDtAposentadoria());
                    fisica.setDtNascimento(fi.getDtNascimento());
                    fisica.setCarteira(fi.getCarteira());
                    fisica.setSerie(fi.getSerie());
                    fisica.setTituloEleitor(fi.getTitulo_eleitor());
                    fisica.setTituloZona(fi.getTitulo_zona());
                    fisica.setTituloSecao(fi.getTitulo_secao());
                    fisica.setNit("");
                    fisica.setOrgaoEmissaoRG(fi.getOrgao_emissao_rg());
                    fisica.setUfEmissaoRG(fi.getUf_emissao_rg());
                    if (!dao.save(fisica)) {
                        dao.rollback();
                        GenericaMensagem.warn("ERRO", "Ao salvar física. " + dao.EXCEPCION.getMessage());
                        return;
                    }
                    if (fi.getProfissao() != null) {
                        pessoaProfissao.setFisica(fisica);
                        pessoaProfissao.setProfissao(fi.getProfissaoObjeto());
                        if (!dao.save(pessoaProfissao)) {
                            dao.rollback();
                            GenericaMensagem.warn("ERRO", "Ao salvar pessoa profissao. " + dao.EXCEPCION.getMessage());
                            return;
                        }
                    }
                    if (type.equals("fisica")) {
                        if (fi.getEndereco() != null) {
                            for (int z = 0; z < listTipoEndereco.size(); z++) {
                                PessoaEndereco pessoaEndereco = new PessoaEndereco();
                                pessoaEndereco.setPessoa(pessoa);
                                pessoaEndereco.setComplemento(fi.getComplemento());
                                pessoaEndereco.setNumero(fi.getNumero());
                                pessoaEndereco.setTipoEndereco(listTipoEndereco.get(z));
                                pessoaEndereco.setEndereco(fi.getEndereco());
                                if (!dao.save(pessoaEndereco)) {
                                    dao.rollback();
                                    GenericaMensagem.warn("ERRO", "Ao salvar pessoa endereço. " + dao.EXCEPCION.getMessage());
                                    return;
                                }

                            }
                        }
                        PessoaComplemento pessoaComplemento = new PessoaComplemento();
                        pessoaComplemento.setObsAviso("");
                        pessoaComplemento.setPessoa(pessoa);
                        if (!dao.save(pessoaComplemento)) {
                            dao.rollback();
                            GenericaMensagem.warn("ERRO", "Ao salvar pessoa complemento. " + dao.EXCEPCION.getMessage());
                            return;
                        }
                    }
                    fi.setDtHomologacao(new Date());
                    fi.setFisica(fisica);
                    if (!dao.update(fi)) {
                        dao.rollback();
                        GenericaMensagem.warn("ERRO", "Ao homologar essa importação. " + dao.EXCEPCION.getMessage());
                        return;
                    }
                }
            } catch (Exception e) {
                dao.rollback();
                GenericaMensagem.warn("ERRO", "Ao realizar importação. " + e.getMessage());
                return;
            }

        }
        dao.commit();
        GenericaMensagem.info("Sucesso", "IMPORTAÇÃO REALIZADA COM SUCESSO.");
        // thread = new Thread(execute);
        // thread.start();        
    }

    public void finishJuridica() {
        Dao dao = new Dao();
        JuridicaImportacaoDao juridicaImportacaoDao = new JuridicaImportacaoDao();
        listJuridicaImportacao = new ArrayList();
        if (type.equals("juridica")) {
            listJuridicaImportacao = juridicaImportacaoDao.findAllEmpresas();
        } else if (type.equals("contabilidade")) {
            listJuridicaImportacao = juridicaImportacaoDao.findAllContabilidades();
        }
        List<TipoEndereco> listTipoEndereco = dao.find("TipoEndereco", new int[]{2, 3, 4, 5});
        for (JuridicaImportacao ji : listJuridicaImportacao) {
            dao.openTransaction();
            try {
                if (ji.getDtHomologacao() == null) {
                    TipoDocumento tipoDocumento = null;
//                    String d = "";
                    if (ji.getDocumento() == null || ji.getDocumento().isEmpty() || ji.getDocumento().equals("0")) {
                        tipoDocumento = (TipoDocumento) dao.find(new TipoDocumento(), 4);
                    } else if (ji.getDocumentoSomenteNumeros().length() == 14) {
                        tipoDocumento = (TipoDocumento) dao.find(new TipoDocumento(), 2);
                    } else if (ji.getDocumentoSomenteNumeros().length() == 11) {
                        tipoDocumento = (TipoDocumento) dao.find(new TipoDocumento(), 1);
                    } else {
                        tipoDocumento = (TipoDocumento) dao.find(new TipoDocumento(), 4);
                    }
                    /**
                     * Pessoa
                     */
                    Pessoa pessoa = new Pessoa();
                    pessoa.setNome(ji.getNome());
                    pessoa.setDocumento(ji.getDocumento());
                    pessoa.setTelefone1(ji.getTelefone1());
                    pessoa.setTelefone2(ji.getTelefone2());
                    pessoa.setTelefone3(ji.getTelefone3());
                    pessoa.setTelefone4(ji.getTelefone4());
                    pessoa.setEmail1(ji.getEmail1());
                    pessoa.setEmail2(ji.getEmail2());
                    pessoa.setEmail3(ji.getEmail3());
                    pessoa.setObs(ji.getObservacao());
                    pessoa.setSite(ji.getSite());
                    pessoa.setTipoDocumento(tipoDocumento);
                    pessoa.setDtCriacao(ji.getDtCriacao());
                    pessoa.setDtAtualizacao(new Date());
                    pessoa.setIsTitular(false);
                    if (!dao.save(pessoa)) {
                        dao.rollback();
                        GenericaMensagem.warn("ERRO", "Ao salvar pessoa. " + dao.EXCEPCION.getMessage());
                        return;
                    }
                    /**
                     * Jurídica
                     */
                    Juridica juridica = new Juridica();
                    juridica.setPessoa(pessoa);
                    juridica.setFantasia(ji.getFantasia());
                    if (juridica.getFantasia().isEmpty()) {
                        juridica.setFantasia(ji.getNome());
                    }
                    juridica.setInscricaoEstadual(ji.getInscricao_estadual());
                    juridica.setInscricaoMunicipal(ji.getInscricao_municipal());
                    if (!ji.getContabilidade_nome().isEmpty()) {
                        if (ji.getContabilidade() != null) {
                            juridica.setContabilidade(ji.getContabilidade());
                        }
                    }
                    juridica.setDtAbertura(ji.getDtAbertura());
                    juridica.setDtFechamento(ji.getDtFechamento());
                    juridica.setCnae(ji.getCnae());
                    juridica.setFoto(ji.getFoto());
                    juridica.setContato(ji.getContato());
                    juridica.setResponsavel(ji.getResponsavel());
                    juridica.setPorte(ji.getPorte());
                    juridica.setCobrancaEscritorio(false);
                    juridica.setEmailEscritorio(false);
                    if (!dao.save(juridica)) {
                        dao.rollback();
                        GenericaMensagem.warn("ERRO", "Ao salvar jurídica. " + dao.EXCEPCION.getMessage());
                        return;
                    }
                    if (type.equals("contabilidade")) {
                        if (ji.getIs_contabilidade()) {
                            if (ji.getCodigo() != null && !ji.getCodigo().isEmpty()) {
                                List<JuridicaImportacao> listEmpresas = juridicaImportacaoDao.findEmpresasPorContabilidade(ji.getCodigo());
                                for (int z = 0; z < listEmpresas.size(); z++) {
                                    listEmpresas.get(z).setContabilidade(juridica);
                                    listEmpresas.get(z).getJuridica().setContabilidade(juridica);
                                    listEmpresas.get(z).getJuridica().setCobrancaEscritorio(true);
                                    listEmpresas.get(z).getJuridica().setEmailEscritorio(true);
                                    if (!dao.update(listEmpresas.get(z))) {
                                        dao.rollback();
                                        GenericaMensagem.warn("ERRO", "Ao víncular contabildiade a empresa. " + dao.EXCEPCION.getMessage());
                                        return;
                                    }
                                    if (!dao.update(listEmpresas.get(z).getJuridica())) {
                                        dao.rollback();
                                        GenericaMensagem.warn("ERRO", "Ao víncular contabildiade a empresa. " + dao.EXCEPCION.getMessage());
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    if (ji.getEndereco() != null) {
                        for (int z = 0; z < listTipoEndereco.size(); z++) {
                            PessoaEndereco pessoaEndereco = new PessoaEndereco();
                            pessoaEndereco.setPessoa(pessoa);
                            pessoaEndereco.setComplemento(ji.getComplemento());
                            pessoaEndereco.setNumero(ji.getNumero());
                            pessoaEndereco.setTipoEndereco(listTipoEndereco.get(z));
                            pessoaEndereco.setEndereco(ji.getEndereco());
                            if (!dao.save(pessoaEndereco)) {
                                dao.rollback();
                                GenericaMensagem.warn("ERRO", "Ao salvar pessoa endereço. " + dao.EXCEPCION.getMessage());
                                return;
                            }

                        }
                    }
                    PessoaComplemento pessoaComplemento = new PessoaComplemento();
                    pessoaComplemento.setObsAviso("ATUALIZAR ESTE CADASTRO!");
                    pessoaComplemento.setPessoa(pessoa);
                    if (!dao.save(pessoaComplemento)) {
                        dao.rollback();
                        GenericaMensagem.warn("ERRO", "Ao salvar pessoa complemento. " + dao.EXCEPCION.getMessage());
                        return;
                    }
                    ji.setDtHomologacao(new Date());
                    ji.setJuridica(juridica);
                    ji.setPorte(juridica.getPorte());
                    if (!dao.update(ji)) {
                        dao.rollback();
                        GenericaMensagem.warn("ERRO", "Ao homologar essa importação. " + dao.EXCEPCION.getMessage());
                        return;
                    }
                }
            } catch (Exception e) {
                dao.rollback();
                GenericaMensagem.warn("ERRO", "Ao realizar importação. " + e.getMessage());
                return;
            }
            dao.commit();

        }
        GenericaMensagem.info("Sucesso", "IMPORTAÇÃO REALIZADA COM SUCESSO.");
        // thread = new Thread(execute);
        // thread.start();        
    }

    public void cancel() {
        if (type.equals("fisica") || type.equals("juridica") || type.equals("contabilidade")) {
            run = false;
        }

    }

    private final Runnable execute = new Runnable() {
        @Override
        public void run() {
            // fisica();
        }
    };

    public void fisica() {
        Rotina r = new Rotina().get();
        SisProcesso sisProcesso = new SisProcesso();
        sisProcesso.start();
        try {
            String json = "";
            File file = new File(((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/resources/cliente/" + GenericaSessao.getString("sessaoCliente").toLowerCase() + "/arquivos/importacao/" + type + "/importacao.json"));
            Gson gson = new Gson();
            if (file.exists()) {
                json = FileUtils.readFileToString(file);
                // json = json.replace("[", "{");
                // json = json.replace("]", "}");
                // json = gson.toJson(json);
            }
            List<FisicaImportacao> list = new ArrayList();
            list = gson.fromJson(json, new TypeToken<List<FisicaImportacao>>() {
            }.getType()); // retorna um objeto User.
            total = list.size();
            boolean error = true;
            // System.out.println(String.format("%d-%s", list.toString()));
            Dao dao = new Dao();
            EnderecoDao enderecoDao = new EnderecoDao();
            List<Critica> listCritica = new ArrayList();
            List listNacionalidade = PessoaUtilitarios.loadListPaises();
            Registro registro = Registro.get();
            Endereco enderecoPrincipal = registro.getFilial().getPessoa().getPessoaEndereco().getEndereco();
            for (int i = 0; i < list.size(); i++) {
                List<FisicaImportacao> listVerificacao = new FisicaImportacaoDao().exists(list.get(i).getNome(), list.get(i).getDocumento());
                // FisicaImportacao fiResult = (FisicaImportacao) new FisicaImportacaoDao().find(list.get(i).getNome(), list.get(i).getDocumento());
                try {
                    if (listVerificacao.isEmpty()) {
                        dao.openTransaction();
                        DescricaoEndereco descricaoEndereco = null;
                        Bairro bairro = null;
                        Cidade cidade = null;
                        Profissao profissao = null;
                        list.get(i).reviseDocumento();
                        list.get(i).revisePIS();
                        list.get(i).reviseSexo();
                        list.get(i).reviseEstadoCivil();
                        list.get(i).reviseCNPJ();
                        list.get(i).reviseTelefone();
                        list.get(i).reviseCEP();
                        list.get(i).reviseCTPS();
                        list.get(i).getDtNascimento();
                        list.get(i).getDtAposentadoria();
                        list.get(i).getDtFiliacao();
                        list.get(i).getDtInativacao();
                        list.get(i).getDtCriacao();
                        list.get(i).reviseDatas();
                        if (type.equals("dependentes")) {
                            list.get(i).setDependente(true);
                        }
                        Parentesco parentesco = null;
                        if (!list.get(i).getSexo().isEmpty() && !list.get(i).getParentesco().isEmpty()) {
                            parentesco = new ParentescoDao().find(list.get(i).getParentesco().toUpperCase(), list.get(i).getSexo().toUpperCase());
                            if (parentesco == null) {
                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "PARENTESCO NÃO ENCONTRADO! " + list.get(i).getDocumento()));
                            }
                        } else {
                            listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "PARENTESCO NÃO ESPECIFICADO! " + list.get(i).getDocumento()));
                        }
                        list.get(i).setParentescoObjecto(parentesco);
                        String d = Mask.cpf(list.get(i).getDocumento());
                        if (!list.get(i).getDocumento().isEmpty() && !list.get(i).getDocumento().equals("0")) {
                            if (!ValidaDocumentos.isValidoCPF(list.get(i).getDocumento())) {
                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CPF INVÁLIDO! " + list.get(i).getDocumento()));
                            } else if (list.get(i).getDocumento().length() == 11) {
                                list.get(i).setDocumento(d);
                            } else {
                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CPF COM ERROS NÃO DETECTADO(S)! " + d));
                            }
                        }
                        if (!list.get(i).getPis().isEmpty()) {
                            if (!ValidaDocumentos.isValidoPIS(list.get(i).getPis())) {
                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "PIS INVÁLIDO! " + list.get(i).getPis()));
                            } else {
                                list.get(i).setPis(Mask.pis(list.get(i).getPis()));
                            }
                        }
                        String city = "";
                        String state = "";
                        if (!list.get(i).getNaturalidade().isEmpty()) {
                            String n[] = list.get(i).getNaturalidade().split("-");
                            if (n.length == 0) {
                                n = list.get(i).getNaturalidade().split("/");
                            }
                            city = list.get(i).getNaturalidade();
                            state = enderecoPrincipal.getCidade().getUf();
                            if (n.length > 1) {
                                city = list.get(i).replace(n[0]).toUpperCase().trim();
                                state = list.get(i).replace(n[1]).toUpperCase().trim();
                                if (state.isEmpty()) {
                                    state = enderecoPrincipal.getCidade().getUf();
                                }
                            }
                            Cidade naturalidade = new CidadeDao().find(city, state);
                            if (naturalidade == null) {
                                List<Cidade> listCidade = new CidadeDao().findByCidade(city);
                                if (listCidade.isEmpty()) {
                                    listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "NATURALIDADE NÃO EXISTE NO SISTEMA! " + list.get(i).getNaturalidade()));
                                } else {
                                    naturalidade = listCidade.get(0);
                                    list.get(i).setNaturalidadeObjeto(naturalidade);
                                }
                            } else {
                                list.get(i).setNaturalidadeObjeto(naturalidade);
                            }
                        }
                        if (list.get(i).getNacionalidade().isEmpty()) {
                            list.get(i).setNacionalidade("Brasileira(o)");
                        } else {
                            Boolean nacionalidadeExiste = false;
                            for (int z = 0; z < listNacionalidade.size(); z++) {
                                if (listNacionalidade.get(z).toString().toUpperCase().contains(list.get(i).getNacionalidade().toUpperCase())) {
                                    list.get(i).setNacionalidade(listNacionalidade.get(z).toString());
                                    nacionalidadeExiste = true;
                                    break;
                                }
                            }
                            if (!nacionalidadeExiste) {
                                for (int z = 0; z < listNacionalidade.size(); z++) {
                                    if (listNacionalidade.get(z).toString().toUpperCase().contains(city.toUpperCase())) {
                                        list.get(i).setNacionalidade(listNacionalidade.get(z).toString());
                                        nacionalidadeExiste = true;
                                        break;
                                    }
                                }
                            }
                            if (!nacionalidadeExiste) {
                                for (int z = 0; z < listNacionalidade.size(); z++) {
                                    if (listNacionalidade.get(z).toString().toUpperCase().contains(state.toUpperCase())) {
                                        list.get(i).setNacionalidade(listNacionalidade.get(z).toString());
                                        nacionalidadeExiste = true;
                                        break;
                                    }
                                }
                            }
                            if (!nacionalidadeExiste) {
                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "NÚMERO NACIONALIDADE NÃO EXISTE! " + list.get(i).getNacionalidade()));
                            }
                        }
                        if (list.get(i).getProfissao().isEmpty()) {
                            profissao = (Profissao) dao.find(new Profissao(), 0);
                        } else {
                            profissao = new ProfissaoDao().find(list.get(i).getProfissao());
                            if (profissao == null) {
                                profissao = (Profissao) dao.find(new Profissao(), 0);
                            }
                        }
                        list.get(i).setProfissaoObjeto(profissao);
                        Boolean saveEndereco = true;
                        Endereco endereco = new Endereco();
                        if (!list.get(i).getLogradouro().isEmpty() || !list.get(i).getDescricao_endereco().isEmpty() || !list.get(i).getBairro().isEmpty() || !list.get(i).getCidade().isEmpty()) {
                            list.get(i).setDescricao_endereco(list.get(i).getDescricao_endereco().replace("'", " "));
                            List<Endereco> listEndereco = new ArrayList();
                            if (!list.get(i).getLogradouro().isEmpty() && !list.get(i).getDescricao_endereco().isEmpty() && !list.get(i).getBairro().isEmpty() && !list.get(i).getCidade().isEmpty()) {
                                listEndereco = enderecoDao.pesquisaEnderecoImportacao(list.get(i).getUf(), list.get(i).getCidade(), list.get(i).getLogradouro(), list.get(i).getDescricao_endereco(), list.get(i).getCep(), "T", true);
                                if (listEndereco.isEmpty()) {
                                    listEndereco = enderecoDao.pesquisaEnderecoImportacao(list.get(i).getUf(), list.get(i).getCidade(), list.get(i).getLogradouro(), list.get(i).getDescricao_endereco(), list.get(i).getCep(), "T", false);
                                }
                                endereco = new Endereco();
                            }
                            if (list.get(i).getNumero().isEmpty()) {
                                saveEndereco = false;
                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "NÚMERO NÃO INFORMADO!"));
                            }
                            Logradouro logradouro = null;
                            if (!listEndereco.isEmpty()) {
                                endereco = listEndereco.get(0);
//                                for (int z = 0; z < listEndereco.size(); z++) {
//                                    if (listEndereco.get(z).getCep().endsWith(list.get(i).getCep())) {
//                                        list.get(i).setCep(endereco.getCep());
//                                        break;
//                                    }
//                                }
//                                if (endereco.getId() == -1) {
//                                    endereco = new Endereco();
//                                    endereco = listEndereco.get(0);
//                                    if (endereco != null && endereco.getId() != -1) {
//                                        if (endereco.getCep().isEmpty()) {
//                                            list.get(i).setCep(endereco.getCep());
//                                        }
//                                    }
//                                }
                            } else {
//                                if (list.get(i).getCep().isEmpty()) {
//                                    saveEndereco = false;
//                                    listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CEP NÃO INFORMADO!"));
//                                } else {
//                                    CEPService cEPService = new CEPService();
//                                    cEPService.setCep(list.get(i).getCep());
//                                    cEPService.procurar(dao);
//                                    if (cEPService.getEndereco() == null) {
//                                        endereco = new Endereco();
//                                    } else {
//                                        endereco = cEPService.getEndereco();
//                                    }
//                                }
                                endereco = new Endereco();
                                if (saveEndereco) {
                                    if (endereco.getId() == -1) {
                                        if (list.get(i).getLogradouro().isEmpty()) {
                                            saveEndereco = false;
                                            listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "LOGRADOURO NÃO INFORMADO!"));
                                        } else {
                                            logradouro = new LogradouroDao().find(list.get(i).getLogradouro());
                                            if (logradouro == null) {
                                                saveEndereco = false;
                                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "LOGRADOURO NÃO EXISTE NO SISTEMA! " + list.get(i).getLogradouro()));
                                                // logradouro = (Logradouro) dao.find(new Logradouro(), 0);
                                            }
                                        }
                                        if (list.get(i).getDescricao_endereco().isEmpty()) {
                                            descricaoEndereco = (DescricaoEndereco) dao.find(new DescricaoEndereco(), 0);
                                        } else {
                                            descricaoEndereco = new DescricaoEnderecoDao().find(list.get(i).getDescricao_endereco());
                                            if (descricaoEndereco == null) {
                                                descricaoEndereco = new DescricaoEndereco();
                                                descricaoEndereco.setAtivo(false);
                                                descricaoEndereco.setDescricao(list.get(i).getDescricao_endereco().trim());
                                                if (!dao.save(descricaoEndereco)) {
                                                    dao.rollback();
                                                    return;
                                                }
                                            }
                                        }
                                        if (list.get(i).getBairro().isEmpty()) {
                                            bairro = (Bairro) dao.find(new Bairro(), 0);
                                        } else {
                                            bairro = new BairroDao().find(list.get(i).getBairro());
                                            if (bairro == null) {
                                                bairro = new Bairro();
                                                bairro.setAtivo(false);
                                                bairro.setDescricao(list.get(i).getBairro().trim());
                                                if (!dao.save(bairro)) {
                                                    dao.rollback();
                                                    return;
                                                }
                                            }
                                        }
                                        if (list.get(i).getCidade().isEmpty()) {
                                            saveEndereco = false;
                                            listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CIDADE NÃO INFORMADA!"));
                                            cidade = (Cidade) dao.find(new Cidade(), 0);
                                        } else {
                                            cidade = new CidadeDao().find(list.get(i).getCidade(), list.get(i).getUf());
                                            if (cidade == null) {
                                                saveEndereco = false;
                                                // cidade = (Cidade) dao.find(new Cidade(), 0);
                                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CIDADE NÃO EXISTE NO SISTEMA! " + list.get(i).getCidade()));
                                            }
                                        }
                                    }
                                }

                            }

                            if ((endereco != null && endereco.getId() == -1) || endereco == null && saveEndereco) {
                                if (list.get(i).getCep().isEmpty()) {
                                    saveEndereco = false;
                                    listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CEP NÃO ENCONTRADO!"));
                                }
                                endereco = new Endereco();
                                endereco.setCep(list.get(i).getCep());
                                endereco.setAtivo(false);
                                endereco.setLogradouro(logradouro);
                                endereco.setBairro(bairro);
                                endereco.setCidade(cidade);
                                endereco.setDescricaoEndereco(descricaoEndereco);
                                if (endereco.getId() == -1 && saveEndereco) {
                                    if (!dao.save(endereco)) {
                                        dao.rollback();
                                        return;
                                    }
                                }
                            }
                            if (endereco.getId() != -1) {
                                list.get(i).setEndereco(endereco);
                            }

                        }

                        if (!dao.save(list.get(i))) {
                            dao.rollback();
                            return;
                        }
                        dao.commit();
                    }
                } catch (Exception e2) {
                    e2.getMessage();
                }
                if (!run) {
                    break;
                }
            }
            for (int i = 0; i < listCritica.size(); i++) {
                new Dao().save(listCritica.get(i), true);
            }
            sisProcesso.finish();
            // Saída: id: 123; first name: Wektabyte
        } catch (Exception e) {
            e.getMessage();
        }
        run = false;

    }

    public void juridica() {
        juridica(false);
    }

    public void juridica(Boolean is_contabilidade) {
        Rotina r = new Rotina().get();
        SisProcesso sisProcesso = new SisProcesso();
        sisProcesso.start();
        try {
            String json = "";
            File file = new File(((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/resources/cliente/" + GenericaSessao.getString("sessaoCliente").toLowerCase() + "/arquivos/importacao/" + type + "/importacao.json"));
            Gson gson = new Gson();
            if (file.exists()) {
                json = FileUtils.readFileToString(file);
                // json = json.replace("[", "{");
                // json = json.replace("]", "}");
                // json = gson.toJson(json);
            }
            List<JuridicaImportacao> list = new ArrayList();
            list = gson.fromJson(json, new TypeToken<List<JuridicaImportacao>>() {
            }.getType()); // retorna um objeto User.
            total = list.size();
            boolean error = true;
            // System.out.println(String.format("%d-%s", list.toString()));
            Dao dao = new Dao();
            EnderecoDao enderecoDao = new EnderecoDao();
            List<Critica> listCritica = new ArrayList();
            Registro registro = Registro.get();
            Endereco enderecoPrincipal = registro.getFilial().getPessoa().getPessoaEndereco().getEndereco();
            String documentoxxx = "";
            for (int i = 0; i < list.size(); i++) {
                try {
                    list.get(i).setDocumento_original(list.get(i).getDocumento());
                    if (i == 1078) {
                        i = i;
                    }
                    documentoxxx = list.get(i).getDocumentoSomenteNumeros();
                    List<JuridicaImportacao> listVerificacao = new JuridicaImportacaoDao().find(list.get(i).getNome(), list.get(i).getDocumento_original(), is_contabilidade);
                    if (listVerificacao.isEmpty()) {
                        dao.openTransaction();
                        DescricaoEndereco descricaoEndereco = null;
                        Bairro bairro = null;
                        Cidade cidade = null;
                        if (is_contabilidade) {
                            list.get(i).setIs_contabilidade(true);
                        }
                        list.get(i).revisePorte();
                        list.get(i).reviseDocumento();
                        list.get(i).reviseTelefone();
                        list.get(i).reviseCEP();
                        list.get(i).getDtAbertura();
                        list.get(i).getDtFechamento();
                        list.get(i).getDtInativacao();
                        list.get(i).getDtCriacao();
                        list.get(i).reviseDatas();
                        list.get(i).reviseEmail();
                        String d = "";
                        if (list.get(i).getDocumento().length() == 11) {
                            d = Mask.cpf(list.get(i).getDocumentoSomenteNumeros());
                        }
                        if (list.get(i).getDocumento().length() == 14) {
                            d = Mask.cnpj(list.get(i).getDocumentoSomenteNumeros());
                        }
                        if (!list.get(i).getDocumento().isEmpty() && !list.get(i).getDocumento().equals("0")) {
                            if (!ValidaDocumentos.isValidoCNPJ(list.get(i).getDocumentoSomenteNumeros()) && list.get(i).getDocumentoSomenteNumeros().length() == 14) {
                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CNPJ INVÁLIDO! " + list.get(i).getDocumento()));
                                d = "";
                            } else if (!ValidaDocumentos.isValidoCPF(list.get(i).getDocumentoSomenteNumeros()) && list.get(i).getDocumentoSomenteNumeros().length() == 11) {
                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CPF INVÁLIDO! " + list.get(i).getDocumento()));
                                d = "";
                            } else if (list.get(i).getDocumentoSomenteNumeros().length() == 11) {
                                list.get(i).setDocumento(d);
                            } else if (list.get(i).getDocumentoSomenteNumeros().length() == 14) {
                                list.get(i).setDocumento(d);
                            } else {
                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CNPJ COM ERROS NÃO DETECTADO(S)! " + d));
                                d = "";
                            }
                        }
                        Boolean saveEndereco = true;
                        if (saveEndereco) {
                            Endereco endereco = new Endereco();
                            if (!list.get(i).getLogradouro().isEmpty() || !list.get(i).getDescricao_endereco().isEmpty() || !list.get(i).getBairro().isEmpty() || !list.get(i).getCidade().isEmpty()) {
                                list.get(i).setDescricao_endereco(list.get(i).getDescricao_endereco().replace("'", " "));
                                List<Endereco> listEndereco = new ArrayList();
                                if (!list.get(i).getLogradouro().isEmpty() && !list.get(i).getDescricao_endereco().isEmpty() && !list.get(i).getBairro().isEmpty() && !list.get(i).getCidade().isEmpty()) {
                                    listEndereco = enderecoDao.pesquisaEnderecoImportacao(list.get(i).getUf(), list.get(i).getCidade(), list.get(i).getLogradouro(), list.get(i).getDescricao_endereco(), list.get(i).getCep(), "T", true);
                                    if (listEndereco.isEmpty()) {
                                        listEndereco = enderecoDao.pesquisaEnderecoImportacao(list.get(i).getUf(), list.get(i).getCidade(), list.get(i).getLogradouro(), list.get(i).getDescricao_endereco(), list.get(i).getCep(), "T", false);
                                    }
                                    endereco = new Endereco();
                                }
                                if (list.get(i).getNumero().isEmpty()) {
                                    saveEndereco = false;
                                    listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "NÚMERO NÃO INFORMADO!"));
                                }
                                Logradouro logradouro = null;
                                if (!listEndereco.isEmpty()) {
                                    endereco = listEndereco.get(0);
//                                    for (int z = 0; z < listEndereco.size(); z++) {
//                                        if (listEndereco.get(z).getCep().endsWith(list.get(i).getCep())) {
//                                            endereco = listEndereco.get(z);
//                                            list.get(i).setCep(endereco.getCep());
//                                            break;
//                                        }
//                                    }
//                                    if (endereco.getId() == -1) {
//                                        endereco = new Endereco();
//                                        endereco = listEndereco.get(0);
//                                        if (endereco != null && endereco.getId() != -1) {
//                                            if (endereco.getCep().isEmpty()) {
//                                                list.get(i).setCep(endereco.getCep());
//                                            }
//                                        }
//                                    }
                                } else {
//                                    if (list.get(i).getCep().isEmpty()) {
//                                        saveEndereco = false;
//                                        listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CEP NÃO INFORMADO!"));
//                                    } else {
//                                        CEPService cEPService = new CEPService();
//                                        cEPService.setCep(list.get(i).getCep());
//                                        cEPService.procurar(dao);
//                                        if (cEPService.getEndereco() == null) {
//                                        } else {
//                                            endereco = cEPService.getEndereco();
//                                        }
//                                    }

                                    endereco = new Endereco();
                                    if (saveEndereco) {
                                        if (endereco.getId() == -1) {
                                            if (list.get(i).getLogradouro().isEmpty()) {
                                                saveEndereco = false;
                                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "LOGRADOURO NÃO INFORMADO!"));
                                            } else {
                                                logradouro = new LogradouroDao().find(list.get(i).getLogradouro());
                                                if (logradouro == null) {
                                                    saveEndereco = false;
                                                    listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "LOGRADOURO NÃO EXISTE NO SISTEMA! " + list.get(i).getLogradouro()));
                                                    // logradouro = (Logradouro) dao.find(new Logradouro(), 0);
                                                }
                                            }
                                            if (list.get(i).getDescricao_endereco().isEmpty()) {
                                                descricaoEndereco = (DescricaoEndereco) dao.find(new DescricaoEndereco(), 0);
                                            } else {
                                                descricaoEndereco = new DescricaoEnderecoDao().find(list.get(i).getDescricao_endereco());
                                                if (descricaoEndereco == null) {
                                                    descricaoEndereco = new DescricaoEndereco();
                                                    descricaoEndereco.setAtivo(false);
                                                    descricaoEndereco.setDescricao(list.get(i).getDescricao_endereco().trim());
                                                    if (!dao.save(descricaoEndereco)) {
                                                        dao.rollback();
                                                        return;
                                                    }
                                                }
                                            }
                                            if (list.get(i).getBairro().isEmpty()) {
                                                bairro = (Bairro) dao.find(new Bairro(), 0);
                                            } else {
                                                bairro = new BairroDao().find(list.get(i).getBairro());
                                                if (bairro == null) {
                                                    bairro = new Bairro();
                                                    bairro.setAtivo(false);
                                                    bairro.setDescricao(list.get(i).getBairro().trim());
                                                    if (!dao.save(bairro)) {
                                                        dao.rollback();
                                                        return;
                                                    }
                                                }
                                            }
                                            if (list.get(i).getCidade().isEmpty()) {
                                                saveEndereco = false;
                                                listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CIDADE NÃO INFORMADA!"));
                                                cidade = (Cidade) dao.find(new Cidade(), 0);
                                            } else {
                                                cidade = new CidadeDao().find(list.get(i).getCidade(), list.get(i).getUf());
                                                if (cidade == null) {
                                                    saveEndereco = false;
                                                    // cidade = (Cidade) dao.find(new Cidade(), 0);
                                                    listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CIDADE NÃO EXISTE NO SISTEMA! " + list.get(i).getCidade()));
                                                }
                                            }
                                        }
                                    }

                                }

                                if ((endereco != null && endereco.getId() == -1) || endereco == null && saveEndereco) {
                                    if (list.get(i).getCep().isEmpty()) {
                                        saveEndereco = false;
                                        listCritica.add(new Critica(r, list.get(i).getDocumento(), list.get(i).getNome(), "CEP NÃO ENCONTRADO!"));
                                    }
                                    endereco = new Endereco();
                                    endereco.setCep(list.get(i).getCep());
                                    endereco.setAtivo(false);
                                    endereco.setLogradouro(logradouro);
                                    endereco.setBairro(bairro);
                                    endereco.setCidade(cidade);
                                    endereco.setDescricaoEndereco(descricaoEndereco);
                                    if (endereco.getId() == -1 && saveEndereco) {
                                        if (!dao.save(endereco)) {
                                            dao.rollback();
                                            return;
                                        }
                                    }
                                }
                                if (endereco.getId() != -1) {
                                    list.get(i).setEndereco(endereco);
                                }
                            }

                        }
                        if (list.get(i).getPorte_descricao() != null && list.get(i).getPorte() == null) {
                            if (list.get(i).getPorte_descricao().isEmpty()) {
                                list.get(i).revisePorte();
                                switch (list.get(i).getPorte_descricao()) {
                                    case "ME":
                                        list.get(i).setPorte((Porte) dao.find(new Porte(), 1));
                                        break;
                                    case "EPP":
                                        list.get(i).setPorte((Porte) dao.find(new Porte(), 2));
                                        break;
                                    default:
                                        list.get(i).setPorte((Porte) dao.find(new Porte(), 3));
                                        break;
                                }
                            } else {
                                switch (list.get(i).getPorte_descricao()) {
                                    case "ME":
                                        list.get(i).setPorte((Porte) dao.find(new Porte(), 1));
                                        break;
                                    case "EPP":
                                        list.get(i).setPorte((Porte) dao.find(new Porte(), 2));
                                        break;
                                    default:
                                        list.get(i).setPorte((Porte) dao.find(new Porte(), 3));
                                        break;
                                }
                            }
                        } else if (list.get(i).getPorte() == null) {
                            list.get(i).setPorte((Porte) dao.find(new Porte(), 3));
                        }
                        if (!d.isEmpty()) {
                            list.get(i).setDocumento(d);
                        }
                        if (!dao.save(list.get(i))) {
                            dao.rollback();
                            return;
                        }
                        dao.commit();
                    }
                } catch (Exception e2) {
                    e2.getMessage();
                    dao.rollback();
                }
                if (!run) {
                    break;
                }
            }
            for (int i = 0; i < listCritica.size(); i++) {
                new Dao().save(listCritica.get(i), true);
            }
            sisProcesso.finish();
            // Saída: id: 123; first name: Wektabyte
        } catch (Exception e) {
            e.getMessage();
        }
        run = false;

    }

    public String getPath() {
        return "resources/cliente/" + GenericaSessao.getString("sessaoCliente").toLowerCase() + "/arquivos/importacao/fisica/importacao.json";
    }

    public FisicaImportacao getFisicaImportacao() {
        return fisicaImportacao;
    }

    public void setFisicaImportacao(FisicaImportacao fisicaImportacao) {
        this.fisicaImportacao = fisicaImportacao;
    }

    public List<FisicaImportacao> getListFisicaImportacao() {
        return listFisicaImportacao;
    }

    public void setListFisicaImportacao(List<FisicaImportacao> listFisicaImportacao) {
        this.listFisicaImportacao = listFisicaImportacao;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getRun() {
        return run;
    }

    public void setRun(Boolean run) {
        this.run = run;
    }

    public Integer getTotal() {
        if (total == null || total == 0) {
            String json = "";
            File file = new File(((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/resources/cliente/" + GenericaSessao.getString("sessaoCliente").toLowerCase() + "/arquivos/importacao/" + type + "/importacao.json"));
            Gson gson = new Gson();
            if (file.exists()) {
                try {
                    json = FileUtils.readFileToString(file);
                    // json = json.replace("[", "{");
                    // json = json.replace("]", "}");
                    // json = gson.toJson(json);
                } catch (IOException ex) {
                    Logger.getLogger(ImportacaoBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            List<FisicaImportacao> list = new ArrayList();
            if (type.equals("fisica")) {
                list = gson.fromJson(json, new TypeToken<List<FisicaImportacao>>() {
                }.getType());
            } else if (type.equals("juridica")) {
                list = gson.fromJson(json, new TypeToken<List<JuridicaImportacao>>() {
                }.getType());
            } else if (type.equals("contabilidade")) {
                list = gson.fromJson(json, new TypeToken<List<JuridicaImportacao>>() {
                }.getType());
            }
            total = list.size();
        }
        return total;
    }

    public void corrigirEnderecos() {
        if (type.equals("fisica")) {
            EnderecoDao enderecoDao = new EnderecoDao();
            Dao dao = new Dao();
            DescricaoEndereco descricaoEndereco = null;
            Bairro bairro = null;
            Cidade cidade = null;
            listFisicaImportacao = new ArrayList();
            listFisicaImportacao = dao.list(new FisicaImportacao());
            for (int i = 0; i < listFisicaImportacao.size(); i++) {
                dao.openTransaction();
                Boolean saveEndereco = true;
                if (saveEndereco) {
                    Endereco endereco = new Endereco();
                    if (!listFisicaImportacao.get(i).getLogradouro().isEmpty() || !listFisicaImportacao.get(i).getDescricao_endereco().isEmpty() || !listFisicaImportacao.get(i).getBairro().isEmpty() || !listFisicaImportacao.get(i).getCidade().isEmpty() && !listFisicaImportacao.get(i).getCep().isEmpty()) {
                        listFisicaImportacao.get(i).setDescricao_endereco(listFisicaImportacao.get(i).getDescricao_endereco().replace("'", " "));
                        List<Endereco> listEndereco = new ArrayList();
                        if (!listFisicaImportacao.get(i).getLogradouro().isEmpty() && !listFisicaImportacao.get(i).getDescricao_endereco().isEmpty() && !listFisicaImportacao.get(i).getBairro().isEmpty() && !listFisicaImportacao.get(i).getCidade().isEmpty()) {
                            listEndereco = enderecoDao.pesquisaEnderecoImportacao(listFisicaImportacao.get(i).getUf(), listFisicaImportacao.get(i).getCidade(), listFisicaImportacao.get(i).getLogradouro(), listFisicaImportacao.get(i).getDescricao_endereco(), listFisicaImportacao.get(i).getCep(), "T", true);
                            if (listEndereco.isEmpty()) {
                                listEndereco = enderecoDao.pesquisaEnderecoImportacao(listFisicaImportacao.get(i).getUf(), listFisicaImportacao.get(i).getCidade(), listFisicaImportacao.get(i).getLogradouro(), listFisicaImportacao.get(i).getDescricao_endereco(), listFisicaImportacao.get(i).getCep(), "T", false);
                            }
                            endereco = new Endereco();
                        }
                        Logradouro logradouro = null;
                        if (!listEndereco.isEmpty()) {
                            endereco = listEndereco.get(0);
//                            for (int z = 0; z < listEndereco.size(); z++) {
//                                if (listFisicaImportacaoEndereco.get(z).getCep().endsWith(listFisicaImportacao.get(i).getCep())) {
//                                    endereco = listEndereco.get(z);
//                                    listFisicaImportacao.get(i).setCep(endereco.getCep());
//                                    break;
//                                }
//                            }
//                            if (endereco.getId() == -1) {
//                                endereco = new Endereco();
//                                endereco = listEndereco.get(0);
//                                if (endereco != null && endereco.getId() != -1) {
//                                    if (endereco.getCep().isEmpty()) {
//                                        listFisicaImportacao.get(i).setCep(endereco.getCep());
//                                    }
//                                }
//                            }
                        } else {
                            if (listFisicaImportacao.get(i).getCep().isEmpty()) {
                                saveEndereco = false;
                            } else {
//                                CEPService cEPService = new CEPService();
//                                cEPService.setCep(listFisicaImportacao.get(i).getCep());
//                                cEPService.procurar(dao);
//                                if (cEPService.getEndereco() == null) {
//                                    endereco = new Endereco();
//                                } else {
//                                    endereco = cEPService.getEndereco();
//                                }
                            }

                            if (saveEndereco) {
                                if (endereco.getId() == -1) {
                                    if (listFisicaImportacao.get(i).getLogradouro().isEmpty()) {
                                        saveEndereco = false;

                                    } else {
                                        logradouro = new LogradouroDao().find(listFisicaImportacao.get(i).getLogradouro());
                                        if (logradouro == null) {
                                            saveEndereco = false;
                                            // logradouro = (Logradouro) dao.find(new Logradouro(), 0);
                                        }
                                    }
                                    if (listFisicaImportacao.get(i).getDescricao_endereco().isEmpty()) {
                                        descricaoEndereco = (DescricaoEndereco) dao.find(new DescricaoEndereco(), 0);
                                    } else {
                                        descricaoEndereco = new DescricaoEnderecoDao().find(listFisicaImportacao.get(i).getDescricao_endereco());
                                        if (descricaoEndereco == null) {
                                            descricaoEndereco = new DescricaoEndereco();
                                            descricaoEndereco.setAtivo(false);
                                            descricaoEndereco.setDescricao(listFisicaImportacao.get(i).getDescricao_endereco().trim());
                                            if (!dao.save(descricaoEndereco)) {
                                                dao.rollback();
                                                return;
                                            }
                                        }
                                    }
                                    if (listFisicaImportacao.get(i).getBairro().isEmpty()) {
                                        bairro = (Bairro) dao.find(new Bairro(), 0);
                                    } else {
                                        bairro = new BairroDao().find(listFisicaImportacao.get(i).getBairro());
                                        if (bairro == null) {
                                            bairro = new Bairro();
                                            bairro.setAtivo(false);
                                            bairro.setDescricao(listFisicaImportacao.get(i).getBairro().trim());
                                            if (!dao.save(bairro)) {
                                                dao.rollback();
                                                return;
                                            }
                                        }
                                    }
                                    if (listFisicaImportacao.get(i).getCidade().isEmpty()) {
                                        saveEndereco = false;
                                        cidade = (Cidade) dao.find(new Cidade(), 0);
                                    } else {
                                        cidade = new CidadeDao().find(listFisicaImportacao.get(i).getCidade(), listFisicaImportacao.get(i).getUf());
                                        if (cidade == null) {
                                            saveEndereco = false;
                                            // cidade = (Cidade) dao.find(new Cidade(), 0);

                                        }
                                    }
                                }
                            }

                        }

                        if ((endereco != null && endereco.getId() == -1) || endereco == null && saveEndereco) {
                            if (listFisicaImportacao.get(i).getCep().isEmpty()) {
                                saveEndereco = false;

                            }
                            endereco = new Endereco();
                            endereco.setCep(listFisicaImportacao.get(i).getCep());
                            endereco.setAtivo(false);
                            endereco.setLogradouro(logradouro);
                            endereco.setBairro(bairro);
                            endereco.setCidade(cidade);
                            endereco.setDescricaoEndereco(descricaoEndereco);
                            if (endereco.getId() == -1 && saveEndereco) {
                                if (!dao.save(endereco)) {
                                    dao.rollback();
                                    return;
                                }
                            }
                        }
                        if (endereco.getId() != -1) {
                            listFisicaImportacao.get(i).setEndereco(endereco);
                        }
                    }
                }
                if (!dao.update(listFisicaImportacao.get(i))) {
                    dao.rollback();
                    return;
                }
                dao.commit();
            }

        } else {
            EnderecoDao enderecoDao = new EnderecoDao();
            Dao dao = new Dao();
            DescricaoEndereco descricaoEndereco = null;
            Bairro bairro = null;
            Cidade cidade = null;
            JuridicaImportacaoDao juridicaImportacaoDao = new JuridicaImportacaoDao();
            listJuridicaImportacao = juridicaImportacaoDao.findAll();
            for (int i = 0; i < listJuridicaImportacao.size(); i++) {
                try {
                    dao.openTransaction();
                    Boolean saveEndereco = true;
                    if (saveEndereco) {
                        Endereco endereco = new Endereco();
                        if (!listJuridicaImportacao.get(i).getLogradouro().isEmpty() || !listJuridicaImportacao.get(i).getDescricao_endereco().isEmpty() || !listJuridicaImportacao.get(i).getBairro().isEmpty() || !listJuridicaImportacao.get(i).getCidade().isEmpty() && !listJuridicaImportacao.get(i).getCep().isEmpty()) {
                            listJuridicaImportacao.get(i).setDescricao_endereco(listJuridicaImportacao.get(i).getDescricao_endereco().replace("'", " "));
                            List<Endereco> listEndereco = new ArrayList();
                            if (!listJuridicaImportacao.get(i).getLogradouro().isEmpty() && !listJuridicaImportacao.get(i).getDescricao_endereco().isEmpty() && !listJuridicaImportacao.get(i).getBairro().isEmpty() && !listJuridicaImportacao.get(i).getCidade().isEmpty()) {
                                listEndereco = enderecoDao.pesquisaEnderecoImportacao(listJuridicaImportacao.get(i).getUf(), listJuridicaImportacao.get(i).getCidade(), listJuridicaImportacao.get(i).getLogradouro(), listJuridicaImportacao.get(i).getDescricao_endereco(), listJuridicaImportacao.get(i).getCep(), "T", true);
                                if (listEndereco.isEmpty()) {
                                    listEndereco = enderecoDao.pesquisaEnderecoImportacao(listJuridicaImportacao.get(i).getUf(), listJuridicaImportacao.get(i).getCidade(), listJuridicaImportacao.get(i).getLogradouro(), listJuridicaImportacao.get(i).getDescricao_endereco(), listJuridicaImportacao.get(i).getCep(), "T", false);
                                }
                                endereco = new Endereco();
                            }
                            Logradouro logradouro = null;
                            if (!listEndereco.isEmpty()) {
                                endereco = listEndereco.get(0);
//                            for (int z = 0; z < listEndereco.size(); z++) {
//                                if (listJuridicaImportacaoEndereco.get(z).getCep().endsWith(listJuridicaImportacao.get(i).getCep())) {
//                                    endereco = listEndereco.get(z);
//                                    listJuridicaImportacao.get(i).setCep(endereco.getCep());
//                                    break;
//                                }
//                            }
//                            if (endereco.getId() == -1) {
//                                endereco = new Endereco();
//                                endereco = listEndereco.get(0);
//                                if (endereco != null && endereco.getId() != -1) {
//                                    if (endereco.getCep().isEmpty()) {
//                                        listJuridicaImportacao.get(i).setCep(endereco.getCep());
//                                    }
//                                }
//                            }
                            } else {
                                if (listJuridicaImportacao.get(i).getCep().isEmpty()) {
                                    saveEndereco = false;
                                } else {
//                                CEPService cEPService = new CEPService();
//                                cEPService.setCep(listJuridicaImportacao.get(i).getCep());
//                                cEPService.procurar(dao);
//                                if (cEPService.getEndereco() == null) {
//                                    endereco = new Endereco();
//                                } else {
//                                    endereco = cEPService.getEndereco();
//                                }
                                }

                                if (saveEndereco) {
                                    if (endereco.getId() == -1) {
                                        if (listJuridicaImportacao.get(i).getLogradouro().isEmpty()) {
                                            saveEndereco = false;

                                        } else {
                                            logradouro = new LogradouroDao().find(listJuridicaImportacao.get(i).getLogradouro());
                                            if (logradouro == null) {
                                                saveEndereco = false;
                                                // logradouro = (Logradouro) dao.find(new Logradouro(), 0);
                                            }
                                        }
                                        if (listJuridicaImportacao.get(i).getDescricao_endereco().isEmpty()) {
                                            descricaoEndereco = (DescricaoEndereco) dao.find(new DescricaoEndereco(), 0);
                                        } else {
                                            descricaoEndereco = new DescricaoEnderecoDao().find(listJuridicaImportacao.get(i).getDescricao_endereco());
                                            if (descricaoEndereco == null) {
                                                descricaoEndereco = new DescricaoEndereco();
                                                descricaoEndereco.setAtivo(false);
                                                descricaoEndereco.setDescricao(listJuridicaImportacao.get(i).getDescricao_endereco().trim());
                                                if (!dao.save(descricaoEndereco)) {
                                                    dao.rollback();
                                                    return;
                                                }
                                            }
                                        }
                                        if (listJuridicaImportacao.get(i).getBairro().isEmpty()) {
                                            bairro = (Bairro) dao.find(new Bairro(), 0);
                                        } else {
                                            bairro = new BairroDao().find(listJuridicaImportacao.get(i).getBairro());
                                            if (bairro == null) {
                                                bairro = new Bairro();
                                                bairro.setAtivo(false);
                                                bairro.setDescricao(listJuridicaImportacao.get(i).getBairro().trim());
                                                if (!dao.save(bairro)) {
                                                    dao.rollback();
                                                    return;
                                                }
                                            }
                                        }
                                        if (listJuridicaImportacao.get(i).getCidade().isEmpty()) {
                                            saveEndereco = false;
                                            cidade = (Cidade) dao.find(new Cidade(), 0);
                                        } else {
                                            cidade = new CidadeDao().find(listJuridicaImportacao.get(i).getCidade(), listJuridicaImportacao.get(i).getUf());
                                            if (cidade == null) {
                                                saveEndereco = false;
                                                // cidade = (Cidade) dao.find(new Cidade(), 0);

                                            }
                                        }
                                    }
                                }

                            }

                            if ((endereco != null && endereco.getId() == -1) || endereco == null && saveEndereco) {
                                if (listJuridicaImportacao.get(i).getCep().isEmpty()) {
                                    saveEndereco = false;

                                }
                                endereco = new Endereco();
                                endereco.setCep(listJuridicaImportacao.get(i).getCep());
                                endereco.setAtivo(false);
                                endereco.setLogradouro(logradouro);
                                endereco.setBairro(bairro);
                                endereco.setCidade(cidade);
                                endereco.setDescricaoEndereco(descricaoEndereco);
                                if (endereco.getId() == -1 && saveEndereco) {
                                    if (!dao.save(endereco)) {
                                        dao.rollback();
                                        return;
                                    }
                                }
                            }
                            if (endereco.getId() != -1) {
                                listJuridicaImportacao.get(i).setEndereco(endereco);
                            }
                        }
                    }
                    if (!dao.update(listJuridicaImportacao.get(i))) {
                        dao.rollback();
                        return;
                    }
                    dao.commit();
                } catch (Exception e) {
                    dao.rollback();
                }
            }

        }
    }

    public Integer getProccessed() {
        if (type.equals("fisica")) {
            return new FisicaImportacaoDao().count();
        } else if (type.equals("juridica")) {
            return new JuridicaImportacaoDao().countEmpresas();
        } else if (type.equals("contabilidade")) {
            return new JuridicaImportacaoDao().countContabilidade();
        }
        return 0;
    }

    public Integer getPercent() {
        try {
            int proccessed = getProccessed() * 100;
            return proccessed / getTotal();
        } catch (Exception e) {
            return 0;
        }
    }

    public JuridicaImportacao getJuridicaImportacao() {
        return juridicaImportacao;
    }

    public void setJuridicaImportacao(JuridicaImportacao juridicaImportacao) {
        this.juridicaImportacao = juridicaImportacao;
    }

    public List<JuridicaImportacao> getListJuridicaImportacao() {
        return listJuridicaImportacao;
    }

    public void setListJuridicaImportacao(List<JuridicaImportacao> listJuridicaImportacao) {
        this.listJuridicaImportacao = listJuridicaImportacao;
    }

    public Boolean getCancel() {
        return cancel;
    }

    public void setCancel(Boolean cancel) {
        this.cancel = cancel;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
