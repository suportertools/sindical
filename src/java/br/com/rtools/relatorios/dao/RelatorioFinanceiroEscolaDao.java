/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.rtools.relatorios.dao;

import br.com.rtools.financeiro.Servicos;
import br.com.rtools.principal.DB;
import br.com.rtools.relatorios.RelatorioGrupo;
import br.com.rtools.relatorios.RelatorioParametros;
import br.com.rtools.relatorios.Relatorios;
import br.com.rtools.utilitarios.Debugs;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Query;

/**
 *
 * @author Claudemir Rtools
 */
public class RelatorioFinanceiroEscolaDao extends DB {

    public List<Object> listaRelatorioFinanceiro(String id_servicos, String data_vencimento, String data_vencimento_final, String data_quitacao, String data_quitacao_final, String tipo_pessoa, Integer id_pessoa, String order, Relatorios relatorio, Double desconto, Double desconto_final, String in_turmas, String status) {
        String select
                = " SELECT ";

        List<RelatorioParametros> listaRL = new RelatorioDao().listaRelatorioParametro(relatorio.getId());

        if (!listaRL.isEmpty()) {
            String s = "";
            for (RelatorioParametros rp : listaRL) {
                if (s.isEmpty()) {
                    s = rp.getParametro() + " AS " + rp.getApelido();
                } else {
                    s += ", " + " \n " + rp.getParametro() + " AS " + rp.getApelido();
                }
            }
            select += s + " \n ";
        } else {
            return new ArrayList();
        }

        select += "  FROM movimentos_vw AS m \n "
                + " INNER JOIN pes_pessoa AS r ON r.id = m.id_pessoa"
                + " INNER JOIN pes_pessoa AS b ON b.id = m.id_beneficiario"
                + "  LEFT JOIN soc_socios_vw AS s ON s.codsocio = b.id";

//       select += "       FROM movimentos_vw AS m                               \n "
//                + " INNER JOIN pes_pessoa AS r ON r.id = m.id_pessoa            \n"
//                + " INNER JOIN pes_pessoa AS b ON b.id = m.id_beneficiario      \n"
//                + " INNER JOIN fin_servico_pessoa AS SP ON SP.id_cobranca = M.id_pessoa \n"
//                + "  LEFT JOIN soc_socios_vw AS s ON s.codsocio = b.id          \n";
//        if (!in_turmas.isEmpty()) {
//            //select += "  LEFT JOIN esc_turma     AS T ON T.id_curso = S.id_servico  \n";
//        }        
        List<String> list_where = new ArrayList();
        if (id_pessoa != null) {
            switch (tipo_pessoa) {
                case "aluno":
                    list_where.add(" b.id = " + id_pessoa + " \n ");
                    break;
                default:
                    list_where.add(" r.id = " + id_pessoa + " \n ");
                    break;
            }
        }

        list_where.add(" m.id_grupo = 507  \n ");

        // DATA VENCIMENTO ---
        if (!data_vencimento.isEmpty() && !data_vencimento_final.isEmpty()) {
            list_where.add(" m.vencimento BETWEEN '" + data_vencimento + "' and '" + data_vencimento_final + "' \n ");
        } else if (!data_vencimento.isEmpty() && data_vencimento_final.isEmpty()) {
            list_where.add(" m.vencimento >= '" + data_vencimento + "' \n ");
        } else if (data_vencimento.isEmpty() && !data_vencimento_final.isEmpty()) {
            list_where.add(" m.vencimento <= '" + data_vencimento_final + "' \n ");
        }

        // DATA QUITAÇÃO ---
        if (!data_quitacao.isEmpty() && !data_quitacao_final.isEmpty()) {
            list_where.add(" m.baixa BETWEEN '" + data_quitacao + "' and '" + data_quitacao_final + "' \n ");
        } else if (!data_quitacao.isEmpty() && data_quitacao_final.isEmpty()) {
            list_where.add(" m.baixa >= '" + data_quitacao + "' \n ");
        } else if (data_quitacao.isEmpty() && !data_quitacao_final.isEmpty()) {
            list_where.add(" m.baixa <= '" + data_quitacao_final + "' \n ");
        }

        // DATA QUITAÇÃO ---
        if (!id_servicos.isEmpty()) {
            list_where.add(" m.id_servico IN (" + id_servicos + ") \n ");
        }
        // STATUS
        if (status != null && !status.isEmpty()) {
            switch (status) {
                case "abertos":
                    list_where.add("M.id_baixa IS NULL \n");
                    break;
                case "quitados":
                    list_where.add("M.id_baixa IS NOT NULL \n");
                    break;
                case "atrasados":
                    list_where.add("M.id_baixa IS NULL AND M.vencimento < current_date \n");
                    break;
                default:
                    break;
            }
        }
        // FAIXA DE DESCONTO
//        if (desconto != null && desconto_final != null) {
//            list_where.add("SP.nr_desconto BETWEEN " + desconto + " AND " + desconto_final);
//        } else if (desconto != null && desconto_final == null) {
//            list_where.add("SP.nr_desconto >= " + desconto);
//        } else if (desconto == null && desconto_final != null) {
//            list_where.add("SP.nr_desconto <= " + desconto_final);
//        }
        // TURMA
        if (!in_turmas.isEmpty()) {
            //list_where.add("T.id IN(" + in_turmas + ")");
            // list_where.add("SP.dt_emissao BETWEEN T.dt_inicio AND T.dt_termino");
        }

        if (list_where.isEmpty()) {
            return new ArrayList();
        }

        String where = "";
        for (String linha : list_where) {
            if (where.isEmpty()) {
                where = " WHERE " + linha + " \n";
            } else {
                where += " AND " + linha + " \n";
            }
        }

        List<RelatorioGrupo> listaRG = new RelatorioDao().listaRelatorioGrupo(relatorio.getId());
        String group = "";
        if (!listaRG.isEmpty()) {
            String g = "";
            for (RelatorioGrupo rg : listaRG) {
                if (g.isEmpty()) {
                    g = rg.getGrupo() + " \n ";
                } else {
                    g += ", " + rg.getGrupo() + " \n ";
                }
            }
            group = " GROUP BY \n ";
            group += g;
        }

        if (!order.isEmpty()) {
            order = " ORDER BY " + order;
        }

        Debugs.put("habilitaDebugQuery", select + where + group + order);
        Query qry = getEntityManager().createNativeQuery(select + where + group + order);

        try {
            return qry.getResultList();
        } catch (Exception e) {
            e.getMessage();
        }

        return new ArrayList();
    }

    public List<Servicos> listaServicos() {
        try {
            Query qry = getEntityManager().createNativeQuery(
                    "  SELECT s.* \n"
                    + "  FROM fin_servicos s \n "
                    + " WHERE s.id \n "
                    + "    IN (SELECT sr.id_servicos FROM fin_servico_rotina sr WHERE sr.id_rotina IN (150, 151, 314) GROUP BY sr.id_servicos ORDER BY sr.id_servicos) \n "
                    + "   AND s.ds_situacao = 'A'",
                    Servicos.class
            );

            return qry.getResultList();
        } catch (Exception e) {
            e.getMessage();
        }
        return new ArrayList();
    }
}
