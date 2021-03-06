package br.com.rtools.associativo.dao;

import br.com.rtools.associativo.AEndereco;
import br.com.rtools.associativo.EventoBaileConvite;
import br.com.rtools.associativo.EventoBaileImpressaoConvite;
import br.com.rtools.associativo.EventoBaileMapa;
import br.com.rtools.principal.DB;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Query;
import oracle.toplink.essentials.exceptions.EJBQLException;

public class EventoBaileDao extends DB {

    public List pesquisaTodosAtuais(Date data) {
        try {
            Query qry = getEntityManager().createQuery(
                    "select r from EventoBaile r where r.data >= :date");
            qry.setParameter("date", data);
            return (qry.getResultList());
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    public String excluirBaile(int idEvento) {
        try {
            Query qry = getEntityManager().createNativeQuery(
                    " delete from eve_evento_servico_valor where id_evento_servico in (select id from eve_evento_servico where id_evento = " + idEvento + ");  "
                    + " delete from eve_evento_baile where id_evento = " + idEvento + " ;                                                                       "
                    + " delete from eve_evento_servico where id_evento = " + idEvento + " ;                                                                     "
                    + " delete from eve_evento_Banda where id_evento = " + idEvento + " ;                                                                       "
                    + " delete from eve_endereco where id_evento = " + idEvento + " ;                                                                           "
                    + " delete from eve_evento where id = " + idEvento + " ;                                                                                   ");
            qry.executeUpdate();
        } catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    public AEndereco pesquisaEnderecoEvento(int idEvento) {
        AEndereco aEndereco;
        try {
            Query qry = getEntityManager().createQuery(
                    "select ev "
                    + "  from AEndereco ev"
                    + " where ev.evento.id = " + idEvento);
            aEndereco = (AEndereco) qry.getSingleResult();
        } catch (Exception e) {
            e.getMessage();
            aEndereco = new AEndereco();
        }
        return aEndereco;
    }

    public List pesquisaEventoDescricao(String desc, String como) {
        List lista;
        String textQuery = null;
        if (como.equals("P")) {
            desc = "%" + desc.toLowerCase().toUpperCase() + "%";
            textQuery = "select objeto from EventoBaile objeto where UPPER(objeto.evento.descricaoEvento.descricao) like :desc"
                    + " order by objeto.data DESC, objeto.evento.descricaoEvento.descricao";
        } else if (como.equals("I")) {
            desc = desc.toLowerCase().toUpperCase() + "%";
            textQuery = "select objeto from EventoBaile objeto where UPPER(objeto.evento.descricaoEvento.descricao) like :desc"
                    + " order by objeto.data DESC, objeto.evento.descricaoEvento.descricao";
        }
        try {
            Query qry = getEntityManager().createQuery(textQuery);
            if (desc != null) {
                qry.setParameter("desc", desc);
            }
            lista = qry.getResultList();
        } catch (EJBQLException e) {
            lista = new ArrayList();
        }
        return lista;
    }

    public List listaBaileMapa(int id_baile) {
        String textQuery = "select ebm from EventoBaileMapa ebm where ebm.eventoBaile.id = " + id_baile + " order by ebm.mesa";
        try {
            Query qry = getEntityManager().createQuery(textQuery);
            return qry.getResultList();
        } catch (Exception e) {
            return new ArrayList();
        }
    }

    public List listaBaileConvite(int id_baile) {
        String textQuery = "select ebc from EventoBaileConvite ebc where ebc.eventoBaile.id = " + id_baile + " order by ebc.convite";
        try {
            Query qry = getEntityManager().createQuery(textQuery);
            return qry.getResultList();
        } catch (Exception e) {
            return new ArrayList();
        }
    }

    public List<EventoBaileMapa> listaBaileMapaDisponiveis(int id_baile, Integer id_status, Integer id_pessoa, Integer id_venda) {
        String textQuery
                = "SELECT ebm.* \n"
                + "  FROM eve_evento_baile_mapa ebm \n"
                + "  LEFT JOIN eve_venda v ON v.id = ebm.id_venda \n"
                + " WHERE ebm.id_evento_baile = " + id_baile;

        if (id_status == 1) {
            // DISPONÍVEL
            textQuery += " AND ebm.id_venda IS NULL \n";
        } else if (id_status == 2) {
            // RESERVADO
            textQuery += " AND ebm.id_venda IS NOT NULL AND v.id_status = 2 \n";
        } else {
            // VENDIDO
            textQuery += " AND ebm.id_venda IS NOT NULL AND v.id_status = 3 \n";
        }

        if (id_pessoa != null) {
            textQuery += " AND v.id = " + id_venda + "  AND v.id_pessoa = " + id_pessoa + " \n";
        }

        textQuery += " ORDER BY ebm.nr_mesa";

        try {
            Query qry = getEntityManager().createNativeQuery(textQuery, EventoBaileMapa.class);
            return qry.getResultList();
        } catch (Exception e) {
            e.getMessage();
            return new ArrayList();
        }
    }

    public List<EventoBaileConvite> listaBaileConviteDisponiveis(int id_baile, Integer id_status, Integer id_pessoa, Integer id_venda) {
        String textQuery
                = "SELECT ebc.* "
                + "  FROM eve_evento_baile_convite ebc "
                + "  LEFT JOIN eve_venda v ON v.id = ebc.id_venda "
                + " WHERE ebc.id_evento_baile = " + id_baile;

        if (id_status == 1) {
            // DISPONÍVEL
            textQuery += " AND ebc.id_venda IS NULL \n";
        } else if (id_status == 3) {
            // VENDIDO
            textQuery += " AND ebc.id_venda IS NOT NULL AND v.id_status = 3 \n";
        }

        if (id_pessoa != null) {
            textQuery += " AND v.id = " + id_venda + "  AND v.id_pessoa = " + id_pessoa + " \n";
        }

        textQuery += " ORDER BY ebc.nr_convite";
        try {
            Query qry = getEntityManager().createNativeQuery(textQuery, EventoBaileConvite.class);
            return qry.getResultList();
        } catch (Exception e) {
            e.getMessage();
            return new ArrayList();
        }
    }

    public EventoBaileMapa pesquisaMesaBaile(int id_baile, int mesa) {
        String textQuery = "SELECT EBM FROM EventoBaileMapa AS EBM WHERE EBM.eventoBaile.id = " + id_baile + " AND EBM.mesa = " + mesa;
        try {
            Query qry = getEntityManager().createQuery(textQuery);
            return (EventoBaileMapa) qry.getSingleResult();
        } catch (Exception e) {
            return new EventoBaileMapa();
        }
    }

    public EventoBaileConvite pesquisaConviteBaile(int id_baile, int convite) {
        String textQuery = "SELECT EBC FROM EventoBaileConvite AS EBC WHERE EBC.eventoBaile.id = " + id_baile + " AND EBC.convite = " + convite;
        try {
            Query qry = getEntityManager().createQuery(textQuery);
            return (EventoBaileConvite) qry.getSingleResult();
        } catch (Exception e) {
            return new EventoBaileConvite();
        }
    }

    public List<EventoBaileImpressaoConvite> listaEventoBaileImpressaoConvite(int id_evento_baile, String tipo) {
        String textQuery
                = "SELECT ebic.* \n "
                + "  FROM eve_evento_baile_impressao_convite ebic \n ";
        if (tipo.equals("mesa")) {
            textQuery += " WHERE ebic.id_evento_baile_mapa = " + id_evento_baile;
        } else {
            textQuery += " WHERE ebic.id_evento_baile_convite = " + id_evento_baile;
        }

        try {
            Query qry = getEntityManager().createNativeQuery(textQuery + "ORDER BY ebic.dt_impressao DESC, ebic.ds_hora_impressao DESC, ebic.id DESC ", EventoBaileImpressaoConvite.class);
            return qry.getResultList();
        } catch (Exception e) {
            return new ArrayList();
        }
    }
}
