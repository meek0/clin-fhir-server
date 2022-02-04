package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.properties.BioProperties;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRuleTester;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.springframework.stereotype.Component;

@Component
public class MetaTagAuthRuleTester implements IAuthRuleTester {
  
  private final BioProperties bioProperties;
  
  public MetaTagAuthRuleTester(BioProperties bioProperties) {
    this.bioProperties = bioProperties;
  }
  
  @Override
  public boolean matches(RestOperationTypeEnum theOperation, RequestDetails theRequestDetails, IIdType theInputResourceId, IBaseResource theInputResource) {
    boolean allowed = IAuthRuleTester.super.matches(theOperation, theRequestDetails, theInputResourceId, theInputResource);
    if(bioProperties.isTaggingEnabled()) {
      
    }
    return allowed;
  }

  @Override
  public boolean matchesOutput(RestOperationTypeEnum theOperation, RequestDetails theRequestDetails, IBaseResource theOutputResource) {
    boolean allowed =  IAuthRuleTester.super.matchesOutput(theOperation, theRequestDetails, theOutputResource);
    if(bioProperties.isTaggingEnabled()) {

    }
    return allowed;
  }
}
