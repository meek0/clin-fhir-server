package bio.ferlab.clin.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Person;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

import static bio.ferlab.clin.validation.validators.nanuq.PatientValidator.MRN_CODE;
import static bio.ferlab.clin.validation.validators.nanuq.PersonValidator.RAMQ_CODE;

@Service
@Interceptor
public class ImmutableMrnInterceptor {

  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && oldResource instanceof Patient && newResource instanceof Patient;  // instanceof is null-safe
  }
  
  private String getMrn(Patient patient) {
    return patient.getIdentifier().stream().filter(identifier -> MRN_CODE.equals(identifier.getType().getCodingFirstRep().getCode()))
        .findFirst().map(Identifier::getValue).orElse(null);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void updated(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    if (isValidRequestAndResource(requestDetails, oldResource, newResource)) {
      final String oldMrn = getMrn((Patient)oldResource);
      final String newMrn = getMrn((Patient)newResource);
      if (StringUtils.isNoneBlank(oldMrn, newMrn) && !oldMrn.equals(newMrn)) {
        throw new InvalidRequestException("Can't change the MRN of Patient/" + ((Patient) oldResource).getIdElement().getIdPart());
      }
    }
  }
}
