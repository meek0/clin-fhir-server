package bio.ferlab.clin.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.IPreResourceAccessDetails;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Interceptor
public class SameRequestInterceptor {

  private final Map<RequestDetails, List<IBaseResource>> requests = new ConcurrentHashMap<>();

  @Hook(Pointcut.STORAGE_PREACCESS_RESOURCES)
  public void concatResources(IPreResourceAccessDetails preResourceShowDetails, RequestDetails requestDetails) {
    List<IBaseResource> resources = new ArrayList<>();
    for (int i=0; i<preResourceShowDetails.size(); i++) {
      resources.add(preResourceShowDetails.getResource(i));
    }
    requests.computeIfAbsent(requestDetails, rd -> new ArrayList<>());
    requests.get(requestDetails).addAll(resources);
  }
  
  @Hook(Pointcut.SERVER_PROCESSING_COMPLETED)
  public void remove(RequestDetails requestDetails) {
    requests.remove(requestDetails);
  }

  /**
   * Return list of resources associated to a RequestDetails.
   * Resources from this list are read-only, modifying a field won't do anything.
   * If you want to mask/edit data before the request ends then use STORAGE_PRESHOW_RESOURCES 
   * (cf PrescriptionMaskingInterceptor)
   * Be aware that resources listed in STORAGE_PRESHOW_RESOURCES aren't equals from JAVA object point of view 
   * (cf FhirUtils.equals() to compare them)
   * 
   * @param requestDetails current RequestDetails
   * @return read-only resources of the RequestDetails
   */
  public List<IBaseResource> get(RequestDetails requestDetails) {
    return requests.get(requestDetails);
  }
}
