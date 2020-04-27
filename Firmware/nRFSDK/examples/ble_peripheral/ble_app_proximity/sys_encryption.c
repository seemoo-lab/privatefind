
#include "sdk_common.h"
#include "sys_encryption.h"
#include "sha256.h"
#include "nrf_soc.h"

#include "app_error.h"

#define NRF_LOG_LEVEL       4
#define NRF_LOG_MODULE_NAME "SEC"
#include "nrf_log.h"
#include "nrf_log_ctrl.h"

struct iv_ctr {
  char iv[12];
  uint32_t ctr;
};


void aes_ctr_crypt_block(const uint8_t* key, const struct iv_ctr *ctr, const uint8_t* in, uint8_t *out, uint16_t length) {
  nrf_ecb_hal_data_t m_ecb_data;
  uint32_t err_code;
  uint16_t i;
  memset(&m_ecb_data, 0, sizeof(m_ecb_data));
  
  memcpy(m_ecb_data.key,       key,   SOC_ECB_KEY_LENGTH);
  memcpy(m_ecb_data.cleartext, ctr,   SOC_ECB_CLEARTEXT_LENGTH);

  err_code = sd_ecb_block_encrypt(&m_ecb_data);
  APP_ERROR_CHECK(err_code);
  
  for(i = 0; i<length; i++) {
    out[i] = in[i] ^ m_ecb_data.ciphertext[i];
  }
}


void aes_ctr_crypt_message(const uint8_t* key, const uint8_t* in, uint8_t *out, uint16_t length) {
  struct iv_ctr d;
  uint16_t i;
  sd_rand_application_vector_get((uint8_t*)&d, sizeof(d.iv));
  memcpy(out, &d, sizeof(d));
  d.ctr = 0;
  for(i=0; i<length; i+=16) {
    aes_ctr_crypt_block(key, &d, in + i, out + sizeof(d) + i, (length-i) > 16 ? 16 : (length-i));
    d.ctr ++;
  }
}



#define HMAC_IPAD_VALUE 0x36
#define HMAC_OPAD_VALUE 0x5c

//key length must be <= 64 byte
void hmac_sha256_init(sha256_context_t *context, const uint8_t* key, uint8_t keylen) {
  uint8_t ipad[64];
  uint8_t i;
  memset(ipad, 0, 64);
  memcpy(ipad, key, keylen);
  for(i=0;i<64;i++) ipad[i] ^= HMAC_IPAD_VALUE;
  sha256_init(context);
  sha256_update(context, ipad, 64);
}
void hmac_sha256_final(sha256_context_t *context, const uint8_t* key, uint8_t keylen, uint8_t* digest) {
  uint8_t opad[64 + 32];
  uint8_t i;
  memset(opad, 0, 64);
  memcpy(opad, key, keylen);
  for(i=0;i<64;i++) opad[i] ^= HMAC_OPAD_VALUE;
  sha256_final(context, opad + 64, 0/*big endian*/);
  
  // outer hash
  sha256_init(context);
  sha256_update(context, opad, 96);
  sha256_final(context, digest, 0/*big endian*/);
}



//NRF_CRYPTO_HASH_SIZE_SHA256 = 32
void generate_setup_response(uint8_t *response, uint8_t const * p_key, uint8_t const * mac_addr, uint8_t const * challenge) {
  sha256_context_t context;
  uint8_t prefix[4] = {0x52,0x45,0x47,0x00};

  // Initialize context, prividing the key and pointer to info structure.
  hmac_sha256_init(&context, p_key, 16);
  NRF_LOG_HEXDUMP_INFO(prefix, sizeof(prefix));
  NRF_LOG_HEXDUMP_INFO(mac_addr, 6);
  NRF_LOG_HEXDUMP_INFO(challenge, 16);
  NRF_LOG_HEXDUMP_INFO(p_key, 16);
  
  
  sha256_update(&context, prefix, sizeof(prefix));
  sha256_update(&context, mac_addr, 6);
  sha256_update(&context, challenge, 16);
  hmac_sha256_final(&context, p_key, 16, response);
}

