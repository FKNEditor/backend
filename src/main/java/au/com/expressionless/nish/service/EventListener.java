package au.com.expressionless.nish.service;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import javax.transaction.Transactional;

import org.jboss.logging.Logger;

import au.com.expressionless.nish.models.entity.edition.Edition;
import au.com.expressionless.nish.models.entity.edition.Keyword;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class EventListener {
    
    @Inject
    Logger log;
    
    @Inject
    MinIO minIO;

    @Transactional
    public void init(@Observes StartupEvent e) {
        // String name = "Bla";
        // Optional<Edition> optE = Edition.findByName(name);
        // Edition ee;
        // if(!optE.isPresent()) {
        //     ee = new Edition();
        //     ee.setFileName(name);
        //     ee.setAuthor("Veta");
        //     ee.persist();
        // } else {
        //     ee = optE.get();
        // }

        // // Keyword keyword = new Keyword("Bla", ee, 1.0);
        // // keyword.persist();

        // Keyword k = Keyword.findByEditionAndWord("Bla", 47);
        // if(k != null)
        //     System.out.println(k.getKeyword());

        // List<Edition> editions = Keyword.listEditionsByKeywordSearch("Bla", false, 1);
        // System.out.println("Editions: ");
        // for(Edition eee : editions) {
        //     System.out.println(eee.getFileName());
        // }
        // System.out.println(editions.size());
    }
}
