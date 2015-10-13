package br.com.rtools.pessoa.db;

import br.com.rtools.endereco.Endereco;
import br.com.rtools.pessoa.PessoaEndereco;
import br.com.rtools.principal.DB;
import br.com.rtools.utilitarios.AnaliseString;
import br.com.rtools.utilitarios.SalvarAcumuladoDB;
import br.com.rtools.utilitarios.SalvarAcumuladoDBToplink;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.persistence.Query;

public class PessoaEnderecoDBToplink extends DB implements PessoaEnderecoDB {

    @Override
    public List pesquisaEndPorPessoa(int id) {
        try {
            Query qry = getEntityManager().createQuery(
                    "select pe"
                    + "  from PessoaEndereco pe"
                    + " where pe.pessoa.id = :pid"
                    + " order by pe.tipoEndereco.id");
            qry.setParameter("pid", id);
            List list = qry.getResultList();
            return list;
        } catch (Exception e) {
            return null;
        }
    }
 

    @Override
    public PessoaEndereco pesquisaEndPorPessoaTipo(int idPessoa, int idTipoEndereco) {
        try {
            Query qry = getEntityManager().createQuery(
                    "select pe"
                    + "  from PessoaEndereco pe"
                    + " where pe.pessoa.id = :p"
                    + "   and pe.tipoEndereco.id = :t");
            qry.setParameter("p", idPessoa);
            qry.setParameter("t", idTipoEndereco);
            return (PessoaEndereco) qry.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Endereco enderecoReceita(String cep, String[] descricao, String[] bairro) {
//        List vetor;
//        Endereco endereco = new Endereco();
//        SalvarAcumuladoDB dB = new SalvarAcumuladoDBToplink();

        try {
            String text_qry = "SELECT e.* \n "
                            + "  FROM end_endereco e \n "
                            + " INNER JOIN end_descricao_endereco de ON (de.id = e.id_descricao_endereco) \n "
                            + " INNER JOIN end_bairro ba ON (ba.id = e.id_bairro) \n "
                            + " WHERE ds_cep = '" + cep + "' \n ";
            String or_desc = "", or_bairro = "";
            for (int i = 0; i < descricao.length; i++) {
                if (descricao.length == 1) {
                    text_qry += " AND ( UPPER(TRANSLATE(de.ds_descricao)) LIKE UPPER('%" + AnaliseString.normalizeLower(descricao[i]) + "%') )  \n ";
                    break;
                } else {
                    or_desc += " OR UPPER(TRANSLATE(de.ds_descricao)) LIKE UPPER('%" + AnaliseString.normalizeLower(descricao[i]) + "%') \n ";
                }
            }
            if (descricao.length > 1) {
                text_qry += " AND ( UPPER(TRANSLATE(de.ds_descricao)) LIKE UPPER('%" + AnaliseString.normalizeLower(descricao[0]) + "%') " + or_desc + ") \n ";
            }


            for (int i = 0; i < bairro.length; i++) {
                if (bairro.length == 1) {
                    text_qry += " AND ( UPPER(TRANSLATE(ba.ds_descricao)) LIKE UPPER('%" + AnaliseString.normalizeLower(bairro[i]) + "%') ) \n ";
                    break;
                } else {
                    or_bairro += " OR UPPER(TRANSLATE(ba.ds_descricao)) LIKE UPPER('%" + AnaliseString.normalizeLower(bairro[i]) + "%') \n ";
                }
            }
            if (bairro.length > 1) {
                text_qry += " AND ( UPPER(TRANSLATE(ba.ds_descricao)) LIKE UPPER('%" + AnaliseString.normalizeLower(bairro[0]) + "%') " + or_bairro + ") ";
            }

            Query qry = getEntityManager().createNativeQuery(text_qry, Endereco.class);
            qry.setMaxResults(1);
            
            return (Endereco) qry.getSingleResult();
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }
    
    @Override
    public List<PessoaEndereco> listaEnderecoContabilidadeDaEmpresa(Integer id_empresa, Integer id_tipo_endereco){
        String text_qry =
                "SELECT pe.* \n " +
                "  FROM pes_pessoa_endereco pe \n " +
                " INNER JOIN pes_juridica jc ON jc.id_pessoa = pe.id_pessoa \n " +
                " INNER JOIN pes_juridica j ON j.id_contabilidade = jc.id \n " +
                " WHERE j.id = " + id_empresa;
        String and = "";
        if(id_tipo_endereco != null){
            and = " AND pe.id_tipo_endereco = " + id_tipo_endereco;
        }
        
        Query qry = getEntityManager().createNativeQuery(text_qry + and, PessoaEndereco.class);
        
        try{
            return qry.getResultList();
        }catch(Exception e){
            e.getMessage();
        }
        return new ArrayList();
    }    
}
