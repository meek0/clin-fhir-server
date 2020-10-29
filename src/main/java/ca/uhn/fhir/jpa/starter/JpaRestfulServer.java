package ca.uhn.fhir.jpa.starter;

import bio.ferlab.clin.resourceproviders.NDJsonResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;

public class JpaRestfulServer extends BaseJpaRestfulServer {

  private static final long serialVersionUID = 1L;
  private final Logger logger = LoggerFactory.getLogger(JpaRestfulServer.class);

  @Override
  protected void initialize() throws ServletException {
    super.initialize();

    ApplicationContext applicationContext = (ApplicationContext) getServletContext()
            .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

    registerProviders(new NDJsonResourceProvider(applicationContext));
  }
}
