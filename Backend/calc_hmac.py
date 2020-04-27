

from binascii import unhexlify
import hmac, sys
challenge=sys.argv[1]
key=sys.argv[2]
print(hmac.new(unhexlify(key), unhexlify(challenge), "sha256").hexdigest())

