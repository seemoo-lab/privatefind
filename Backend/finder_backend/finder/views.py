from django.shortcuts import render
import hmac, binascii, struct, os
from django.http import JsonResponse
from .models import Tracker, Finding
from django.views import View
from django.views.decorators.csrf import csrf_exempt
from django.utils.decorators import method_decorator
from django.db.utils import IntegrityError
from django.forms.models import model_to_dict
import base64
from binascii import hexlify
from django.conf import settings

def generate_key():
    return base64.b64encode(os.urandom(16)).decode()

@method_decorator(csrf_exempt, name='dispatch')
class RegisterView(View):
    def get(self, request):
        if "register_challenge" not in request.session:
            request.session["register_challenge"] = generate_key()
        return JsonResponse({'setup-challenge': request.session["register_challenge"]})

    def post(self, request):
        mac_address = request.POST["mac-address"]
        print(mac_address)
        device = Tracker.objects.get(mac_address=mac_address)
        challenge = base64.b64decode(request.session["register_challenge"])
        expected_response = hmac.new(device.key_bin(), b"REG\0" + device.mac_bin() + challenge, "sha256").digest()[0:16]
        received_response = base64.b64decode(request.POST["setup-response"])
        print("mac key:",hexlify(device.key_bin()), "  data:",hexlify(b"REG\0" + device.mac_bin() + challenge))
        print ("expected: ",expected_response)
        print ("received: ",received_response)
        if received_response != expected_response:
            raise Exception("invalid setup-response")
        del request.session["register_challenge"]
        device.access_token = generate_key()
        device.save()
        return JsonResponse({'access-token': device.access_token})

@method_decorator(csrf_exempt, name='dispatch')
class FoundView(View):
    def post(self, request):
        mac_address = request.POST["mac-address"]
        whole_message = base64.b64decode(request.POST["e2e-message"])
        if len(whole_message) == 96:
            b_ctr, b_e2e, b_server_hmac = whole_message[0:4], whole_message[4:84], whole_message[84:96]

            ctr = struct.unpack("!I", b_ctr)
            print("MAC",mac_address,"Ctr",ctr)
            print("Received e2e", hexlify(b_e2e))
            print("Received hmac", hexlify(b_server_hmac))

            device = Tracker.objects.get(mac_address=mac_address)
            server_hmac_msg = b"FIN\0" + device.mac_bin() + struct.pack("!I", ctr) + b_e2e
            expected_response = hmac.new(device.key_bin(), server_hmac_msg, "sha256").digest()
            
            print("Expected hmac", hexlify(expected_response))
            print("server_hmac_msg", hexlify(server_hmac_msg))
            print("server_hmac_key", hexlify(device.key_bin()))

            print("Found device ",device)
            if b_server_hmac != expected_response:
                print("Invalid signature",request.POST["signature"],expected_response)
                return JsonResponse({'success':False, 'msg': 'invalid signature'})
            try:
                Finding.objects.create(tracker=device, counter=ctr, e2e_message = base64.b64encode(server_hmac_msg))
            except IntegrityError:
                return JsonResponse({'success':False, 'msg': 'duplicate counter'})
            device.found_counter = ctr
            device.save()

            send_push_message(device, base64.b64encode(server_hmac_msg))

            return JsonResponse({'success':True})
        else:
            return JsonResponse({'success':False})


@method_decorator(csrf_exempt, name='dispatch')
class FindingsView(View):
    def get(self, request):
        device = Tracker.objects.get(access_token=request.GET["access-token"])
        return JsonResponse({"findings": [{'mac-address': f.tracker.mac_address, 'counter': f.counter, 'e2e-message': f.e2e_message} for f in device.finding_set.all()]})
    def delete(self, request):
        device = Tracker.objects.get(access_token=request.GET["access-token"])
        device.finding_set.delete()
        return HttpResponse(204)


@method_decorator(csrf_exempt, name='dispatch')
class PushTokenView(View):
    def post(self, request):
        device = Tracker.objects.get(access_token=request.POST["access-token"])
        device.push_service_type = request.POST["push-type"]
        device.push_service_token = request.POST["push-token"]
        device.save()
        return JsonResponse({'success':True})

def send_push_message(device, e2e_message):
    if device.push_service_type == "firebase":
        send_firebase_push_message(device.push_service_token)

def send_firebase_push_message(registration_id, e2e_message):
    from pyfcm import FCMNotification
    
    push_service = FCMNotification(api_key=settings.FIREBASE_FCM_API_KEY)
    
    message_title = "privatefind_push"
    message_body = e2e_message
    result = push_service.notify_single_device(registration_id=registration_id, message_title=message_title, message_body=message_body)
    
    print (result)
