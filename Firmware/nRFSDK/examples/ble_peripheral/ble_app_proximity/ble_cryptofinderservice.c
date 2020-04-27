/**
 * Copyright (c) 2013 - 2017, Nordic Semiconductor ASA
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form, except as embedded into a Nordic
 *    Semiconductor ASA integrated circuit in a product or a software update for
 *    such product, must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 * 
 * 3. Neither the name of Nordic Semiconductor ASA nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 * 
 * 4. This software, with or without modification, must only be used with a
 *    Nordic Semiconductor ASA integrated circuit.
 * 
 * 5. Any software provided in binary form under this license must not be reverse
 *    engineered, decompiled, modified and/or disassembled.
 * 
 * THIS SOFTWARE IS PROVIDED BY NORDIC SEMICONDUCTOR ASA "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, NONINFRINGEMENT, AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
#define NRF_LOG_LEVEL       4

#include "sdk_common.h"
#if NRF_MODULE_ENABLED(BLE_CRYPTOSERVICE)
#include "sha256.h"
#include "nrf_soc.h"
#include "ble_cryptofinderservice.h"
#include "ble_srv_common.h"
#include "sys_encryption.h"
#include "fds.h"

#define NRF_LOG_MODULE_NAME "Crypto"
#include "nrf_log.h"
#include "nrf_log_ctrl.h"

#include "fstorage.h"
#include "section_vars.h"


uint32_t ble_cryptoservice_send_iamlost(ble_cryptoservice_t * p_cfs, uint8_t *response, uint16_t response_length);
uint32_t ble_cryptoservice_send_setup_response(ble_cryptoservice_t * p_cfs, uint8_t *setup_response);



uint8_t g_mfg_key[16];
uint8_t g_e2e_key[16];
uint32_t g_geoloc_ctr;


#define FILE_ID_KEYS     0x1577
#define REC_MFGKEY     0x2222
#define REC_E2EKEY     0xE2EE
#define REC_TESTMSG     0x1234

static void store_key(uint16_t record_id, uint8_t* key, uint8_t length) {
    fds_record_t        record;
    fds_record_desc_t   record_desc;
    fds_record_chunk_t  record_chunk;
    // Set up data.
    record_chunk.p_data         = &key;
    record_chunk.length_words   = length/4;
    // Set up record.
    record.file_id                  = FILE_ID_KEYS;
    record.key               = record_id;
    record.data.p_chunks     = &record_chunk;
    record.data.num_chunks   = 1;
    
    ret_code_t ret = fds_record_write(&record_desc, &record);
    if (ret != FDS_SUCCESS)
    {
        NRF_LOG_INFO("store_key fds_record_write fail %d\n", ret);
    }
}
static int load_key(uint16_t record_id, uint8_t* key, uint8_t length) {
  fds_flash_record_t  flash_record;
  fds_record_desc_t   record_desc;
  fds_find_token_t    ftok;
  memset(&ftok, 0x00, sizeof(fds_find_token_t));
  // Loop until all records with the given key and file ID have been found.
  while (fds_record_find(FILE_ID_KEYS, record_id, &record_desc, &ftok) == FDS_SUCCESS)
  {
      if (fds_record_open(&record_desc, &flash_record) != FDS_SUCCESS)
      {
          NRF_LOG_INFO("fds_record_open failed\r\n");
          return 2;
      }
      // Access the record through the flash_record structure.
      memcpy(key, flash_record.p_data, length);
      // Close the record when done.
      if (fds_record_close(&record_desc) != FDS_SUCCESS)
      {
          NRF_LOG_INFO("fds_record_close failed\r\n");
          return 3;
      }
      NRF_LOG_INFO("load_key %04x success\r\n", record_id);
      NRF_LOG_HEXDUMP_INFO(key, length);
      
      return 0;
  }
  NRF_LOG_INFO("load_key %04x no match\r\n", record_id);
  return 1;
}

static void load_app_config() {
    load_key(REC_MFGKEY, g_mfg_key, 16);
    load_key(REC_E2EKEY, g_e2e_key, 16);
    
}


static void get_bluetooth_mac(uint8_t *mac) {
  NRF_LOG_HEXDUMP_INFO((uint8_t*)NRF_FICR->DEVICEADDR, 8);
  const uint8_t *macptr = (uint8_t*)NRF_FICR->DEVICEADDR;
  mac[0] = macptr[5];
  mac[1] = macptr[4];
  mac[2] = macptr[3];
  mac[3] = macptr[2];
  mac[4] = macptr[1];
  mac[5] = macptr[0];
  NRF_LOG_INFO("get_bluetooth_mac: %02x %02x %02x %02x %02x %02x\n", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
}


static void on_setup_challenge(ble_cryptoservice_t * p_cfs, const uint8_t* challenge) {
  uint32_t err_code;
  uint8_t response[32];
  uint8_t mac[6];
  get_bluetooth_mac(mac);
  
  NRF_LOG_INFO("on_setup_challenge\r\n");
  generate_setup_response(response,
                          g_mfg_key,
                          mac,
                          challenge);
  
  err_code = ble_cryptoservice_send_setup_response(p_cfs, response);
  APP_ERROR_CHECK(err_code);
}


static void on_geoloc(ble_cryptoservice_t * p_cfs, const uint8_t* geoloc) {
  sha256_context_t context;
  uint8_t response[4/*msg-ctr*/ + 6/*mac-addr*/ + 16/*IV*/ + 32/*e2e-msg*/ + 32/*hmac*/];
  *((uint32_t*)response) = g_geoloc_ctr ++;
  get_bluetooth_mac(response + 4);
  aes_ctr_crypt_message(g_e2e_key, geoloc, response + 4 + 6, 32);
  
  uint32_t err_code;
  uint8_t prefix[4] = {0x46,0x49,0x4e,0x00};

  // Initialize context, prividing the key and pointer to info structure.
  hmac_sha256_init(&context, g_mfg_key, 32);
  sha256_update(&context, prefix, sizeof(prefix));
  sha256_update(&context, response, sizeof(response) - 32);
  hmac_sha256_final(&context, g_mfg_key, 32, response + 4 + 6 + 16 + 32);
  APP_ERROR_CHECK(err_code);
  
  ble_cryptoservice_send_iamlost(p_cfs, response, sizeof(response));
}


/**@brief Function for handling the Connect event.
 *
 * @param[in] p_cfs      LED Button Service structure.
 * @param[in] p_ble_evt  Event received from the BLE stack.
 */
static void on_connect(ble_cryptoservice_t * p_cfs, ble_evt_t * p_ble_evt)
{
    p_cfs->conn_handle = p_ble_evt->evt.gap_evt.conn_handle;
}


/**@brief Function for handling the Disconnect event.
 *
 * @param[in] p_cfs      LED Button Service structure.
 * @param[in] p_ble_evt  Event received from the BLE stack.
 */
static void on_disconnect(ble_cryptoservice_t * p_cfs, ble_evt_t * p_ble_evt)
{
    UNUSED_PARAMETER(p_ble_evt);
    p_cfs->conn_handle = BLE_CONN_HANDLE_INVALID;
}


/**@brief Function for handling the Write event.
 *
 * @param[in] p_cfs      LED Button Service structure.
 * @param[in] p_ble_evt  Event received from the BLE stack.
 */
static void on_write(ble_cryptoservice_t * p_cfs, ble_evt_t * p_ble_evt)
{
    ble_gatts_evt_write_t * p_evt_write = &p_ble_evt->evt.gatts_evt.params.write;

    char data[33];
    NRF_LOG_INFO("on_write h=%X dl=%d\n", p_evt_write->handle, p_evt_write->len);
    
    if (p_evt_write->len == 16) {
        memcpy(&data, p_evt_write->data, p_evt_write->len);
        data[p_evt_write->len] = 0;
        
        if (p_evt_write->handle == p_cfs->e2ekey_char_handles.value_handle) {
            NRF_LOG_INFO("e2ekey_WR: %d\n", p_evt_write->data[0]);
            memcpy(g_e2e_key, p_evt_write->data, 16);
            store_key(REC_E2EKEY, g_e2e_key, 16);
            NRF_LOG_INFO("ok\n");
            
        }
        if (p_evt_write->handle == p_cfs->setup_char_handles.value_handle) {
            NRF_LOG_INFO("setup_WR: %d\n", p_evt_write->data[0]);
            on_setup_challenge(p_cfs, p_evt_write->data);
            NRF_LOG_INFO("OK\n");
        }
        if (p_evt_write->handle == p_cfs->geoloc_char_handles.value_handle) {
            NRF_LOG_INFO("geoloc_WR: %d\n", p_evt_write->data[0]);
            on_geoloc(p_cfs, p_evt_write->data);
        }
    }
    if (p_evt_write->len == 32) {
        memcpy(&data, p_evt_write->data, p_evt_write->len);
        data[p_evt_write->len] = 0;
        
        if (p_evt_write->handle == p_cfs->message_char_handles.value_handle) {
            NRF_LOG_INFO("MSG_WR %d >>%s<<\n", p_evt_write->len, (uint32_t)data);
            store_key(REC_TESTMSG, p_evt_write->data, 32);
            NRF_LOG_INFO("Ok\n");
        }
    }
}


void ble_cryptoservice_on_ble_evt(ble_cryptoservice_t * p_cfs, ble_evt_t * p_ble_evt)
{
    switch (p_ble_evt->header.evt_id)
    {
        case BLE_GAP_EVT_CONNECTED:
            on_connect(p_cfs, p_ble_evt);
            break;

        case BLE_GAP_EVT_DISCONNECTED:
            on_disconnect(p_cfs, p_ble_evt);
            break;

        case BLE_GATTS_EVT_WRITE:
            on_write(p_cfs, p_ble_evt);
            break;

        default:
            // No implementation needed.
            break;
    }
}



#define RCP_READ      1  /**< Reading the value permitted. */
#define RCP_WRITE_WR  2  /**< Writing the value with Write Command permitted. */
#define RCP_WRITE     4  /**< Writing the value with Write Request permitted. */
#define RCP_NOTIFY    8  /**< Notications of the value permitted. */
#define RCP_PUB_WRITE 16
#define RCP_PUB_READ  32

/**@brief Function for adding the LED Characteristic.
 *
 * @param[in] p_cfs      LED Button Service structure.
 *
 * @retval NRF_SUCCESS on success, else an error value from the SoftDevice
 */
static uint32_t register_characteristic(uint16_t service_handle, uint8_t uuidType, uint16_t uuid,
                                        ble_gatts_char_handles_t * char_handle,
                                        uint8_t flags, uint8_t value_len)
{
    ble_gatts_char_md_t char_md;
    ble_gatts_attr_t    attr_char_value;
    ble_uuid_t          ble_uuid;
    ble_gatts_attr_md_t attr_md;

    memset(&char_md, 0, sizeof(char_md));

    char_md.char_props.read     = !!(flags & RCP_READ);
    char_md.char_props.write_wo_resp = !!(flags & RCP_WRITE_WR);
    char_md.char_props.write    = !!(flags & RCP_WRITE);
    char_md.char_props.notify   = !!(flags & RCP_NOTIFY);
    char_md.p_char_user_desc  = NULL;
    char_md.p_char_pf         = NULL;
    char_md.p_user_desc_md    = NULL;
    char_md.p_cccd_md         = NULL;
    char_md.p_sccd_md         = NULL;

    ble_uuid.type = uuidType;
    ble_uuid.uuid = uuid;

    memset(&attr_md, 0, sizeof(attr_md));

    if (flags & RCP_PUB_READ)
      BLE_GAP_CONN_SEC_MODE_SET_OPEN(&attr_md.read_perm);
    else
      BLE_GAP_CONN_SEC_MODE_SET_ENC_NO_MITM(&attr_md.read_perm);
    if (flags & RCP_PUB_WRITE)
      BLE_GAP_CONN_SEC_MODE_SET_OPEN(&attr_md.write_perm);
    else
      BLE_GAP_CONN_SEC_MODE_SET_ENC_NO_MITM(&attr_md.write_perm);
    attr_md.vloc       = BLE_GATTS_VLOC_STACK;
    attr_md.rd_auth    = 0;
    attr_md.wr_auth    = 0;
    attr_md.vlen       = 0;

    memset(&attr_char_value, 0, sizeof(attr_char_value));

    attr_char_value.p_uuid       = &ble_uuid;
    attr_char_value.p_attr_md    = &attr_md;
    attr_char_value.init_len     = sizeof(uint8_t);
    attr_char_value.init_offs    = 0;
    attr_char_value.max_len      = value_len;
    attr_char_value.p_value      = NULL;

    return sd_ble_gatts_characteristic_add(service_handle, &char_md,
                                           &attr_char_value, char_handle);
}



uint32_t ble_cryptoservice_init(ble_cryptoservice_t * p_cfs)
{
    uint32_t   err_code;
    ble_uuid_t ble_uuid;

    
    g_geoloc_ctr = 0;
    memset(g_mfg_key, 0x42, 16);
    memset(g_e2e_key, 0x00, 16);
    
    load_app_config();
    
    
    
    // Initialize service structure.
    p_cfs->conn_handle       = BLE_CONN_HANDLE_INVALID;
    //p_cfs->led_write_handler = p_cfs_init->led_write_handler;

    // Define a custom / vendor-specific UUID
    // we get a special uuid_type which we will later use for adding the service
    ble_uuid128_t base_uuid = {CRYPTOSERVICE_UUID_BASE};
    err_code = sd_ble_uuid_vs_add(&base_uuid, &p_cfs->uuid_type);
    VERIFY_SUCCESS(err_code);

    // for a BLE-SIG-UUID (16bit, defined in standard) we would use BLE_UUID_BLE_ASSIGN
    // which would set type = BLE_UUID_TYPE_BLE
    // here we use our custom uuid_type we registered earlier
    ble_uuid.type = p_cfs->uuid_type;
    ble_uuid.uuid = CRYPTOSERVICE_UUID_SERVICE;

    // Add service.
    err_code = sd_ble_gatts_service_add(BLE_GATTS_SRVC_TYPE_PRIMARY, &ble_uuid, &p_cfs->service_handle);
    VERIFY_SUCCESS(err_code);

    // Add characteristics.
    err_code = register_characteristic(p_cfs->service_handle, p_cfs->uuid_type, 
                CFS_UUID_E2EKEY_CHAR, &p_cfs->e2ekey_char_handles, RCP_WRITE, 16);
    VERIFY_SUCCESS(err_code);
    err_code = register_characteristic(p_cfs->service_handle, p_cfs->uuid_type, 
                CFS_UUID_SETUP_CHAR, &p_cfs->setup_char_handles, RCP_WRITE | RCP_NOTIFY, 16);
    VERIFY_SUCCESS(err_code);
    err_code = register_characteristic(p_cfs->service_handle, p_cfs->uuid_type, 
                CFS_UUID_GEOLOC_CHAR, &p_cfs->geoloc_char_handles, RCP_WRITE | RCP_PUB_WRITE, 8);
    VERIFY_SUCCESS(err_code);
    err_code = register_characteristic(p_cfs->service_handle, p_cfs->uuid_type, 
                CFS_UUID_IAMLOST_CHAR, &p_cfs->iamlost_char_handles, RCP_NOTIFY | RCP_PUB_READ, 32);
    VERIFY_SUCCESS(err_code);
    err_code = register_characteristic(p_cfs->service_handle, p_cfs->uuid_type, 
                CFS_UUID_MESSAGE_CHAR, &p_cfs->message_char_handles, RCP_READ | RCP_WRITE | RCP_PUB_READ, 32);
    VERIFY_SUCCESS(err_code);

    // Load value of message from flash    
    ble_gatts_value_t gatts_value;

    memset(&gatts_value, 0, sizeof(gatts_value));

    uint8_t buf[32];
    load_key(REC_TESTMSG, buf, 32);
    
    gatts_value.len     = sizeof(buf);
    gatts_value.offset  = 0;
    gatts_value.p_value = buf;

    err_code = sd_ble_gatts_value_set(BLE_CONN_HANDLE_INVALID,
                                      p_cfs->message_char_handles.value_handle,
                                      &gatts_value);
    VERIFY_SUCCESS(err_code);

    return NRF_SUCCESS;
}

uint32_t ble_cryptoservice_send_setup_response(ble_cryptoservice_t * p_cfs, uint8_t *setup_response)
{
    ble_gatts_hvx_params_t params;
    uint16_t len = 16; // sha256 = 32 byte ...is too long for notification, sending only first 16 byte

    memset(&params, 0, sizeof(params));
    params.type = BLE_GATT_HVX_NOTIFICATION;
    params.handle = p_cfs->setup_char_handles.value_handle;
    params.p_data = setup_response;
    params.p_len = &len;

    return sd_ble_gatts_hvx(p_cfs->conn_handle, &params);
}

uint32_t ble_cryptoservice_send_iamlost(ble_cryptoservice_t * p_cfs, uint8_t *response, uint16_t response_length)
{
    ble_gatts_hvx_params_t params;

    memset(&params, 0, sizeof(params));
    params.type = BLE_GATT_HVX_NOTIFICATION;
    params.handle = p_cfs->iamlost_char_handles.value_handle;
    params.p_data = response;
    params.p_len = &response_length;

    return sd_ble_gatts_hvx(p_cfs->conn_handle, &params);
}
#endif // NRF_MODULE_ENABLED(ble_cryptoservice)
