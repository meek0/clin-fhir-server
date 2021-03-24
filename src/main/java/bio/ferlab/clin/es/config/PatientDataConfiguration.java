package bio.ferlab.clin.es.config;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.r4.model.*;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PatientDataConfiguration {
    public final IFhirResourceDao<Patient> patientDAO;
    public final IFhirResourceDao<ServiceRequest> serviceRequestDAO;
    public final IFhirResourceDao<ClinicalImpression> clinicalImpressionDAO;
    public final IFhirResourceDao<Organization> organizationDAO;
    public final IFhirResourceDao<Practitioner> practitionerDao;
    public final IFhirResourceDao<PractitionerRole> practitionerRoleDao;
    public final IFhirResourceDao<Group> groupDao;

    public PatientDataConfiguration(
            IFhirResourceDao<Patient> patientDAO,
            IFhirResourceDao<ServiceRequest> serviceRequestDAO,
            IFhirResourceDao<ClinicalImpression> clinicalImpressionDAO,
            IFhirResourceDao<Organization> organizationDAO,
            IFhirResourceDao<Practitioner> practitionerDao,
            IFhirResourceDao<PractitionerRole> practitionerRoleDao,
            IFhirResourceDao<Group> groupDao) {
        this.patientDAO = patientDAO;
        this.serviceRequestDAO = serviceRequestDAO;
        this.clinicalImpressionDAO = clinicalImpressionDAO;
        this.organizationDAO = organizationDAO;
        this.practitionerDao = practitionerDao;
        this.practitionerRoleDao = practitionerRoleDao;
        this.groupDao = groupDao;
    }
}
