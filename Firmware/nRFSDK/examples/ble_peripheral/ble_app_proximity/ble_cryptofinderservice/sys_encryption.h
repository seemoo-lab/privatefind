#ifndef _SYS_ENCRYPTION
#define _SYS_ENCRYPTION
#include "sha256.h"
void generate_setup_response(uint8_t *response, uint8_t const * p_key, uint8_t const * mac_addr, uint8_t const * challenge);
void aes_ctr_crypt_message(const uint8_t* key, const uint8_t* in, uint8_t *out, uint16_t length);


//key length must be <= 64 byte
void hmac_sha256_init(sha256_context_t *context, const uint8_t* key, uint8_t keylen);
void hmac_sha256_final(sha256_context_t *context, const uint8_t* key, uint8_t keylen, uint8_t* digest) ;


#endif
