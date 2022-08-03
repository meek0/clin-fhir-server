package bio.ferlab.clin.validation.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorUtilsTest {
  
  @Test 
  void isTrimmed() {
    // OK
    assertTrue(ValidatorUtils.isTrimmed(null));
    assertTrue(ValidatorUtils.isTrimmed(""));
    assertTrue(ValidatorUtils.isTrimmed("foo"));
    // KO
    assertFalse(ValidatorUtils.isTrimmed(" "));
    assertFalse(ValidatorUtils.isTrimmed(" foo"));
    assertFalse(ValidatorUtils.isTrimmed(" foo "));
  }

}