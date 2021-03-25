package ca.uhn.fhir.jpa.app.cql;

import ca.uhn.fhir.cql.config.CqlR4Config;
import ca.uhn.fhir.jpa.app.annotations.OnR4Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

@Conditional({OnR4Condition.class, CqlConfigCondition.class})
@Import({CqlR4Config.class})
public class StarterCqlR4Config {
}
