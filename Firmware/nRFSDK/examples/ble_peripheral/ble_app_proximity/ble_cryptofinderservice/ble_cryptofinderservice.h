/**
 * Copyright (c) 2015 - 2017, Nordic Semiconductor ASA
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

/** @file
 *
 * @defgroup ble_cryptoservice LED Button Service Server
 * @{
 * @ingroup ble_sdk_srv
 *
 * @brief LED Button Service Server module.
 *
 * @details This module implements a custom LED Button Service with an LED and Button Characteristics.
 *          During initialization, the module adds the LED Button Service and Characteristics
 *          to the BLE stack database.
 *
 *          The application must supply an event handler for receiving LED Button Service
 *          events. Using this handler, the service notifies the application when the
 *          LED value changes.
 *
 *          The service also provides a function for letting the application notify
 *          the state of the Button Characteristic to connected peers.
 *
 * @note The application must propagate BLE stack events to the LED Button Service
 *       module by calling ble_cryptoservice_on_ble_evt() from the @ref softdevice_handler callback.
*/

#ifndef ble_cryptoservice_H__
#define ble_cryptoservice_H__

#include <stdint.h>
#include <stdbool.h>
#include "ble.h"
#include "ble_srv_common.h"

#ifdef __cplusplus
extern "C" {
#endif

#define CRYPTOSERVICE_UUID_BASE        {0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0x34, 0x12, \
                              0x34, 0x12, 0x00, 0x00, 0x00, 0x00, 0xc0, 0xab}
#define CRYPTOSERVICE_UUID_SERVICE     0x1577
#define CFS_UUID_E2EKEY_CHAR   0x1580
#define CFS_UUID_SETUP_CHAR    0x1581
#define CFS_UUID_GEOLOC_CHAR   0x1582
#define CFS_UUID_IAMLOST_CHAR  0x1583
#define CFS_UUID_MESSAGE_CHAR  0x1584


struct my_app_config {
  uint8_t e2e_key[16];
  char finder_message[32];
  
};


// Forward declaration of the ble_cryptoservice_t type.
typedef struct ble_cryptoservice_s ble_cryptoservice_t;

typedef void (*ble_cryptoservice_led_write_handler_t) (ble_cryptoservice_t * p_cryptoservice, uint8_t new_state);

/** @brief LED Button Service init structure. This structure contains all options and data needed for
 *        initialization of the service.*/
typedef struct
{
    ble_cryptoservice_led_write_handler_t led_write_handler; /**< Event handler to be called when the LED Characteristic is written. */
} ble_cryptoservice_init_t;

/**@brief LED Button Service structure. This structure contains various status information for the service. */
struct ble_cryptoservice_s
{
    uint16_t                    service_handle;      /**< Handle of LED Button Service (as provided by the BLE stack). */
    
    ble_gatts_char_handles_t    e2ekey_char_handles;
    ble_gatts_char_handles_t    setup_char_handles;
    ble_gatts_char_handles_t    geoloc_char_handles;
    ble_gatts_char_handles_t    iamlost_char_handles;
    ble_gatts_char_handles_t    message_char_handles;

    uint8_t                     uuid_type;           /**< UUID type for the LED Button Service. */
    uint16_t                    conn_handle;         /**< Handle of the current connection (as provided by the BLE stack). BLE_CONN_HANDLE_INVALID if not in a connection. */
    //ble_cryptoservice_led_write_handler_t led_write_handler;   /**< Event handler to be called when the LED Characteristic is written. */
};

/**@brief Function for initializing the LED Button Service.
 *
 * @param[out] p_cryptoservice      LED Button Service structure. This structure must be supplied by
 *                        the application. It is initialized by this function and will later
 *                        be used to identify this particular service instance.
 * @param[in] p_cryptoservice_init  Information needed to initialize the service.
 *
 * @retval NRF_SUCCESS If the service was initialized successfully. Otherwise, an error code is returned.
 */
uint32_t ble_cryptoservice_init(ble_cryptoservice_t * p_cryptoservice);

/**@brief Function for handling the application's BLE stack events.
 *
 * @details This function handles all events from the BLE stack that are of interest to the LED Button Service.
 *
 * @param[in] p_cryptoservice      LED Button Service structure.
 * @param[in] p_ble_evt  Event received from the BLE stack.
 */
void ble_cryptoservice_on_ble_evt(ble_cryptoservice_t * p_cryptoservice, ble_evt_t * p_ble_evt);

/**@brief Function for sending a button state notification.
 *
 * @param[in] p_cryptoservice      LED Button Service structure.
 * @param[in] button_state  New button state.
 *
 * @retval NRF_SUCCESS If the notification was sent successfully. Otherwise, an error code is returned.
 */
uint32_t ble_cryptoservice_on_button_change(ble_cryptoservice_t * p_cryptoservice, uint8_t button_state);


#ifdef __cplusplus
}
#endif

#endif // ble_cryptoservice_H__

/** @} */
