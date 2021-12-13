package bio.ferlab.clin.es.data;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrescriptionDataTest {
  
  @Nested
  class BuildState {
    @Test
    public void submitted() {
      final PrescriptionData data = new PrescriptionData();
      assertNull(data.getState());
      data.setStatus("on-hold");
      data.setSubmitted(true);
      data.buildState();
      assertEquals(PrescriptionData.State.submitted, data.getState());
    }
    @Test
    public void incomplete() {
      final PrescriptionData data = new PrescriptionData();
      assertNull(data.getState());
      data.setStatus("on-hold");
      data.setSubmitted(false);
      data.buildState();
      assertEquals(PrescriptionData.State.incomplete, data.getState());
    }
    @Test
    public void active() {
      final PrescriptionData data = new PrescriptionData();
      assertNull(data.getState());
      data.setStatus("active");
      data.buildState();
      assertEquals(PrescriptionData.State.active, data.getState());
    }
    @Test
    public void completed() {
      final PrescriptionData data = new PrescriptionData();
      assertNull(data.getState());
      data.setStatus("completed");
      data.buildState();
      assertEquals(PrescriptionData.State.completed, data.getState());
    }
    @Test
    public void revoked() {
      final PrescriptionData data = new PrescriptionData();
      assertNull(data.getState());
      data.setStatus("revoked");
      data.buildState();
      assertEquals(PrescriptionData.State.revoked, data.getState());
    }
  }

}