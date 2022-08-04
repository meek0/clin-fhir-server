package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.es.config.ResourceDaoConfiguration;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Person;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

import static bio.ferlab.clin.validation.validators.nanuq.PersonValidator.RAMQ_CODE;

@Service
@Interceptor
@RequiredArgsConstructor
public class ImmutableRamqInterceptor {

  private final ResourceDaoConfiguration configuration;

  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource newResource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && newResource instanceof Person;  // instanceof is null-safe
  }

  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && oldResource instanceof Person && newResource instanceof Person;  // instanceof is null-safe
  }
  
  private String getRamq(Person person) {
    return person.getIdentifier().stream().filter(identifier -> RAMQ_CODE.equals(identifier.getType().getCodingFirstRep().getCode()))
        .findFirst().map(Identifier::getValue).orElse(null);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
  public void created(RequestDetails requestDetails, IBaseResource newResource) {
    if (isValidRequestAndResource(requestDetails, newResource)) {
      final String ramq = getRamq((Person)newResource);
      if (StringUtils.isNotBlank(ramq)) {
        final IBundleProvider search = this.configuration.personDAO
            .search(SearchParameterMap.newSynchronous().add(Person.SP_IDENTIFIER, new TokenParam(ramq)));
        if(!search.isEmpty()) {
          throw new InvalidRequestException("Duplicated person with same RAMQ " + getObstructed(ramq));
        }
      }
    }
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void updated(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    if (isValidRequestAndResource(requestDetails, oldResource, newResource)) {
      final String oldRamq = getRamq((Person)oldResource);
      final String newRamq = getRamq((Person)newResource);
      if (StringUtils.isNotBlank(oldRamq) && !oldRamq.equals(newRamq)) {
        throw new InvalidRequestException("Can't change the RAMQ (" + getObstructed(oldRamq) + ") of Person/" + ((Person) oldResource).getIdElement().getIdPart());
      }
    }
  }
  
  private String getObstructed(String ramq) {
    return StringUtils.substring(ramq, 0, 4) + "...";
  }
  
}
