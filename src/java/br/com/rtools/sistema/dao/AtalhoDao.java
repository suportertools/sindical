package br.com.rtools.sistema.dao;

import br.com.rtools.principal.DB;
import br.com.rtools.sistema.Atalhos;
import br.com.rtools.sistema.ContadorAcessos;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Query;

public class AtalhoDao extends DB {

    public List<Atalhos> listaTodos(int id_pessoa) {
        try {
            Query qry = getEntityManager().createQuery(
                    "select at "
                    + "  from Atalhos at "
                    + " where at.pessoa.id = " + id_pessoa);
            return qry.getResultList();
        } catch (Exception e) {
            return new ArrayList();
        }
    }

    public Atalhos pesquisaPorSigla(String sigla) {
        try {
            Query qry = getEntityManager().createQuery(
                    "select at "
                    + "  from Atalhos at "
                    + " where at.sigla = '" + sigla + "'");
            return (Atalhos) qry.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public Atalhos pesquisaPorRotina(int id_rotina) {
        try {
            Query qry = getEntityManager().createQuery(
                    "select at "
                    + "  from Atalhos at "
                    + " where at.rotina.id = " + id_rotina);
            return (Atalhos) qry.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public List<ContadorAcessos> listaAcessosUsuario(int id_usuario) {
        try {
            Query qry = getEntityManager().createQuery(
                    "select ca from ContadorAcessos ca where ca.usuario.id = " + id_usuario + " order by ca.acessos desc");
            qry.setMaxResults(15);
            return qry.getResultList();
        } catch (Exception e) {
            return new ArrayList();
        }
    }

    public ContadorAcessos pesquisaContadorAcessos(int id_usuario, int id_rotina) {
        try {
            Query qry = getEntityManager().createQuery(
                    "select ca from ContadorAcessos ca where ca.usuario.id = " + id_usuario + " and ca.rotina.id = " + id_rotina);
            return (ContadorAcessos) qry.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public ContadorAcessos pesquisaContadorAcessos(int id_usuario, int id_rotina, int id_modulo) {
        try {
            Query qry = getEntityManager().createQuery(
                    "select ca from ContadorAcessos ca where ca.usuario.id = " + id_usuario + " and ca.rotina.id = " + id_rotina + " and ca.modulo.id = " + id_modulo);
            return (ContadorAcessos) qry.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}
