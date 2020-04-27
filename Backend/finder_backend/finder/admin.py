from django.contrib import admin

# Register your models here.
from .models import *

class TrackerAdmin(admin.ModelAdmin):
    pass
admin.site.register(Tracker, TrackerAdmin)


class FindingAdmin(admin.ModelAdmin):
    pass
admin.site.register(Finding, FindingAdmin)

