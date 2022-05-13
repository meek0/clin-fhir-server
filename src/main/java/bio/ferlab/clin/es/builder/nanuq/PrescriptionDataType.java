package bio.ferlab.clin.es.builder.nanuq;

public enum PrescriptionDataType {

  ANALYSIS("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-analysis-request"),
  SEQUENCING("http://fhir.cqgc.ferlab.bio/StructureDefinition/cqgc-sequencing-request");

  public final String value;

  PrescriptionDataType(String value){
    this.value = value;
  }
  
}
