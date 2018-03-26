package br.com.rtools.webservice;

import br.com.rtools.sistema.ControleScripts;
import br.com.rtools.sistema.TipoControleScripts;
import br.com.rtools.utilitarios.Dao;
import br.com.rtools.utilitarios.GenericaRequisicao;
import br.com.rtools.utilitarios.GenericaSessao;
import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@RequestScoped
@ViewScoped
public class WSControleScripts implements Serializable {

    public WSControleScripts() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        GenericaSessao.put("sessaoCliente", "Rtools");
    }

    public void storeDataBaseServer() {
        try {
            String database_server = GenericaRequisicao.getParametro("database_server");
            // String device = GenericaRequisicao.getParametro("device");
            // String type = GenericaRequisicao.getParametro("type");
            // String mac = GenericaRequisicao.getParametro("mac");
            ControleScripts controleScripts = new ControleScripts();
            controleScripts.setControleScripts((TipoControleScripts) new Dao().find(new TipoControleScripts(), 1));
            controleScripts.setDescricao(database_server);
            controleScripts.setMac("");
            new Dao().save(controleScripts, true);
        } catch (Exception e) {

        }
    }

}
