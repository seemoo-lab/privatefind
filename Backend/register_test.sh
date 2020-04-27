#!/bin/bash
mac="$1"
#"777777777777"
mfg_key="$2"
#"1234567890ab1234567890ab1234567890ab1234567890ab1234567890ab1234"


challenge="$(curl -s -b cookiejar.txt -c cookiejar.txt http://127.0.0.1:8000/register | jq -r '.["setup-challenge"]')"
echo $challenge
response="$(python3 calc_hmac.py "52454700$mac$challenge" "$mfg_key")"


curl -s -b cookiejar.txt -c cookiejar.txt http://127.0.0.1:8000/register \
    --data-urlencode "mac-address=$mac" \
    --data-urlencode "setup-response=$response" | jq .
