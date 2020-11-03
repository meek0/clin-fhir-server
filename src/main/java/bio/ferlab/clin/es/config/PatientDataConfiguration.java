package bio.ferlab.clin.es.config;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;

public class PatientDataConfiguration {
    public final IFhirResourceDao<ServiceRequest> serviceRequestDAO;
    public final IFhirResourceDao<Organization> organizationDAO;
    public final IFhirResourceDao<Practitioner> practitionerDao;

    public PatientDataConfiguration(
            IFhirResourceDao<ServiceRequest> serviceRequestDAO,
            IFhirResourceDao<Organization> organizationDAO,
            IFhirResourceDao<Practitioner> practitionerDao
    ) {
        this.serviceRequestDAO = serviceRequestDAO;
        this.organizationDAO = organizationDAO;
        this.practitionerDao = practitionerDao;
    }
}
