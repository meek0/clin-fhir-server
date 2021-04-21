package bio.ferlab.clin.dao;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.r4.model.*;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaoConfiguration {
    public IFhirResourceDao<Patient> patientDao(DaoRegistry daoRegistry) {
        return daoRegistry.getResourceDao(Patient.class);
    }

    public IFhirResourceDao<ServiceRequest> serviceRequestDao(DaoRegistry daoRegistry) {
        return daoRegistry.getResourceDao(ServiceRequest.class);
    }

    public IFhirResourceDao<ClinicalImpression> clinicalImpression(DaoRegistry daoRegistry) {
        return daoRegistry.getResourceDao(ClinicalImpression.class);
    }

    public IFhirResourceDao<Group> groupDao(DaoRegistry daoRegistry) {
        return daoRegistry.getResourceDao(Group.class);
    }

    public IFhirResourceDao<Organization> organizationDao(DaoRegistry daoRegistry) {
        return daoRegistry.getResourceDao(Organization.class);
    }

    public IFhirResourceDao<Practitioner> practitionerDao(DaoRegistry daoRegistry) {
        return daoRegistry.getResourceDao(Practitioner.class);
    }

    public IFhirResourceDao<AuditEvent> auditEventDao(DaoRegistry daoRegistry) {
        return daoRegistry.getResourceDao(AuditEvent.class);
    }

    public IFhirResourceDao<Observation> observationDao(DaoRegistry daoRegistry) {
        return daoRegistry.getResourceDao(Observation.class);
    }
}
