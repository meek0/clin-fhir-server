package bio.ferlab.clin.utils;

import bio.ferlab.clin.es.builder.nanuq.AbstractPrescriptionDataBuilder;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static bio.ferlab.clin.interceptors.metatag.PrescriptionMasking.RESTRICTED_FIELD;

public class MaskingUtils {
  
  private MaskingUtils(){}

  public static boolean isAnalysis(ServiceRequest serviceRequest) {
    return serviceRequest != null && serviceRequest.getMeta().getProfile().stream()
        .anyMatch(s -> AbstractPrescriptionDataBuilder.Type.ANALYSIS.value.equals(s.getValue()));
  }
  
  public static boolean areLinked(ServiceRequest sr, Patient p) {
    return Optional.ofNullable(sr).map(ServiceRequest::getSubject).map(FhirUtils::extractId).stream()
        .anyMatch(id -> p != null && id.equals(p.getIdElement().getIdPart()));
  }

  public static boolean areLinked(Person pers, Patient patient) {
    return pers != null && pers.getLink().stream().anyMatch(l -> patient != null && FhirUtils.extractId(l.getTarget()).equals(patient.getIdElement().getIdPart()));
  }

  public static boolean isValidPrescriptionRequest(List<IBaseResource> resources) {
    // the following algorithm detects if the current request is about displaying a Prescription
    final ServiceRequest analysis = extractAnalysis(resources);
    final List<Patient> patients = FhirUtils.extractAllOfType(resources, Patient.class);
    final List<Person> persons = FhirUtils.extractAllOfType(resources, Person.class);
    if (analysis != null && patients.size() == 1 && persons.size() == 1) {
      Patient patient = patients.get(0);
      Person pers = persons.get(0);
      return MaskingUtils.areLinked(analysis, patient) && MaskingUtils.areLinked(pers, patient);
    }
    return false;
  }
  
  public static Person extractPerson(IPreResourceShowDetails details) {
    final List<IBaseResource> resources = new ArrayList<>();
    if (details != null) {
      details.forEach(resources::add);
    }
    final List<Person> persons = FhirUtils.extractAllOfType(resources, Person.class);
    return persons.isEmpty() ? null : persons.get(0);
  }
  
  public static ServiceRequest extractAnalysis(List<? extends IBaseResource> resources) {
    return FhirUtils.extractAllOfType(resources, ServiceRequest.class).stream().filter(MaskingUtils::isAnalysis).findFirst().orElse(null);
  }

  public static boolean isAlreadyMasked(Person person) {
    return Optional.ofNullable(person).map(Person::getId).stream().anyMatch(id -> id.equals(RESTRICTED_FIELD));
  }
  
}
