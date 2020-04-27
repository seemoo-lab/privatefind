#!/bin/bash
mac="$1"
#"777777777777"
mfg_key="$2"
#"1234567890ab1234567890ab1234567890ab1234567890ab1234567890ab1234"

ctr="$3"
hexctr="$(printf '%08x' "$ctr")"
echo $hexctr
e2emessage="$4"

response="$(python3 calc_hmac.py "46494e00$mac$hexctr" "$mfg_key")"


curl -s -b cookiejar.txt -c cookiejar.txt http://127.0.0.1:8000/found \
    --data-urlencode "mac-address=$mac" \
    --data-urlencode "counter=$ctr" \
    --data-urlencode "signature=$response" \
    --data-urlencode "e2e-message=$e2emessage" | jq .

