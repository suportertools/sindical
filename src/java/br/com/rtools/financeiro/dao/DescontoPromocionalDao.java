package br.com.rtools.financeiro.dao;

import br.com.rtools.arrecadacao.ConvencaoPeriodo;
import br.com.rtools.financeiro.DescontoPromocional;
import br.com.rtools.financeiro.Servicos;
import br.com.rtools.principal.DB;
import br.com.rtools.utilitarios.DataHoje;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.persistence.Query;

public class DescontoPromocionalDao extends DB {

    public List<Servicos> listaServicosDisponiveis(int idSubGrupoConvenio) {
        try {
            Query qry = getEntityManager().createQuery("SELECT S FROM Servicos AS S WHERE S.situacao = 'A' AND S.subGrupoFinanceiro.id = :pid AND S.id NOT IN (SELECT SR.servicos.id FROM ServicoRotina AS SR WHERE SR.rotina.id = 4 GROUP BY SR.servicos.id) ORDER BY S.descricao ASC ");
            qry.setParameter("pid", idSubGrupoConvenio);
            List list = qry.getResultList();
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            return new ArrayList();
        }
        return new ArrayList();
    }

    /**
     * @param por todos ou naoVencidos
     * @return 0 - fin_desconto_promocional.id 1 - fin_servicos.ds_descricao 2 -
     * soc_categoria.ds_categoria 3 - func_valor_servico_idade.valor 4 -
     * fin_desconto_promocional.nr_desconto 5 -
     * fin_desconto_promocional.ds_referencia_inicial 6 -
     * fin_desconto_promocional.ds_referencia_final 7 -
     * fin_servico_valor.nr_idade_ini 8 - fin_servico_valor.nr_idade_fim
     *
     */
    public List<Vector> listaDescontoPromocional(String por) {
        try {
            String text = "SELECT dp.id, \n"
                    + "       s.ds_descricao, \n"
                    + "       c.ds_categoria, \n"
                    + "       func_valor_servico_idade(sv.nr_idade_ini, s.id, CURRENT_DATE, 0, c.id) AS valor, \n"
                    + "       dp.nr_desconto, \n"
                    + "       dp.ds_referencia_inicial, \n"
                    + "       dp.ds_referencia_final, \n "
                    + "       sv.nr_idade_ini, \n "
                    + "       sv.nr_idade_fim \n "
                    + "  FROM fin_desconto_promocional dp \n "
                    + "  LEFT JOIN soc_categoria c ON c.id = dp.id_categoria \n "
                    + " INNER JOIN fin_servicos s ON s.id = dp.id_servico \n "
                    + " INNER JOIN fin_servico_valor sv ON sv.id_servico = s.id"
                    + "      WHERE dp.is_mensal = true \n";
            String and = "";

            if (por.equals("naoVencidos")) {
                and = " AND '" + DataHoje.data().substring(3) + "' BETWEEN dp.ds_referencia_inicial AND dp.ds_referencia_final ";
            }
            Query qry = getEntityManager().createNativeQuery(text + and + " ORDER BY s.ds_descricao, c.ds_categoria ");
            return qry.getResultList();
        } catch (Exception e) {
            e.getMessage();
        }
        return new ArrayList();
    }

    public List<DescontoPromocional> listaDescontoPromocional(Integer id_servico, Integer id_categoria, String ref_inicial, String ref_final) {
        try {
            String text
                    = "SELECT ds.* \n "
                    + "  FROM fin_desconto_promocional ds \n "
                    + " WHERE ds.id_servico = " + id_servico + " \n "
                    + "   AND ds.id_categoria " + ((id_categoria == null) ? " IS NULL " : " = " + id_categoria) + " \n ";
            Query qry = getEntityManager().createNativeQuery(text, DescontoPromocional.class);
            return qry.getResultList();
        } catch (Exception e) {
            e.getMessage();
        }
        return new ArrayList();
    }

    public DescontoPromocional findByServicoNaoMensal(Integer servico_id) {
        try {
            String text
                    = "SELECT DP.*                                              \n "
                    + "  FROM fin_desconto_promocional AS DP                    \n "
                    + " WHERE DP.id_servico = " + servico_id + "                \n "
                    + "   AND DP.id_categoria IS NULL                           \n"
                    + "   AND DP.is_mensal = false                              \n";
            Query qry = getEntityManager().createNativeQuery(text, DescontoPromocional.class);
            return (DescontoPromocional) qry.getSingleResult();
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public List<DescontoPromocional> findListByServicoNaoMensal(Integer servico_id) {
        try {
            String text
                    = "SELECT DP.*                                              \n "
                    + "  FROM fin_desconto_promocional AS DP                    \n "
                    + " WHERE DP.id_servico = " + servico_id + "                \n "
                    + "   AND DP.id_categoria IS NULL                           \n"
                    + "   AND DP.is_mensal = false                              \n";
            Query qry = getEntityManager().createNativeQuery(text, DescontoPromocional.class);
            return qry.getResultList();
        } catch (Exception e) {
            e.getMessage();
        }
        return new ArrayList();
    }

    public DescontoPromocional findByServicoMatricula(Integer servico_id) {
        String queryString = ""
                + "     SELECT DP.*                                             \n"
                + "       FROM fin_desconto_promocional AS DP                   \n"
                + "      WHERE DP.id_servico = " + servico_id + "               \n"
                + "        AND current_date BETWEEN cast('01/'||DP.ds_referencia_inicial AS date)                                   \n"
                + "        AND date_trunc('month',cast('01/' || ds_referencia_final AS date)) + INTERVAL'1 month' - INTERVAL'1 day' \n"
                + "        AND DP.id_categoria IS NULL                                                                              \n";

        try {
            Query query = getEntityManager().createNativeQuery(queryString, DescontoPromocional.class);
            return (DescontoPromocional) query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

}
