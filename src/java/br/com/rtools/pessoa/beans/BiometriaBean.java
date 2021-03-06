package br.com.rtools.pessoa.beans;

import br.com.rtools.pessoa.Biometria;
import br.com.rtools.pessoa.BiometriaAtualizaCatraca;
import br.com.rtools.pessoa.BiometriaCaptura;
import br.com.rtools.pessoa.BiometriaErro;
import br.com.rtools.pessoa.BiometriaServidor;
import br.com.rtools.pessoa.Fisica;
import br.com.rtools.pessoa.Pessoa;
import br.com.rtools.pessoa.dao.BiometriaDao;
import br.com.rtools.pessoa.dao.BiometriaErroDao;
import br.com.rtools.seguranca.MacFilial;
import br.com.rtools.seguranca.Registro;
import br.com.rtools.seguranca.Rotina;
import br.com.rtools.sistema.Dispositivo;
import br.com.rtools.sistema.dao.DispositivoDao;
import br.com.rtools.utilitarios.Dao;
import br.com.rtools.utilitarios.GenericaMensagem;
import br.com.rtools.utilitarios.GenericaSessao;
import br.com.rtools.utilitarios.PF;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

@ManagedBean
@ViewScoped
public class BiometriaBean implements Serializable {

    private Biometria biometria = null;
    private Boolean disabled = false;
    private Boolean open_modal_cancel = false;

    public boolean isOpenBiometria() {
        if (GenericaSessao.exists("acessoFilial")) {
            Registro registro = (Registro) new Dao().find(new Registro(), 1);
            return registro.isBiometria();
        }
        return false;
    }

    public boolean isStatus() {
        if (GenericaSessao.exists("acessoFilial")) {
            BiometriaDao biometriaDao = new BiometriaDao();
            List list = biometriaDao.pesquisaStatusPorComputador(MacFilial.getAcessoFilial().getId());
            if (!list.isEmpty()) {
                BiometriaServidor biometriaServidor = (BiometriaServidor) new Dao().rebind((BiometriaServidor) list.get(0));
                return biometriaServidor.getAtivo();
            }
        }
        return false;
    }

    public boolean complete() {
        switch (new Rotina().get().getCurrentPage()) {
            case "pessoaFisica":
                return complete(((FisicaBean) GenericaSessao.getObject("fisicaBean")).getFisica().getPessoa().getId());
        }
        return false;
    }

    public boolean complete(Fisica f) {
        if (f == null) {
            return false;
        }
        return complete(f.getPessoa().getId());
    }

    public boolean complete(Pessoa p) {
        if (p == null) {
            return false;
        }
        return complete(p.getId());
    }

    public boolean complete(Integer pessoa_id) {
        if (pessoa_id == null) {
            return false;
        }
        if (biometria == null) {
            if (GenericaSessao.exists("acessoFilial")) {
                BiometriaDao biometriaDao = new BiometriaDao();
                Dao dao = new Dao();
                biometria = biometriaDao.pesquisaBiometriaPorPessoa(pessoa_id);
                if (biometria != null) {
                    biometria = (Biometria) dao.rebind(biometria);
                    if (biometria.getAtivo() && biometria.getBiometria() != null && !biometria.getBiometria().isEmpty()) {
                        return true;
                    }
                }
            }
        } else if (biometria.getBiometria() != null && !biometria.getBiometria().isEmpty() && biometria.getAtivo()) {
            return true;
        }
        return false;
    }

    public void gravar(Pessoa p) {
        delete(p, false);
        open_modal_cancel = false;
        Dao dao = new Dao();
        if (GenericaSessao.exists("acessoFilial") && !open_modal_cancel) {
            BiometriaDao biometriaDao = new BiometriaDao();
            disabled = true;
            List list = biometriaDao.pesquisaBiometriaCapturaPorPessoa(p.getId());
            if (!list.isEmpty()) {
                for (Object list1 : list) {
                    dao.delete(list1, true);
                }
            }
            BiometriaErroDao biometriaErroDao = new BiometriaErroDao();
            BiometriaCaptura biometriaCaptura = new BiometriaCaptura();
            biometriaCaptura.setMacFilial((MacFilial) GenericaSessao.getObject("acessoFilial"));
            biometriaCaptura.setPessoa(p);
            dao.save(biometriaCaptura, true);
            biometria = null;
            Dispositivo dispositivo = new DispositivoDao().findByMacFilial(biometriaCaptura.getMacFilial().getId(), 2);
            if (dispositivo != null && dispositivo.getSocketPort() != null) {
                Socket socket = null;
                try {
                    if (!dispositivo.getAtivo()) {
                        GenericaMensagem.warn("Sistema", "DISPOSITIVO COM STATUS INÁTIVO!");
                        return;
                    }
                    if (dispositivo.getSocketPort() != null && dispositivo.getSocketPort() != 0 && (dispositivo.getSocketHost() == null && dispositivo.getSocketHost().isEmpty())) {
                        socket = new Socket("localhost", dispositivo.getSocketPort());
                    } else if (dispositivo.getSocketHost() != null && !dispositivo.getSocketHost().isEmpty() && dispositivo.getSocketPort() != null && dispositivo.getSocketPort() != 0) {
                        socket = new Socket(dispositivo.getSocketHost(), dispositivo.getSocketPort());
                    }
                    if (socket != null) {
                        if (socket == null) {
                            GenericaMensagem.warn("Sistema", "DISPOSITIVO DESCONECTADO!");
                            return;
                        }
                        try {
                            if (socket.isClosed()) {
                                GenericaMensagem.warn("Sistema", "DISPOSITIVO DESCONECTADO!");
                                return;
                            }
                        } catch (Exception e) {

                        }
                        // BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        // String answer = br.readLine();
                        // br.close();
                        dispositivo = (Dispositivo) dao.find(dispositivo);
                        if (!dispositivo.getMensagemAlerta().isEmpty()) {
                            GenericaMensagem.warn("Sistema", dispositivo.getMensagemAlerta());
                            dispositivo.setMensagemAlerta("");
                            dao.update(dispositivo, true);
                        }
                    }
                } catch (Exception e) {
                    GenericaMensagem.warn("Sistema", e.getMessage());
                    return;
                }
            }
            for (int i = 0; i < 10; i++) {
//                try {
//                    Socket socket = new Socket("192.168.1.160", 5566);
//                    socket.getInputStream();
//                    socket.close();
//                } catch (UnknownHostException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                if (isStatus()) {
                    BiometriaErro biometriaErro = biometriaErroDao.findByMac(((MacFilial) GenericaSessao.getObject("acessoFilial")).getId());
                    if (biometriaErro != null) {
                        PF.closeDialog("dlg_waiting_biometria");
                        PF.openDialog("dlg_waiting_biometria_erro");
                        open_modal_cancel = true;
                        if (biometriaErro.getNrCodigoErro() == 513) {
                            GenericaMensagem.warn("Erro", "Leitura cancelada pelo usuário no dispositivo!");
                        }
                        new Dao().delete(biometriaErro, true);
                        biometria = null;
                        disabled = false;
                        break;
                    }
                    biometria = (Biometria) biometriaDao.pesquisaBiometriaPorPessoa(p.getId());
                    if (biometria != null) {
                        biometria = (Biometria) dao.rebind(biometria);
                        if (biometria.getBiometria() != null && !biometria.getBiometria().isEmpty()) {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(BiometriaBean.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            disabled = false;
                            BiometriaAtualizaCatraca bac = (BiometriaAtualizaCatraca) new Dao().find(new BiometriaAtualizaCatraca(), 1);
                            if (bac == null) {
                                bac = new BiometriaAtualizaCatraca();
                                dao.save(bac, true);
                            }
                            bac = (BiometriaAtualizaCatraca) dao.rebind(bac);
                            bac.setAparelho1(true);
                            bac.setAparelho2(true);
                            bac.setAparelho3(true);
                            bac.setAparelho4(true);
                            dao.update(bac, true);
                            break;
                        }
                    }
                    try {
                        list = biometriaDao.pesquisaBiometriaCapturaPorPessoa(p.getId());
                        if (list.isEmpty()) {
                            new Dao().delete(biometriaCaptura, true);
                            biometria = null;
                            disabled = false;
                            PF.closeDialog("dlg_waiting_biometria");
                            PF.openDialog("dlg_waiting_biometria_erro");
                            if (open_modal_cancel) {
                                GenericaMensagem.info("Mensagem do Sistema", "Leitura cancelada pelo usuário!");
                            } else {
                                GenericaMensagem.info("Mensagem Dispostivo", "Leitura cancelada pelo usuário!");
                            }
                            open_modal_cancel = true;
                            break;
                        }
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                    }
                    i--;
                } else {
                    new Dao().delete(biometriaCaptura, true);
                    PF.closeDialog("dlg_waiting_biometria");
                    disabled = false;
                    PF.openDialog("dlg_waiting_biometria_info");
                    break;
                }
            }
        }
    }

    public void delete(Pessoa p) {
        delete(p, true);
    }

    public void delete(Pessoa p, Boolean complete) {
        BiometriaAtualizaCatraca bac = (BiometriaAtualizaCatraca) new Dao().find(new BiometriaAtualizaCatraca(), 1);
        Dao dao = new Dao();
        if (complete) {
            if (bac == null) {
                bac = new BiometriaAtualizaCatraca();
                dao.save(bac, true);
            }
        }
        BiometriaDao biometriaDao = new BiometriaDao();
        if (GenericaSessao.exists("acessoFilial")) {
            Biometria b = (Biometria) dao.rebind(biometriaDao.pesquisaBiometriaPorPessoa(p.getId()));
            if (b != null) {
//                b.setAtivo(false);
//                b.setBiometria("");
                if (dao.delete(b, true)) {
                    List list = biometriaDao.listaBiometriaDepartamentoPorPessoa(p.getId());
                    for (Object list1 : list) {
                        dao.delete(list1, true);
                    }
                    if (complete) {
                        bac = (BiometriaAtualizaCatraca) dao.rebind(bac);
                        bac.setAparelho1(true);
                        bac.setAparelho2(true);
                        bac.setAparelho3(true);
                        bac.setAparelho4(true);
                        dao.update(bac, true);
                    }
                    biometria = null;
                    if (complete) {
                        GenericaMensagem.info("Sucesso", "Registro excluído com sucesso");
                    }
                } else if (complete) {
                    GenericaMensagem.warn("Erro", "Ao excluir registro!");
                }
            }
        }
    }

    public Biometria getBiometria() {
        return biometria;
    }

    public void setBiometria(Biometria biometria) {
        this.biometria = biometria;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisabledSave() {
        if (biometria != null) {
            if (biometria.getAtivo()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void abort(Pessoa p) {
        open_modal_cancel = true;
        BiometriaDao dao = new BiometriaDao();
        List list = dao.pesquisaBiometriaCapturaPorMacFilial(((MacFilial) GenericaSessao.getObject("acessoFilial")).getId());
        for (int i = 0; i < list.size(); i++) {
            new Dao().delete(list.get(i), true);
        }
        GenericaMensagem.info("Mensagem do Sistema", "Leitura cancelada pelo usuário!");
        disabled = false;
    }

    public Boolean getOpen_modal_cancel() {
        return open_modal_cancel;
    }

    public void setOpen_modal_cancel(Boolean open_modal_cancel) {
        this.open_modal_cancel = open_modal_cancel;
    }

    public void closeModalApp() {
        this.open_modal_cancel = false;
    }

}
