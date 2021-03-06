package br.com.rtools.associativo.dao;

import br.com.rtools.associativo.EventoBanda;
import br.com.rtools.principal.DB;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Query;

public class EventoBandaDao extends DB {

    public List<EventoBanda> pesquisaBandasDoEvento(Integer evento_id) {
        try {
            Query query = getEntityManager().createQuery("SELECT EB FROM EventoBanda EB WHERE EB.evento.id = :evento_id ORDER BY EB.banda.descricao ASC");
            query.setParameter("evento_id", evento_id);
            return query.getResultList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
