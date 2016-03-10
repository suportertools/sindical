package br.com.rtools.arrecadacao.beans;

import br.com.rtools.arrecadacao.ConfiguracaoArrecadacao;
import br.com.rtools.pessoa.Filial;
import br.com.rtools.utilitarios.Dao;
import br.com.rtools.utilitarios.GenericaMensagem;
import br.com.rtools.utilitarios.GenericaSessao;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

@ManagedBean
@ViewScoped
public class ConfiguracaoArrecadacaoBean implements Serializable {

    private ConfiguracaoArrecadacao configuracaoArrecadacao;
    private List<SelectItem> listaFilial;
    private Integer idFilial;

    @PostConstruct
    public void init() {
        Dao dao = new Dao();
        configuracaoArrecadacao = (ConfiguracaoArrecadacao) dao.find(new ConfiguracaoArrecadacao(), 1);
        if (configuracaoArrecadacao == null) {
            configuracaoArrecadacao = new ConfiguracaoArrecadacao();
            configuracaoArrecadacao.setId(1);
            configuracaoArrecadacao.setFilial(null);
            dao.save(configuracaoArrecadacao, true);
        }
        listaFilial = new ArrayList();
        getListaFilial();
    }

    @PreDestroy
    public void destroy() {
        GenericaSessao.remove("configuracaoArrecadacaoBean");
    }

    public void load() {

    }

    public void update() {
        Dao dao = new Dao();
        if (configuracaoArrecadacao.getId() != -1) {
            configuracaoArrecadacao.setFilial((Filial) dao.find(new Filial(), Integer.valueOf(listaFilial.get(idFilial).getDescription())));
            if (dao.update(configuracaoArrecadacao, true)) {
                GenericaMensagem.info("Sucesso", "Configurações Aplicadas");
            } else {
                GenericaMensagem.warn("Erro", "Ao atualizar este registro!");
            }
        }
    }

    public ConfiguracaoArrecadacao getConfiguracaoArrecadacao() {
        return configuracaoArrecadacao;
    }

    public void setConfiguracaoArrecadacao(ConfiguracaoArrecadacao configuracaoArrecadacao) {
        this.configuracaoArrecadacao = configuracaoArrecadacao;
    }

    public List<SelectItem> getListaFilial() {
        if (listaFilial.isEmpty()) {
            List<Filial> list = new Dao().list(new Filial(), true);
            for (int i = 0; i < list.size(); i++) {
                if (Objects.equals(list.get(i).getId(), configuracaoArrecadacao.getFilial().getId())) {
                    setIdFilial((Integer) i);
                }
                listaFilial.add(new SelectItem(i, list.get(i).getFilial().getPessoa().getNome(), "" + list.get(i).getId()));
            }
        }
        return listaFilial;
    }

    public void setListaFilial(List<SelectItem> listaFilial) {
        this.listaFilial = listaFilial;
    }

    public Integer getIdFilial() {
        return idFilial;
    }

    public void setIdFilial(Integer idFilial) {
        this.idFilial = idFilial;
    }

    public static ConfiguracaoArrecadacao get() {
        return (ConfiguracaoArrecadacao) new Dao().find(new ConfiguracaoArrecadacao(), 1);
    }
}
