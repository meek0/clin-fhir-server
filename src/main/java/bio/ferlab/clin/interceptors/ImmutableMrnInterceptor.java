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
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

import static bio.ferlab.clin.validation.validators.nanuq.PatientValidator.MRN_CODE;

@Service
@Interceptor
@RequiredArgsConstructor
public class ImmutableMrnInterceptor {

  private final ResourceDaoConfiguration configuration;

  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource newResource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && newResource instanceof Patient;  // instanceof is null-safe
  }

  private boolean isValidRequestAndResource(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    return EnumSet.of(RequestTypeEnum.POST, RequestTypeEnum.PUT).contains(requestDetails.getRequestType())
        && oldResource instanceof Patient && newResource instanceof Patient;  // instanceof is null-safe
  }
  
  private String getMrn(Patient patient) {
    return patient.getIdentifier().stream().filter(identifier -> MRN_CODE.equals(identifier.getType().getCodingFirstRep().getCode()))
        .findFirst().map(Identifier::getValue).orElse(null);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
  public void created(RequestDetails requestDetails, IBaseResource newResource) {
    if (isValidRequestAndResource(requestDetails, newResource)) {
      final String mrn = getMrn((Patient) newResource);
      if (StringUtils.isNotBlank(mrn)) {
        final IBundleProvider search = this.configuration.patientDAO
            .search(SearchParameterMap.newSynchronous().add(Patient.SP_IDENTIFIER, new TokenParam(mrn)));
        if(!search.isEmpty()) {
          throw new InvalidRequestException("Duplicated person with same MRN " + getObstructed(mrn));
        }
      }
    }
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void updated(RequestDetails requestDetails, IBaseResource oldResource, IBaseResource newResource) {
    if (isValidRequestAndResource(requestDetails, oldResource, newResource)) {
      final String oldMrn = getMrn((Patient)oldResource);
      final String newMrn = getMrn((Patient)newResource);
      if (StringUtils.isNotBlank(oldMrn) && !oldMrn.equals(newMrn)) {
        throw new InvalidRequestException("Can't change the MRN (" + getObstructed(oldMrn) + ") of Patient/" + ((Patient) oldResource).getIdElement().getIdPart());
      }
    }
  }
  
  private String getObstructed(String mrn) {
    return StringUtils.substring(mrn, 0, 4) + "...";
  }
  
}
