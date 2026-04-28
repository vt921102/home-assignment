package com.toanlv.flashsale.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashUtilsTest {

  // ----------------------------------------------------------------
  // sha256
  // ----------------------------------------------------------------

  @Test
  void sha256_producesDeterministicOutput() {
    var first = HashUtils.sha256("hello");
    var second = HashUtils.sha256("hello");

    assertThat(first).isEqualTo(second);
  }

  @Test
  void sha256_producesHexStringOf64Chars() {
    var hash = HashUtils.sha256("test-input");

    assertThat(hash).hasSize(64);
    assertThat(hash).matches("[0-9a-f]{64}");
  }

  @Test
  void sha256_differentInputsProduceDifferentHashes() {
    var hash1 = HashUtils.sha256("input-a");
    var hash2 = HashUtils.sha256("input-b");

    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  void sha256_knownVector() {
    // SHA-256("abc") = well-known test vector
    var expected = "ba7816bf8f01cfea414140de5dae2223" + "b00361a396177a9cb410ff61f20015ad";
    assertThat(HashUtils.sha256("abc")).isEqualTo(expected);
  }

  // ----------------------------------------------------------------
  // hashOtp
  // ----------------------------------------------------------------

  @Test
  void hashOtp_sameInputProducesSameHash() {
    var h1 = HashUtils.hashOtp("123456", "user-uuid", "pepper");
    var h2 = HashUtils.hashOtp("123456", "user-uuid", "pepper");

    assertThat(h1).isEqualTo(h2);
  }

  @Test
  void hashOtp_differentUserProducesDifferentHash() {
    var h1 = HashUtils.hashOtp("123456", "user-uuid-1", "pepper");
    var h2 = HashUtils.hashOtp("123456", "user-uuid-2", "pepper");

    assertThat(h1).isNotEqualTo(h2);
  }

  @Test
  void hashOtp_differentPepperProducesDifferentHash() {
    var h1 = HashUtils.hashOtp("123456", "user-uuid", "pepper-a");
    var h2 = HashUtils.hashOtp("123456", "user-uuid", "pepper-b");

    assertThat(h1).isNotEqualTo(h2);
  }

  @Test
  void hashOtp_differentOtpProducesDifferentHash() {
    var h1 = HashUtils.hashOtp("111111", "user-uuid", "pepper");
    var h2 = HashUtils.hashOtp("999999", "user-uuid", "pepper");

    assertThat(h1).isNotEqualTo(h2);
  }

  // ----------------------------------------------------------------
  // hashRefreshToken
  // ----------------------------------------------------------------

  @Test
  void hashRefreshToken_producesDeterministicOutput() {
    var token = "550e8400-e29b-41d4-a716-446655440000";
    var h1 = HashUtils.hashRefreshToken(token);
    var h2 = HashUtils.hashRefreshToken(token);

    assertThat(h1).isEqualTo(h2);
  }

  @Test
  void hashRefreshToken_producesHexStringOf64Chars() {
    var hash = HashUtils.hashRefreshToken("550e8400-e29b-41d4-a716-446655440000");

    assertThat(hash).hasSize(64);
    assertThat(hash).matches("[0-9a-f]{64}");
  }

  // ----------------------------------------------------------------
  // constantTimeEquals
  // ----------------------------------------------------------------

  @Test
  void constantTimeEquals_returnsTrue_forEqualStrings() {
    assertThat(HashUtils.constantTimeEquals("abc", "abc")).isTrue();
  }

  @Test
  void constantTimeEquals_returnsFalse_forDifferentStrings() {
    assertThat(HashUtils.constantTimeEquals("abc", "xyz")).isFalse();
  }

  @Test
  void constantTimeEquals_returnsFalse_forDifferentLengths() {
    assertThat(HashUtils.constantTimeEquals("abc", "abcd")).isFalse();
  }

  @Test
  void constantTimeEquals_returnsTrue_forBothNull() {
    assertThat(HashUtils.constantTimeEquals(null, null)).isTrue();
  }

  @Test
  void constantTimeEquals_returnsFalse_forOneNull() {
    assertThat(HashUtils.constantTimeEquals(null, "abc")).isFalse();
    assertThat(HashUtils.constantTimeEquals("abc", null)).isFalse();
  }

  @Test
  void constantTimeEquals_worksWithHashedValues() {
    var input = "123456:user-uuid:pepper";
    var hashA = HashUtils.sha256(input);
    var hashB = HashUtils.sha256(input);
    var hashWrong = HashUtils.sha256("wrong:user-uuid:pepper");

    assertThat(HashUtils.constantTimeEquals(hashA, hashB)).isTrue();
    assertThat(HashUtils.constantTimeEquals(hashA, hashWrong)).isFalse();
  }
}
