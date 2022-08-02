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
import org.hl7.fhir.r4.model.Person;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

import static bio.ferlab.clin.validation.validators.nanuq.PersonValidator.RAMQ_CODE;

@Service
@Interceptor
public class ImmutableRamqInterceptor {

  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && oldResource instanceof Person && newResource instanceof Person;  // instanceof is null-safe
  }
  
  private String getRamq(Person person) {
    return person.getIdentifier().stream().filter(identifier -> RAMQ_CODE.equals(identifier.getType().getCodingFirstRep().getCode()))
        .findFirst().map(Identifier::getValue).orElse(null);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void updated(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    if (isValidRequestAndResource(requestDetails, oldResource, newResource)) {
      final String oldRamq = getRamq((Person)oldResource);
      final String newRamq = getRamq((Person)newResource);
      if (StringUtils.isNoneBlank(oldRamq, newRamq) && !oldRamq.equals(newRamq)) {
        throw new InvalidRequestException("Can't change the RAMQ of Person/" + ((Person) oldResource).getIdElement().getIdPart());
      }
    }
  }
}
