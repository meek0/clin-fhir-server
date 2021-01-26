package bio.ferlab.clin.es.config;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.r4.model.*;

public class PatientDataConfiguration {
    public final IFhirResourceDao<Patient> patientDAO;
    public final IFhirResourceDao<ServiceRequest> serviceRequestDAO;
    public final IFhirResourceDao<Organization> organizationDAO;
    public final IFhirResourceDao<Practitioner> practitionerDao;
    public final IFhirResourceDao<PractitionerRole> practitionerRoleDao;

    public PatientDataConfiguration(
            IFhirResourceDao<Patient> patientDAO,
            IFhirResourceDao<ServiceRequest> serviceRequestDAO,
            IFhirResourceDao<Organization> organizationDAO,
            IFhirResourceDao<Practitioner> practitionerDao,
            IFhirResourceDao<PractitionerRole> practitionerRoleDao
    ) {
        this.patientDAO = patientDAO;
        this.serviceRequestDAO = serviceRequestDAO;
        this.organizationDAO = organizationDAO;
        this.practitionerDao = practitionerDao;
        this.practitionerRoleDao = practitionerRoleDao;
    }
}
