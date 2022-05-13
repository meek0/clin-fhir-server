package bio.ferlab.clin.es.builder;

import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

@Component
public class CommonDataBuilder {

  private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private static final String ID_SEPARATOR = "/";
  
  // TODO refractoring of al indexes and put common stuff here
  
}
