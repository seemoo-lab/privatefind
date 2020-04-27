from django.db import models
import binascii
from django.core.validators import RegexValidator
# Create your models here.

validate_mac_addr = RegexValidator(regex="[a-f0-9]{12}")

class Tracker(models.Model):
    mac_address = models.CharField(unique=True, max_length=12, validators=[validate_mac_addr])
    mfg_key = models.CharField(max_length=64)
    is_lost = models.BooleanField()
    contact = models.CharField(max_length=64, blank=True, null=True)
    access_token = models.CharField(max_length=64, blank=True, null=True)
    push_service_type = models.CharField(max_length=32, blank=True, null=True)
    push_service_token = models.CharField(max_length=255, blank=True, null=True)
    found_counter = models.IntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    def mac_bin(self):
        return binascii.unhexlify(self.mac_address)
    def key_bin(self):
        return binascii.unhexlify(self.mfg_key)
    def __str__(self):
        return "-".join("%02X"%x for x in self.mac_bin())

class Finding(models.Model):
    class Meta:
        unique_together = (('tracker', 'counter'),)
    tracker = models.ForeignKey("Tracker", on_delete="CASCADE")
    counter = models.IntegerField()
    e2e_message = models.TextField()
    created_at = models.DateTimeField(auto_now_add=True)

