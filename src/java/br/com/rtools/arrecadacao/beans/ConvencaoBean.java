package br.com.rtools.arrecadacao.beans;

import br.com.rtools.arrecadacao.dao.ConvencaoDao;
import br.com.rtools.arrecadacao.dao.ConvencaoCidadeDao;
import br.com.rtools.arrecadacao.Convencao;
import br.com.rtools.arrecadacao.ConvencaoCidade;
import br.com.rtools.arrecadacao.GrupoCidade;
import br.com.rtools.logSistema.NovoLog;
import br.com.rtools.seguranca.controleUsuario.ControleUsuarioBean;
import br.com.rtools.utilitarios.Dao;
import br.com.rtools.utilitarios.DataObject;
import br.com.rtools.utilitarios.GenericaMensagem;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import org.primefaces.model.UploadedFile;

@ManagedBean
@SessionScoped
public class ConvencaoBean implements Serializable {

    private Convencao convencao = new Convencao();
    private ConvencaoCidade conCidade = new ConvencaoCidade();
    private String comoPesquisa = "P";
    private String descPesquisa = "";
    private int idGrupoCidade = 0;
    private List<DataObject> listaGpCidade = new ArrayList();
    private DataObject dolinha;
    private int idIndex = -1;
    private List<Convencao> listaConvencao = new ArrayList();
    private UploadedFile file;

    public void set(DataObject linha) {
        dolinha = linha;
    }

    public void upload() {
        if (file != null) {
            if (dolinha == null) {
                return;
            }
            //    FacesContext context = FacesContext.getCurrentInstance();
            //String caminho = ((ServletContext) context.getExternalContext().getContext()).getRealPath("/Cliente/" + ControleUsuarioBean.getCliente() + "/Arquivos/convencao/arqTemp.pdf");
            //File fl = new File(caminho);
            ConvencaoCidadeDao ccdbt = new ConvencaoCidadeDao();
            GrupoCidade gc = (GrupoCidade) dolinha.getArgumento0();

            ConvencaoCidade cc = ccdbt.pesquisarConvencao(convencao.getId(), gc.getId());
            dolinha.setArgumento1(String.valueOf(gc.getId()) + "_" + convencao.getId());
            cc.setCaminho(String.valueOf(gc.getId()) + "_" + convencao.getId());

            Dao dao = new Dao();

            dao.openTransaction();
            if (!dao.update(cc)) {
                dao.rollback();
                GenericaMensagem.warn("Erro", "Erro ao alterar Convenção Cidade!");
                return;
            }
            dao.commit();

            String destino = ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/Cliente/" + ControleUsuarioBean.getCliente() + "/Arquivos/convencao/" + String.valueOf(gc.getId()) + "_" + convencao.getId() + ".pdf");
            File fl_destino = new File(destino);
            try {

                InputStream in = new BufferedInputStream(file.getInputstream());

                //O método file.getAbsolutePath() fornece o caminho do arquivo criado
                //Pode ser usado para ligar algum objeto do banco ao arquivo enviado
                FileOutputStream fout = new FileOutputStream(fl_destino);

                while (in.available() != 0) {

                    fout.write(in.read());

                }

                fout.close();

//                InputStream in = file.getInputstream();
//                FileOutputStream out = new FileOutputStream(fl_destino.getPath());
//
//                byte[] buf = new byte[(int) file.getSize()];
//                int count;
//                while ((count = in.read()) >= 0) {
//                    out.write(buf, 0, count);
//                }
//                in.close();
//                out.flush();
//                out.close();
            } catch (Exception e) {

            }

            GenericaMensagem.info("Sucesso", "Envio de " + file.getFileName() + " concluído!");
            listaGpCidade.clear();
            dolinha = null;
        }
    }

    public String salvar() {
        ConvencaoDao db = new ConvencaoDao();
        Dao dao = new Dao();

        if (convencao.getId() == -1 && db.pesquisaConvencaoDesc(convencao.getDescricao()) != null) {
            GenericaMensagem.warn("Erro", "Esta convenção já existe no Sistema");
            return null;
        }
        if (convencao.getDescricao().equals("")) {
            GenericaMensagem.warn("Erro", "Digite uma Convenção por favor!");
            return null;
        }

        NovoLog novoLog = new NovoLog();
        dao.openTransaction();
        if (convencao.getId() == -1) {

            if (!dao.save(convencao)) {
                dao.rollback();
                GenericaMensagem.warn("Erro", "Erro ao salvar Convenção!");
                return null;
            }

            GenericaMensagem.info("Sucesso", "Convenção salvo com Sucesso!");
            novoLog.save(
                    "ID: " + convencao.getId()
                    + " - Descrição: " + convencao.getDescricao()
            );
        } else {
            Convencao c = (Convencao) dao.find(new Convencao(), convencao.getId());
            String beforeUpdate
                    = "ID: " + c.getId()
                    + " - Descrição: " + c.getDescricao();
            if (!dao.update(convencao)) {
                dao.rollback();
                GenericaMensagem.warn("Erro", "Erro ao atualizar convenção!");
                return null;
            }
            novoLog.update(beforeUpdate,
                    "ID: " + convencao.getId()
                    + " - Descrição: " + convencao.getDescricao()
            );
            GenericaMensagem.info("Sucesso", "Convenção atualizado com Sucesso!");
        }
        listaGpCidade = new ArrayList();
        dao.commit();
        return null;
    }

    public void adicionar() {
        if (convencao.getDescricao().isEmpty()) {
            GenericaMensagem.warn("Erro", "Digite uma Convenção!");
            return;
        }

        ConvencaoCidadeDao dbCC = new ConvencaoCidadeDao();
        Dao dao = new Dao();
        GrupoCidade gpCid = (GrupoCidade) dao.find(new GrupoCidade(), Integer.valueOf(getListaGrupoCidade().get(idGrupoCidade).getDescription()));

        List listaCidades = dbCC.ListaCidadesConvencao(convencao.getId(), gpCid.getId());
        if (!listaCidades.isEmpty()) {
            GenericaMensagem.warn("Erro", "Grupo com cidade já existente!");
            return;
        }

        dao.openTransaction();

        conCidade.setConvencao(convencao);
        conCidade.setGrupoCidade(gpCid);

        if (convencao.getId() == -1) {
            if (!dao.save(convencao)) {
                dao.rollback();
                GenericaMensagem.warn("Erro", "Erro ao salvar Convenção!");
                return;
            }
        }

        if (!dao.save(conCidade)) {
            dao.rollback();
            GenericaMensagem.warn("Erro", "Erro ao salvar Convenção Cidade!");
            return;
        }

        conCidade = new ConvencaoCidade();
        listaGpCidade.clear();

        GenericaMensagem.info("Sucesso", "Grupo Cidade adicionado!");
        dao.commit();
//        List li = dbCC.pesquisarGruposPorConvencao(convencao.getId());
//        listaGpCidade.clear();
//        for (int i = 0; i < li.size(); i++) {
//            listaGpCidade.add(new DataObject(li.get(i), ""));
//        }
    }

    public void btnExcluir(DataObject linha) {
        ConvencaoCidadeDao db = new ConvencaoCidadeDao();
        ConvencaoCidade cc = db.pesquisarConvencao(convencao.getId(), ((GrupoCidade) linha.getArgumento0()).getId());

        if (cc == null) {
            return;
        }

        if (cc.getCaminho() != null || !cc.getCaminho().isEmpty()) {
            if (!excluirPDF(cc)) {
                return;
            }
        }

        Dao dao = new Dao();

        dao.openTransaction();
        if (!dao.delete(dao.find(new ConvencaoCidade(), cc.getId()))) {
            GenericaMensagem.warn("Erro", "Não foi possível excluir Convenção Cidade");
            dao.rollback();
        }
        listaGpCidade.clear();
        dao.commit();
    }

    public boolean excluirPDF(ConvencaoCidade cc) {
        String caminho = ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/Cliente/" + ControleUsuarioBean.getCliente() + "/Arquivos/convencao/" + cc.getCaminho() + ".pdf");
        File fl = new File(caminho);
        if (!fl.exists()) {
            return true;
        }

        if (!fl.delete()) {
            GenericaMensagem.warn("Erro", "Não foi possível excluir Arquivo PDF!");
            return false;
        }
        return true;
    }

    public void excluirApenasPDF(ConvencaoCidade cc) {
        String caminho = ((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext()).getRealPath("/Cliente/" + ControleUsuarioBean.getCliente() + "/Arquivos/convencao/" + cc.getCaminho() + ".pdf");
        File fl = new File(caminho);
        if (!fl.exists()) {
            return;
        }

        if (!fl.delete()) {
            GenericaMensagem.warn("Erro", "Não foi possível excluir Arquivo PDF!");
        }

        Dao dao = new Dao();
        dao.openTransaction();

        cc.setCaminho("");
        if (!dao.update(cc)) {
            GenericaMensagem.warn("Erro", "Não foi possível alterar Convenção Cidade");
            dao.rollback();
        }
        listaGpCidade.clear();
        dao.commit();
    }

    public List getListaGpCidadeParaConvencao() {
        FacesContext context = FacesContext.getCurrentInstance();
        String caminho = ((ServletContext) context.getExternalContext().getContext()).getRealPath("/Cliente/" + ControleUsuarioBean.getCliente() + "/Arquivos/convencao/arqTemp.pdf");
        File fl = new File(caminho);
        if (convencao.getId() != -1 && !listaGpCidade.isEmpty()) {
            ConvencaoCidadeDao ccdbt = new ConvencaoCidadeDao();
            Dao dao = new Dao();
            try {
                if (fl.exists()) {
                    //GrupoCidade gc = (GrupoCidade) listaGpCidade.get(idIndex).getArgumento0(); AQUI
                    GrupoCidade gc = null;

                    ConvencaoCidade cc = ccdbt.pesquisarConvencao(convencao.getId(), gc.getId());
                    //listaGpCidade.get(idIndex).setArgumento1(String.valueOf(gc.getId()) + "_" + convencao.getId()); AQUI
                    cc.setCaminho(String.valueOf(gc.getId()) + "_" + convencao.getId());
                    dao.openTransaction();
                    if (!dao.update(cc)) {
                        dao.rollback();
                        return listaGpCidade;
                    }

                    File item = new File(caminho);
                    caminho = ((ServletContext) context.getExternalContext().getContext()).getRealPath("/Cliente/" + ControleUsuarioBean.getCliente() + "/Arquivos/convencao/" + String.valueOf(gc.getId()) + "_" + convencao.getId() + ".pdf");
                    fl = new File(caminho);
                    FileInputStream in = new FileInputStream(item);
                    FileOutputStream out = new FileOutputStream(fl.getPath());

                    byte[] buf = new byte[(int) item.length()];
                    int count;
                    while ((count = in.read(buf)) >= 0) {
                        out.write(buf, 0, count);
                    }
                    in.close();
                    out.flush();
                    out.close();

                    caminho = ((ServletContext) context.getExternalContext().getContext()).getRealPath("/Cliente/" + ControleUsuarioBean.getCliente() + "/Arquivos/convencao/arqTemp.pdf");
                    fl = new File(caminho);
                    fl.delete();
                    dao.commit();
                }
            } catch (Exception e) {
                dao.rollback();
                System.out.println(e);
            }
        } else if (fl.exists()) {
            fl.delete();
        }
        return listaGpCidade;
    }

    public List<DataObject> getListaGpCidade() {
        if (listaGpCidade.isEmpty() && convencao.getId() != -1) {
            ConvencaoCidadeDao db = new ConvencaoCidadeDao();
            List<GrupoCidade> result = db.pesquisarGruposPorConvencao(convencao.getId());

            for (int i = 0; i < result.size(); i++) {
                ConvencaoCidade cc = db.pesquisarConvencao(convencao.getId(), result.get(i).getId());
                listaGpCidade.add(new DataObject(result.get(i), cc));
            }
        }
        return listaGpCidade;
    }

    public String novo() {
        convencao = new Convencao();
        listaGpCidade.clear();
        return "convencao";
    }

    public String alterarNome() {
        return "menuPrincipal";
    }

    public String excluir() {
        if (convencao.getId() != -1) {
            Dao dao = new Dao();

            convencao = (Convencao) dao.find(new Convencao(), convencao.getId());

            //ConvencaoCidadeDB db = new ConvencaoCidadeDao();
            //List<GrupoCidade> result = db.pesquisarGruposPorConvencao(convencao.getId());
            dao.openTransaction();
            for (int i = 0; i < listaGpCidade.size(); i++) {
                if (!excluirPDF((ConvencaoCidade) listaGpCidade.get(i).getArgumento1())) {
                    GenericaMensagem.warn("Erro", "Erro ao excluir Arquivos PDF!");
                    return null;
                }

                if (!dao.delete(dao.find(new ConvencaoCidade(), ((ConvencaoCidade) listaGpCidade.get(i).getArgumento1()).getId()))) {
                    GenericaMensagem.warn("Erro", "Convenção Cidade não pode ser excluida!");
                    return null;
                }
            }

            if (!dao.delete(convencao)) {
                GenericaMensagem.warn("Erro", "Convenção não pode ser excluida!");
                dao.rollback();
            }
            NovoLog novoLog = new NovoLog();
            novoLog.delete(
                    "ID: " + convencao.getId()
                    + " - Descrição: " + convencao.getDescricao()
            );
            GenericaMensagem.info("Sucesso", "Convenção excluida com Sucesso!");
            convencao = new Convencao();
            listaGpCidade.clear();
            dao.commit();
        } else {
            GenericaMensagem.warn("Erro", "Pesquise uma Convenção para ser excluida!");
        }
        return null;
    }

    public List<Convencao> getListaConvencao() {
        listaConvencao = new Dao().list(new Convencao(), true);
        return listaConvencao;
    }

    public List<SelectItem> getListaGrupoCidade() {
        List<SelectItem> result = new ArrayList<>();
        int i = 0;
        List select = new Dao().list(new GrupoCidade(), true);
        while (i < select.size()) {
            result.add(new SelectItem(i,
                    ((GrupoCidade) select.get(i)).getDescricao(),
                    Integer.toString(((GrupoCidade) select.get(i)).getId())));
            i++;
        }
        return result;
    }

    public void refreshForm() {
    }

    public String editar(Convencao c) {
//        ConvencaoCidadeDB db = new ConvencaoCidadeDao();
        convencao = c;
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("convencaoPesquisa", convencao);
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("linkClicado", true);
//        List<GrupoCidade> li = db.pesquisarGruposPorConvencao(convencao.getId());
        listaGpCidade.clear();
//        for (int i = 0; i < li.size(); i++) {
//            ConvencaoCidade cc = db.pesquisarConvencao(convencao.getId(), li.get(i).getId());
//            if (cc.getCaminho() == null || cc.getCaminho().isEmpty()) {
//                listaGpCidade.add(new DataObject(li.get(i), ""));
//            } else {
//                listaGpCidade.add(new DataObject(li.get(i), cc.getCaminho()));
//            }
//
//        }
        if (FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("urlRetorno") == null) {
            return "convencao";
        } else {
            return (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("urlRetorno");
        }
    }

    public int getIdGrupoCidade() {
        return idGrupoCidade;
    }

    public void setIdGrupoCidade(int idGrupoCidade) {
        this.idGrupoCidade = idGrupoCidade;
    }

    public String getComoPesquisa() {
        return comoPesquisa;
    }

    public void setComoPesquisa(String comoPesquisa) {
        this.comoPesquisa = comoPesquisa;
    }

    public String getDescPesquisa() {
        return descPesquisa;
    }

    public void setDescPesquisa(String descPesquisa) {
        this.descPesquisa = descPesquisa;
    }

    public Convencao getConvencao() {
        return convencao;
    }

    public void setConvencao(Convencao convencao) {
        this.convencao = convencao;
    }

    public ConvencaoCidade getConCidade() {
        return conCidade;
    }

    public void setConCidade(ConvencaoCidade conCidade) {
        this.conCidade = conCidade;
    }

    public int getIdIndex() {
        return idIndex;
    }

    public void setIdIndex(int idIndex) {
        this.idIndex = idIndex;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }
}
